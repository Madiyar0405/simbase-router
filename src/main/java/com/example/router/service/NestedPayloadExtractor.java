package com.example.router.service;

import com.example.router.model.Candidate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Входящее сообщение — SOAP-конверт (SendMessage), внутри которого
 * в элементе <data> (requestData/data) лежит ВЛОЖЕННЫЙ XML-документ
 * (например <cvRecruit>...), обычно обёрнутый в CDATA, со своим ИИН
 * в <cv><iin>.
 * Это отдельный, самостоятельный XML-документ внутри текстового
 * содержимого <data> — поэтому его нужно парсить второй раз, отдельно
 * от внешнего SOAP-документа.
 */
@Component
public class NestedPayloadExtractor {

    private final DocumentBuilderFactory factory;
    private final XPath xPath = XPathFactory.newInstance().newXPath();

    public NestedPayloadExtractor() {
        this.factory = DocumentBuilderFactory.newInstance();
        this.factory.setNamespaceAware(true);
        try {
            // защита от XXE
            this.factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось настроить DocumentBuilderFactory", e);
        }
    }

    /**
     * Достаёт текстовое содержимое элемента <data> (независимо от вложенности —
     * ищет по local-name, без учёта namespace) из внешнего SOAP-документа.
     */
    public String extractDataElementText(Document outerDoc) {
        try {
            String text = (String) xPath.evaluate("//*[local-name()='data']", outerDoc, XPathConstants.STRING);
            if (text == null || text.isBlank()) {
                throw new IllegalArgumentException("Элемент <data> не найден или пуст во входящем XML");
            }
            return text.trim();
        } catch (Exception e) {
            throw new IllegalArgumentException("Ошибка извлечения <data> из входящего XML", e);
        }
    }

    /**
     * Парсит вложенный XML (содержимое <data>) как отдельный документ
     * и достаёт из него ИИН (первый попавшийся элемент <iin> по local-name).
     */
    public String extractBin(String innerXml) {
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document innerDoc = builder.parse(new InputSource(new StringReader(innerXml)));
            String bin = (String) xPath.evaluate("//*[local-name()='bin']", innerDoc, XPathConstants.STRING);
            if (bin == null || bin.isBlank()) {
                throw new IllegalArgumentException("Элемент <bin> не найден во вложенном XML");
            }
            return bin.trim();
        } catch (Exception e) {
            throw new IllegalArgumentException("Ошибка парсинга вложенного XML из <data>", e);
        }
    }

    public String extractCandidateDataAndParseJson(String dataPayload){
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document innerDoc = builder.parse(new InputSource(new StringReader(dataPayload)));



            Candidate candidate = new Candidate(
                    (String) xPath.evaluate("//*[local-name()='lastName']", innerDoc, XPathConstants.STRING),
                     (String) xPath.evaluate("//*[local-name()='firstName']", innerDoc, XPathConstants.STRING),
                     (String) xPath.evaluate("//*[local-name()='parentName']", innerDoc, XPathConstants.STRING),
                     (String) xPath.evaluate("//*[local-name()='email']", innerDoc, XPathConstants.STRING),
                     (String) xPath.evaluate("//*[local-name()='mobile']", innerDoc, XPathConstants.STRING),
                     (String) xPath.evaluate("//*[local-name()='experience']", innerDoc, XPathConstants.STRING),
                     (String) xPath.evaluate("//*[local-name()='desiredSalary']", innerDoc, XPathConstants.STRING),
                    (String) xPath.evaluate("//*[local-name()='consentRelocate']", innerDoc, XPathConstants.STRING),
                     (String) xPath.evaluate("//*[local-name()='consentWork']", innerDoc, XPathConstants.STRING),
                     (String) xPath.evaluate("//*[local-name()='dateBirth']", innerDoc, XPathConstants.STRING),
                     (String) xPath.evaluate("//*[local-name()='bDate']", innerDoc, XPathConstants.STRING),
                    (String) xPath.evaluate("//*[local-name()='eDate']", innerDoc, XPathConstants.STRING),
                     (String) xPath.evaluate("//*[local-name()='dateCreate']", innerDoc, XPathConstants.STRING),
                    (String) xPath.evaluate("//*[local-name()='msgDate']", innerDoc, XPathConstants.STRING));
            ObjectMapper mapper = new ObjectMapper();

            return mapper.writeValueAsString(candidate);
        }

        catch (Exception e) {
                throw new IllegalArgumentException("Ошибка парсинга вложенного1111 XML из <data>", e);


        }

    }
}