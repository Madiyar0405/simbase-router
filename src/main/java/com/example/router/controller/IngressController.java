package com.example.router.controller;

import com.example.router.config.RoutesProperties;
import com.example.router.service.IncomingAuthService;
import com.example.router.service.NestedPayloadExtractor;
import com.example.router.service.OutboundClient;
import com.example.router.service.RoutingService;
import com.example.router.service.SbApiEnvelopeBuilder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

@RestController
public class IngressController {

    private final NestedPayloadExtractor nestedPayloadExtractor;
    private final RoutingService routingService;
    private final SbApiEnvelopeBuilder envelopeBuilder;
    private final OutboundClient outboundClient;
    private final IncomingAuthService incomingAuthService;

    private static final int INTERFACE_VERSION = 8;
    private static final String MSG_TYPE = "5000";

    public IngressController(
            NestedPayloadExtractor nestedPayloadExtractor,
            RoutingService routingService,
            SbApiEnvelopeBuilder envelopeBuilder,
            OutboundClient outboundClient,
            IncomingAuthService incomingAuthService
    ) {
        this.nestedPayloadExtractor = nestedPayloadExtractor;
        this.routingService = routingService;
        this.envelopeBuilder = envelopeBuilder;
        this.outboundClient = outboundClient;
        this.incomingAuthService = incomingAuthService;
    }

    @PostMapping(
            value = "/route",
            consumes = MediaType.APPLICATION_XML_VALUE
    )
    public ResponseEntity<String> route(
            @RequestBody byte[] incomingBytes,
            HttpServletRequest request
    ) {

        /*
         * ==========================================
         * 1. Получаем входящий SOAP
         * ==========================================
         */

        String incomingXml =
                new String(
                        incomingBytes,
                        StandardCharsets.UTF_8
                );

        Document outerDoc =
                parseXml(incomingXml);

        /*
         * ==========================================
         * 2. Получаем senderId/password/serviceId
         * ==========================================
         */

        String senderId =
                nestedPayloadExtractor.extractLogin(
                        outerDoc
                );

        String password =
                nestedPayloadExtractor.extractPassword(
                        outerDoc
                );

        String serviceId =
                nestedPayloadExtractor.extractServiceId(
                        outerDoc
                );

        /*
         * ==========================================
         * 3. Проверяем авторизацию
         * ==========================================
         */

        incomingAuthService.isAuthorized(
                senderId,
                password
        );

        incomingAuthService.isServiceIdCorrect(
                serviceId
        );

        /*
         * ==========================================
         * 4. Получаем <data>
         *
         * Метод сам определит:
         *
         * - обычный XML
         * - CDATA
         * - экранированный XML
         * ==========================================
         */

        org.w3c.dom.Node dataNode =
                nestedPayloadExtractor.extractDataNode(
                        outerDoc
                );

        /*
         * ==========================================
         * 5. Определяем systemCode
         * ==========================================
         */

        String systemCode =
                nestedPayloadExtractor.extractSystemCode(
                        dataNode
                );

        System.out.println(
                "SYSTEM CODE: " + systemCode
        );

        /*
         * ==========================================
         * 6. Ищем downstream route
         * ==========================================
         */

        RoutingService.RouteMatch match =
                routingService.resolveRouteBySystemCode(
                        systemCode
                );

        RoutesProperties.RouteConfig cfg =
                match.config();

        /*
         * ==========================================
         * 7. Получаем данные кандидата и JSON
         * ==========================================
         */

        String dataIntoJson =
                nestedPayloadExtractor
                        .extractCandidateDataAndParseJson(
                                dataNode
                        );

        System.out.println(
                "CANDIDATE JSON: "
                        + dataIntoJson
        );

        /*
         * ==========================================
         * 8. Создаём SB API envelope
         * ==========================================
         */

        SbApiEnvelopeBuilder.BuildRequest buildRequest =
                new SbApiEnvelopeBuilder.BuildRequest(
                        Integer.parseInt(
                                cfg.getInterfaceId(),
                                16
                        ),
                        INTERFACE_VERSION,
                        envelopeBuilder.nextMsgId(),
                        MSG_TYPE,
                        cfg.getLogin(),
                        cfg.getPassword(),
                        normalizeIp(
                                request.getRemoteAddr()
                        ),
                        dataIntoJson
                );

        String outgoingXml =
                envelopeBuilder.build(
                        buildRequest
                );

        /*
         * ==========================================
         * 9. Отправляем downstream
         * ==========================================
         */

        ResponseEntity<String> downstreamResponse =
                outboundClient.send(
                        cfg.getUrl(),
                        outgoingXml
                );

        System.out.println(
                "DOWNSTREAM BODY: "
                        + downstreamResponse.getBody()
        );

        /*
         * ==========================================
         * 10. Проверяем ответ downstream
         * ==========================================
         */

        String errorId =
                nestedPayloadExtractor.extractErrorId(
                        downstreamResponse.getBody()
                );

        /*
         * ==========================================
         * 11. Формируем ответ клиенту
         * ==========================================
         */

        String responseXml =
                "0".equals(errorId)
                        ? envelopeBuilder.buildAck(
                        "SUCCESS"
                )
                        : envelopeBuilder.buildAck(
                        "ERROR"
                );

        return ResponseEntity
                .status(HttpStatus.OK)
                .contentType(
                        MediaType.APPLICATION_XML
                )
                .body(responseXml);
    }

    /**
     * Безопасный XML parser для внешнего SOAP.
     */
    private Document parseXml(
            String xml
    ) {

        try {

            if (xml == null || xml.isBlank()) {
                throw new IllegalArgumentException(
                        "Входящий XML пуст"
                );
            }

            DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();

            factory.setNamespaceAware(true);

            /*
             * ======================================
             * XXE protection
             * ======================================
             */

            factory.setFeature(
                    "http://apache.org/xml/features/disallow-doctype-decl",
                    true
            );

            factory.setFeature(
                    "http://xml.org/sax/features/external-general-entities",
                    false
            );

            factory.setFeature(
                    "http://xml.org/sax/features/external-parameter-entities",
                    false
            );

            factory.setFeature(
                    "http://apache.org/xml/features/nonvalidating/load-external-dtd",
                    false
            );

            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            factory.setAttribute(
                    XMLConstants.ACCESS_EXTERNAL_DTD,
                    ""
            );

            factory.setAttribute(
                    XMLConstants.ACCESS_EXTERNAL_SCHEMA,
                    ""
            );

            DocumentBuilder builder =
                    factory.newDocumentBuilder();

            return builder.parse(
                    new InputSource(
                            new StringReader(xml)
                    )
            );

        } catch (Exception e) {

            throw new IllegalArgumentException(
                    "Некорректный входящий XML",
                    e
            );
        }
    }

    /**
     * Нормализация IP.
     */
    private String normalizeIp(
            String ip
    ) {

        if (ip == null || ip.isBlank()) {
            return "127.0.0.1";
        }

        if (
                ip.equals("0:0:0:0:0:0:0:1")
                        || ip.equals("::1")
        ) {
            return "127.0.0.1";
        }

        return ip;
    }
}