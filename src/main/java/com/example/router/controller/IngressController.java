package com.example.router.controller;

import com.example.router.config.RoutesProperties;
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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

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

    private static final int INTERFACE_VERSION = 8;
    private static final String MSG_TYPE = "5000";

    public IngressController(NestedPayloadExtractor nestedPayloadExtractor,
                             RoutingService routingService,
                             SbApiEnvelopeBuilder envelopeBuilder,
                             OutboundClient outboundClient) {
        this.nestedPayloadExtractor = nestedPayloadExtractor;
        this.routingService = routingService;
        this.envelopeBuilder = envelopeBuilder;
        this.outboundClient = outboundClient;
    }

    @PostMapping(value = "/route", consumes = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> route(@RequestBody byte[] incomingBytes, HttpServletRequest request) {

        // Явно декодируем как UTF-8, не полагаясь на автоопределение charset
        // Spring'ом по Content-Type (без явного charset может уйти в ISO-8859-1
        // и испортить кириллицу).
        String incomingXml = new String(incomingBytes, StandardCharsets.UTF_8);

        Document outerDoc = parseXml(incomingXml);

        // 1. Достаём текст элемента <data> (обычно CDATA с вложенным XML)
        String innerXml = nestedPayloadExtractor.extractDataElementText(outerDoc);

        // 2. Парсим вложенный XML отдельно и достаём ИИН
        String bin = nestedPayloadExtractor.extractBin(innerXml);
        // 3. Находим маршрут по ИИН (routes.*.iins в application.yml)
        RoutingService.RouteMatch match = routingService.resolveRouteByIin(bin);
        RoutesProperties.RouteConfig cfg = match.config();

        // TODO: уточнить окончательно, что должно быть в arg[data] —
        // в референсном скрипте переменная называется $json и содержит "{}",
        // что намекает на JSON, а не сырой XML. Пока кладём вложенный XML
        // (cvRecruit) как есть, экранированный под текст. Замените здесь,
        // если нужен другой формат (JSON и т.п.).

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

        ResponseEntity<String> downstreamResponse = outboundClient.send(cfg.getUrl(), outgoingXml);

        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_XML)
                .body(downstreamResponse.getBody());
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
