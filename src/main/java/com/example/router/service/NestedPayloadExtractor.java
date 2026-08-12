package com.example.router.service;

import com.example.router.model.CvRecruitResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

@Component
public class NestedPayloadExtractor {

    private final DocumentBuilderFactory factory;
    private final XPath xPath = XPathFactory.newInstance().newXPath();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public NestedPayloadExtractor() {
        this.factory = createSecureDocumentBuilderFactory();
    }

    /**
     * Создаёт безопасный XML parser.
     */
    private DocumentBuilderFactory createSecureDocumentBuilderFactory() {

        DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();

        factory.setNamespaceAware(true);

        try {

            /*
             * Запрещаем DOCTYPE.
             */
            factory.setFeature(
                    "http://apache.org/xml/features/disallow-doctype-decl",
                    true
            );

            /*
             * Запрещаем external entities.
             */
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

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Не удалось настроить XML-парсер",
                    e
            );
        }

        return factory;
    }

    /**
     * Извлекает senderId из SOAP.
     */
    public String extractLogin(Document outerDoc) {

        try {

            String text =
                    (String) xPath.evaluate(
                            "//*[local-name()='senderId']",
                            outerDoc,
                            XPathConstants.STRING
                    );

            if (text == null || text.isBlank()) {
                throw new IllegalArgumentException(
                        "Элемент <senderId> отсутствует или пуст"
                );
            }

            return text.trim();

        } catch (IllegalArgumentException e) {
            throw e;

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Не удалось извлечь <senderId>",
                    e
            );
        }
    }

    /**
     * Извлекает serviceId.
     */
    public String extractServiceId(Document outerDoc) {

        try {

            String text =
                    (String) xPath.evaluate(
                            "//*[local-name()='serviceId']",
                            outerDoc,
                            XPathConstants.STRING
                    );

            if (text == null || text.isBlank()) {
                throw new IllegalArgumentException(
                        "Элемент <serviceId> отсутствует или пуст"
                );
            }

            return text.trim();

        } catch (IllegalArgumentException e) {
            throw e;

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Не удалось извлечь <serviceId>",
                    e
            );
        }
    }

    /**
     * Извлекает password.
     */
    public String extractPassword(Document outerDoc) {

        try {

            String text =
                    (String) xPath.evaluate(
                            "//*[local-name()='password']",
                            outerDoc,
                            XPathConstants.STRING
                    );

            if (text == null || text.isBlank()) {
                throw new IllegalArgumentException(
                        "Элемент <password> отсутствует или пуст"
                );
            }

            return text.trim();

        } catch (IllegalArgumentException e) {
            throw e;

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Не удалось извлечь <password>",
                    e
            );
        }
    }

    /**
     * Универсальный метод.
     *
     * Поддерживает:
     *
     * 1. Новый формат:
     *
     * <data>
     *     <cvRecruit>
     *         ...
     *     </cvRecruit>
     * </data>
     *
     * 2. Старый формат:
     *
     * <data>
     *     &lt;cvRecruit&gt;
     *         ...
     *     &lt;/cvRecruit&gt;
     * </data>
     *
     * или:
     *
     * <data>
     *     <![CDATA[
     *         <cvRecruit>...</cvRecruit>
     *     ]]>
     * </data>
     */
    public Node extractDataNode(Document outerDoc) {

        try {

            Node dataNode =
                    (Node) xPath.evaluate(
                            "//*[local-name()='data']",
                            outerDoc,
                            XPathConstants.NODE
                    );

            if (dataNode == null) {
                throw new IllegalArgumentException(
                        "Элемент <data> отсутствует во входящем XML"
                );
            }

            /*
             * ==========================================
             * ВАРИАНТ 1
             *
             * <data>
             *     <cvRecruit>...</cvRecruit>
             * </data>
             *
             * То есть XML уже настоящий.
             * ==========================================
             */

            Node cvRecruitNode =
                    (Node) xPath.evaluate(
                            "./*[local-name()='cvRecruit']",
                            dataNode,
                            XPathConstants.NODE
                    );

            if (cvRecruitNode != null) {

                System.out.println(
                        "DATA FORMAT: обычный вложенный XML"
                );

                return dataNode;
            }

            /*
             * ==========================================
             * ВАРИАНТ 2
             *
             * <data>
             *     <![CDATA[
             *         <cvRecruit>...</cvRecruit>
             *     ]]>
             * </data>
             *
             * или XML был экранирован.
             *
             * Получаем текст и парсим его отдельно.
             * ==========================================
             */

            String innerXml =
                    dataNode.getTextContent();

            if (innerXml == null || innerXml.isBlank()) {
                throw new IllegalArgumentException(
                        "Элемент <data> пуст"
                );
            }

            innerXml = innerXml.trim();

            System.out.println(
                    "DATA FORMAT: XML в виде текста/CDATA"
            );

            Document innerDoc =
                    parseXml(innerXml);

            return innerDoc.getDocumentElement();

        } catch (IllegalArgumentException e) {
            throw e;

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Не удалось обработать элемент <data>",
                    e
            );
        }
    }

    /**
     * Определяет systemCode.
     *
     * Получает vacancyCode из уже нормализованного Node.
     */
    public String extractSystemCode(Node dataNode) {

        try {

            if (dataNode == null) {
                throw new IllegalArgumentException(
                        "Элемент <data> отсутствует"
                );
            }

            String vacancyCode =
                    (String) xPath.evaluate(
                            ".//*[local-name()='vacancyCode']",
                            dataNode,
                            XPathConstants.STRING
                    );

            if (vacancyCode == null || vacancyCode.isBlank()) {
                throw new IllegalArgumentException(
                        "Элемент <vacancyCode> отсутствует или пуст"
                );
            }

            vacancyCode =
                    vacancyCode.trim();

            if (vacancyCode.length() < 8) {
                throw new IllegalArgumentException(
                        "Некорректный формат <vacancyCode>: " +
                                "минимум 8 символов"
                );
            }

            /*
             * Например:
             *
             * GCGFCDEV12345678
             *
             * substring(2, 8)
             *
             * GFCDEV
             */
            String systemCode =
                    vacancyCode.substring(2, 8);

            /*
             * Специальное правило GFC.
             */
            if (systemCode.length() >= 6
                    && systemCode.substring(3, 6).equals("000")) {

                systemCode = "GFC";
            }

            return systemCode;

        } catch (IllegalArgumentException e) {
            throw e;

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Не удалось определить systemCode",
                    e
            );
        }
    }

    /**
     * Преобразует cvRecruit в JSON.
     */
    public String extractCandidateDataAndParseJson(
            Node dataNode
    ) {

        try {

            if (dataNode == null) {
                throw new IllegalArgumentException(
                        "Элемент <data> отсутствует"
                );
            }

            CvRecruitResponse response =
                    new CvRecruitResponse();

            CvRecruitResponse.CvRecruit cvRecruit =
                    new CvRecruitResponse.CvRecruit();

            /*
             * Ищем cvRecruit.
             */
            Node cvRecruitNode =
                    (Node) xPath.evaluate(
                            ".//*[local-name()='cvRecruit']",
                            dataNode,
                            XPathConstants.NODE
                    );

            /*
             * Если dataNode сам является cvRecruit
             * (например, старый формат после parseXml).
             */
            if (cvRecruitNode == null
                    && "cvRecruit".equals(
                    dataNode.getLocalName())) {

                cvRecruitNode = dataNode;
            }

            if (cvRecruitNode == null) {
                throw new IllegalArgumentException(
                        "В XML отсутствует элемент <cvRecruit>"
                );
            }

            /*
             * cv.
             */
            Node cvNode =
                    (Node) xPath.evaluate(
                            "./*[local-name()='cv']",
                            cvRecruitNode,
                            XPathConstants.NODE
                    );

            if (cvNode != null) {

                cvRecruit.setCv(
                        parseCv(
                                cvNode,
                                xPath
                        )
                );
            }

            /*
             * recruit.
             */
            Node recruitNode =
                    (Node) xPath.evaluate(
                            "./*[local-name()='recruit']",
                            cvRecruitNode,
                            XPathConstants.NODE
                    );

            if (recruitNode != null) {

                cvRecruit.setRecruit(
                        parseRecruit(
                                recruitNode,
                                xPath
                        )
                );
            }

            if (cvNode == null
                    && recruitNode == null) {

                throw new IllegalArgumentException(
                        "В <cvRecruit> отсутствуют <cv> и <recruit>"
                );
            }

            response.setCvRecruit(
                    cvRecruit
            );

            return objectMapper.writeValueAsString(
                    response
            );

        } catch (IllegalArgumentException e) {
            throw e;

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Не удалось преобразовать cvRecruit в JSON",
                    e
            );
        }
    }

    /**
     * Парсит cv.
     */
    private CvRecruitResponse.Cv parseCv(
            Node cvNode,
            XPath xPath
    ) throws Exception {

        CvRecruitResponse.Cv cv =
                new CvRecruitResponse.Cv();

        cv.setCvType(
                parseDictionary(
                        (Node) xPath.evaluate(
                                "./*[local-name()='cvType']",
                                cvNode,
                                XPathConstants.NODE
                        )
                )
        );

        cv.setIin(
                textOf(cvNode, xPath, "iin")
        );

        cv.setLastName(
                textOf(cvNode, xPath, "lastName")
        );

        cv.setFirstName(
                textOf(cvNode, xPath, "firstName")
        );

        cv.setParentName(
                textOf(cvNode, xPath, "parentName")
        );

        cv.setDateBirth(
                textOf(cvNode, xPath, "dateBirth")
        );

        cv.setSex(
                parseDictionary(
                        (Node) xPath.evaluate(
                                "./*[local-name()='sex']",
                                cvNode,
                                XPathConstants.NODE
                        )
                )
        );

        cv.setCountry(
                parseDictionary(
                        (Node) xPath.evaluate(
                                "./*[local-name()='country']",
                                cvNode,
                                XPathConstants.NODE
                        )
                )
        );

        cv.setRegion(
                parseDictionary(
                        (Node) xPath.evaluate(
                                "./*[local-name()='region']",
                                cvNode,
                                XPathConstants.NODE
                        )
                )
        );

        cv.setEducation(
                parseDictionary(
                        (Node) xPath.evaluate(
                                "./*[local-name()='education']",
                                cvNode,
                                XPathConstants.NODE
                        )
                )
        );

        cv.setEmail(
                textOf(cvNode, xPath, "email")
        );

        cv.setMobile(
                textOf(cvNode, xPath, "mobile")
        );

        String consentRelocate =
                textOf(
                        cvNode,
                        xPath,
                        "consentRelocate"
                );

        if (!consentRelocate.isEmpty()) {

            cv.setConsentRelocate(
                    Boolean.parseBoolean(
                            consentRelocate
                    )
            );
        }

        cv.setCode(
                textOf(
                        cvNode,
                        xPath,
                        "code"
                )
        );

        cv.setProfArea(
                parseDictionary(
                        (Node) xPath.evaluate(
                                "./*[local-name()='profArea']",
                                cvNode,
                                XPathConstants.NODE
                        )
                )
        );

        cv.setProfession(
                parseDictionary(
                        (Node) xPath.evaluate(
                                "./*[local-name()='profession']",
                                cvNode,
                                XPathConstants.NODE
                        )
                )
        );

        cv.setDesiredNote(
                textOf(
                        cvNode,
                        xPath,
                        "desiredNote"
                )
        );

        String experience =
                textOf(
                        cvNode,
                        xPath,
                        "experience"
                );

        if (!experience.isEmpty()) {

            try {

                cv.setExperience(
                        Integer.parseInt(
                                experience
                        )
                );

            } catch (NumberFormatException e) {

                throw new IllegalArgumentException(
                        "Некорректное значение <experience>",
                        e
                );
            }
        }

        String salary =
                textOf(
                        cvNode,
                        xPath,
                        "desiredSalary"
                );

        if (!salary.isEmpty()) {

            try {

                cv.setDesiredSalary(
                        Integer.parseInt(
                                salary
                        )
                );

            } catch (NumberFormatException e) {

                throw new IllegalArgumentException(
                        "Некорректное значение <desiredSalary>",
                        e
                );
            }
        }

        cv.setAccountGoal(
                parseDictionary(
                        (Node) xPath.evaluate(
                                "./*[local-name()='accountGoal']",
                                cvNode,
                                XPathConstants.NODE
                        )
                )
        );

        cv.setWorkSpec(
                parseDictionary(
                        (Node) xPath.evaluate(
                                "./*[local-name()='workSpec']",
                                cvNode,
                                XPathConstants.NODE
                        )
                )
        );

        cv.setDateCreate(
                textOf(
                        cvNode,
                        xPath,
                        "dateCreate"
                )
        );

        /*
         * cvExperienceList.
         */
        NodeList expNodes =
                (NodeList) xPath.evaluate(
                        "./*[local-name()='cvExperienceList']",
                        cvNode,
                        XPathConstants.NODESET
                );

        List<CvRecruitResponse.CvExperience> expList =
                new ArrayList<>();

        for (int i = 0;
             i < expNodes.getLength();
             i++) {

            expList.add(
                    parseExperience(
                            expNodes.item(i),
                            xPath
                    )
            );
        }

        cv.setCvExperienceList(
                expList
        );

        return cv;
    }

    /**
     * Парсит опыт работы.
     */
    private CvRecruitResponse.CvExperience parseExperience(
            Node node,
            XPath xPath
    ) throws Exception {

        CvRecruitResponse.CvExperience exp =
                new CvRecruitResponse.CvExperience();

        exp.setProfession(
                parseDictionary(
                        (Node) xPath.evaluate(
                                "./*[local-name()='profession']",
                                node,
                                XPathConstants.NODE
                        )
                )
        );

        exp.setProfNote(
                textOf(
                        node,
                        xPath,
                        "profNote"
                )
        );

        exp.setEmpName(
                textOf(
                        node,
                        xPath,
                        "empName"
                )
        );

        exp.setDuties(
                textOf(
                        node,
                        xPath,
                        "duties"
                )
        );

        exp.setBDate(
                textOf(
                        node,
                        xPath,
                        "bDate"
                )
        );

        exp.setEDate(
                textOf(
                        node,
                        xPath,
                        "eDate"
                )
        );

        String consentWork =
                textOf(
                        node,
                        xPath,
                        "consentWork"
                );

        if (!consentWork.isEmpty()) {

            exp.setConsentWork(
                    Boolean.parseBoolean(
                            consentWork
                    )
            );
        }

        return exp;
    }

    /**
     * Парсит recruit.
     */
    private CvRecruitResponse.Recruit parseRecruit(
            Node node,
            XPath xPath
    ) throws Exception {

        CvRecruitResponse.Recruit recruit =
                new CvRecruitResponse.Recruit();

        recruit.setRecruitCode(
                textOf(
                        node,
                        xPath,
                        "recruitCode"
                )
        );

        recruit.setMsgDate(
                textOf(
                        node,
                        xPath,
                        "msgDate"
                )
        );

        recruit.setVacancyCode(
                textOf(
                        node,
                        xPath,
                        "vacancyCode"
                )
        );

        recruit.setCodeIin(
                textOf(
                        node,
                        xPath,
                        "codeIin"
                )
        );

        recruit.setCvCode(
                textOf(
                        node,
                        xPath,
                        "cvCode"
                )
        );

        recruit.setStatus(
                parseDictionary(
                        (Node) xPath.evaluate(
                                "./*[local-name()='status']",
                                node,
                                XPathConstants.NODE
                        )
                )
        );

        recruit.setMsgText(
                textOf(
                        node,
                        xPath,
                        "msgText"
                )
        );

        return recruit;
    }

    /**
     * Парсит dictionary.
     */
    private CvRecruitResponse.Dictionary parseDictionary(
            Node node
    ) {

        if (node == null) {
            return null;
        }

        CvRecruitResponse.Dictionary dict =
                new CvRecruitResponse.Dictionary();

        NodeList children =
                node.getChildNodes();

        for (int i = 0;
             i < children.getLength();
             i++) {

            Node child =
                    children.item(i);

            if ("code".equals(
                    child.getLocalName())) {

                dict.setCode(
                        child.getTextContent().trim()
                );
            }

            if ("name".equals(
                    child.getLocalName())) {

                dict.setName(
                        child.getTextContent().trim()
                );
            }
        }

        return dict;
    }

    /**
     * Получает текст непосредственного
     * дочернего элемента.
     */
    private String textOf(
            Node contextNode,
            XPath xPath,
            String tag
    ) throws Exception {

        String value =
                (String) xPath.evaluate(
                        "./*[local-name()='" + tag + "']",
                        contextNode,
                        XPathConstants.STRING
                );

        return value == null
                ? ""
                : value.trim();
    }

    /**
     * Парсит XML String.
     */
    private Document parseXml(String xml)
            throws Exception {

        if (xml == null || xml.isBlank()) {
            throw new IllegalArgumentException(
                    "XML пуст"
            );
        }

        DocumentBuilder builder =
                factory.newDocumentBuilder();

        return builder.parse(
                new InputSource(
                        new StringReader(xml)
                )
        );
    }

    /**
     * Извлекает error id из ответа downstream.
     */
    public String extractErrorId(
            String responseXml
    ) {

        try {

            if (responseXml == null
                    || responseXml.isBlank()) {

                throw new IllegalArgumentException(
                        "Пустой ответ от downstream-системы"
                );
            }

            Document doc =
                    parseXml(responseXml);

            String errorId =
                    (String) xPath.evaluate(
                            "//*[local-name()='error']/@id",
                            doc,
                            XPathConstants.STRING
                    );

            if (errorId == null
                    || errorId.isBlank()) {

                throw new IllegalArgumentException(
                        "Элемент <error id=\"...\"/> " +
                                "отсутствует в ответе downstream-системы"
                );
            }

            return errorId.trim();

        } catch (IllegalArgumentException e) {
            throw e;

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Не удалось разобрать ответ downstream-системы",
                    e
            );
        }
    }
}