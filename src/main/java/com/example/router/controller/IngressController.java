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

/**
 * Принимает SOAP-конверт вида:
 *
 * <SOAP-ENV:Envelope>...<request><requestData><data>
 *   <![CDATA[ <cvRecruit><cv><iin>...</iin></cv></cvRecruit> ]]>
 * </data>...
 *
 * Извлекает системный код из вложенного XML внутри <data> и по нему
 * определяет, в какой downstream-сервис отправить итоговый sbapi-конверт.
 * В ответ клиенту возвращает собственный SOAP-конверт-подтверждение
 * (SendMessageResponse), а не транзитом ответ downstream-системы.
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

        // Явно декодируем как UTF-8, не полагаясь на автоопределение charset
        // Spring'ом по Content-Type (без явного charset может уйти в ISO-8859-1
        // и испортить кириллицу).
        String incomingXml = new String(incomingBytes, StandardCharsets.UTF_8);
        Document outerDoc = parseXml(incomingXml);
        String senderId = nestedPayloadExtractor.extractLogin(outerDoc);
        String password = nestedPayloadExtractor.extractPassword(outerDoc);
        String serviceId = nestedPayloadExtractor.extractServiceId(outerDoc);

        // Методы бросают исключение при неуспешной проверке,
        // возвращаемое значение намеренно не используется.
        incomingAuthService.isAuthorized(senderId, password);
        incomingAuthService.isServiceIdCorrect(serviceId);

        // 1. Достаём текст элемента <data> (обычно CDATA с вложенным XML)
        String innerXml = nestedPayloadExtractor.extractDataElementText(outerDoc);

        // 2. Парсим вложенный XML отдельно и достаём системный код
        String systemCode = nestedPayloadExtractor.extractSystemCode(innerXml);

        // 3. Находим маршрут по системному коду (routes.*.system-code в application.yml)
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

        ResponseEntity<String> downstreamResponse = outboundClient.send(cfg.getUrl(), outgoingXml);

        // Проверяем <error id="..."/> в ответе downstream-системы:
        // "0" — успех, любое другое значение — ошибка.
        String errorId = nestedPayloadExtractor.extractErrorId(downstreamResponse.getBody());

        String responseXml = "0".equals(errorId)
                ? envelopeBuilder.buildAck("SUCCESS")
                : envelopeBuilder.buildAck("ERROR");

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