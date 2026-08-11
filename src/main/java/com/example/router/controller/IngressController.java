package com.example.router.controller;

import com.example.router.config.RoutesProperties;
import com.example.router.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLOutput;

/**
 * Принимает SOAP-конверт вида:
 *
 * <SOAP-ENV:Envelope>...<request><requestData><data>
 *   <![CDATA[ <cvRecruit><cv><iin>...</iin></cv></cvRecruit> ]]>
 * </data>...
 *
 * Извлекает БИН из вложенного XML внутри <data> и по нему определяет,
 * в какой downstream-сервис отправить итоговый sbapi-конверт.
 */
@RestController
public class IngressController {

    private final NestedPayloadExtractor nestedPayloadExtractor;
    private final RoutingService routingService;
    private final SbApiEnvelopeBuilder envelopeBuilder;
    private final OutboundClient outboundClient;
    private final IncomingAuthService incomingAuthService;

    private static final int INTERFACE_VERSION = 8;
    private static final String MSG_TYPE = "5000";

    public IngressController(NestedPayloadExtractor nestedPayloadExtractor,
                             RoutingService routingService,
                             SbApiEnvelopeBuilder envelopeBuilder,
                             OutboundClient outboundClient,
                             IncomingAuthService incomingAuthService) {
        this.nestedPayloadExtractor = nestedPayloadExtractor;
        this.routingService = routingService;
        this.envelopeBuilder = envelopeBuilder;
        this.outboundClient = outboundClient;
        this.incomingAuthService = incomingAuthService;
    }

    @PostMapping(value = "/route", consumes = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> route(@RequestBody byte[] incomingBytes, HttpServletRequest request) {

        String incomingXml = new String(incomingBytes, StandardCharsets.UTF_8);
        Document outerDoc = parseXml(incomingXml);
        String senderId = nestedPayloadExtractor.extractLogin(outerDoc);
        String password = nestedPayloadExtractor.extractPassword(outerDoc);
        String serviceId = nestedPayloadExtractor.extractServiceId(outerDoc);

        incomingAuthService.isAuthorized(senderId, password);
        incomingAuthService.isServiceIdCorrect(serviceId);

        String innerXml = nestedPayloadExtractor.extractDataElementText(outerDoc);
        String systemCode = nestedPayloadExtractor.extractSystemCode(innerXml);
        RoutingService.RouteMatch match = routingService.resolveRouteBySystemCode(systemCode);

        RoutesProperties.RouteConfig cfg = match.config();

        String dataIntoJson = nestedPayloadExtractor.extractCandidateDataAndParseJson(innerXml);
        SbApiEnvelopeBuilder.BuildRequest buildRequest = new SbApiEnvelopeBuilder.BuildRequest(
                Integer.parseInt(cfg.getInterfaceId(), 16),
                INTERFACE_VERSION,
                envelopeBuilder.nextMsgId(),
                MSG_TYPE,
                cfg.getLogin(),
                cfg.getPassword(),
                normalizeIp(request.getRemoteAddr()),
                dataIntoJson
        );

        String outgoingXml = envelopeBuilder.build(buildRequest);

        // === было раньше ===
        // ResponseEntity<String> downstreamResponse = outboundClient.send(cfg.getUrl(), outgoingXml);
        // return ResponseEntity.status(HttpStatus.OK)
        //         .contentType(MediaType.APPLICATION_XML)
        //         .body(downstreamResponse.getBody());

        // === новый блок вместо него ===
        ResponseEntity<String> downstreamResponse = outboundClient.send(cfg.getUrl(), outgoingXml);

        String errorId = nestedPayloadExtractor.extractErrorId(downstreamResponse.getBody());

        String responseXml;
        if ("0".equals(errorId)) {
            responseXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <response>
                    <status>OK</status>
                    <message>Сообщение успешно принято</message>
                </response>
                """;
        } else {
            responseXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <response>
                    <status>ERROR</status>
                    <message>Downstream-система вернула ошибку, код: %s</message>
                </response>
                """.formatted(escapeForXmlText(errorId));
        }

        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_XML)
                .body(responseXml);
    }

    private Document parseXml(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            // защита от XXE
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new InputSource(new StringReader(xml)));
        } catch (Exception e) {
            throw new IllegalArgumentException("Некорректный входящий XML", e);
        }
    }

    private String escapeForXmlText(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    /**
     * При локальных тестах через curl/localhost getRemoteAddr() может вернуть
     * IPv6-loopback ("0:0:0:0:0:0:0:1" или "::1"), что может не устроить
     * валидатор на стороне получателя, ожидающий формат IPv4.
     * В таком случае подставляем 127.0.0.1.
     */
    private String normalizeIp(String ip) {
        if (ip == null) {
            return "127.0.0.1";
        }
        if (ip.equals("0:0:0:0:0:0:0:1") || ip.equals("::1")) {
            return "127.0.0.1";
        }
        return ip;
    }
}
