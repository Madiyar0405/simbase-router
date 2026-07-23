package com.example.router.service;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;

/**
 * Входящее сообщение — SOAP-конверт (SendMessage), внутри которого
 * в элементе <data> (requestData/data) лежит ВЛОЖЕННЫЙ XML-документ
 * (например <cvRecruit>...), обычно обёрнутый в CDATA, со своим ИИН
 * в <cv><iin>.
 *
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
    public String extractIin(String innerXml) {
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document innerDoc = builder.parse(new InputSource(new StringReader(innerXml)));
            String iin = (String) xPath.evaluate("//*[local-name()='iin']", innerDoc, XPathConstants.STRING);
            if (iin == null || iin.isBlank()) {
                throw new IllegalArgumentException("Элемент <iin> не найден во вложенном XML");
            }
            return iin.trim();
        } catch (Exception e) {
            throw new IllegalArgumentException("Ошибка парсинга вложенного XML из <data>", e);
        }
    }
}
