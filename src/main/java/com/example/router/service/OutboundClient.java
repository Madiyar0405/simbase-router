package com.example.router.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;

@Component
public class OutboundClient {

    private static final Logger log = LoggerFactory.getLogger(OutboundClient.class);

    // Референсный скрипт шлёт именно этот Content-Type.
    private static final MediaType SOAP_XML_UTF8 =
            new MediaType("application", "soap+xml", StandardCharsets.UTF_8);

    private final RestTemplate restTemplate;

    public OutboundClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ResponseEntity<String> send(String url, String xmlBody) {
        log.info("Outgoing sbapi envelope to {}:\n{}", url, xmlBody);

        // Явно кодируем в UTF-8 байты: String-конвертер Spring по умолчанию
        // может использовать ISO-8859-1, что ломает кириллицу в теле запроса.
        byte[] bodyBytes = xmlBody.getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(SOAP_XML_UTF8);

        HttpEntity<byte[]> entity = new HttpEntity<>(bodyBytes, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        log.info("Response from {}: status={}, body={}", url, response.getStatusCode(), response.getBody());
        return response;
    }
}
