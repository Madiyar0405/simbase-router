package com.example.router.service;

import com.example.router.model.CvRecruitResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

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

    public NestedPayloadExtractor() {
        this.factory = DocumentBuilderFactory.newInstance();
        this.factory.setNamespaceAware(true);

        try {
            // Защита от XXE
            this.factory.setFeature(
                    "http://apache.org/xml/features/disallow-doctype-decl",
                    true
            );
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Не удалось настроить XML-парсер",
                    e
            );
        }
    }

    /**
     * Извлекает senderId из внешнего SOAP XML.
     */
    public String extractLogin(Document outerDoc) {
        try {
            String text = (String) xPath.evaluate(
                    "//*[local-name()='senderId']",
                    outerDoc,
                    XPathConstants.STRING
            );

            if (text == null || text.isBlank()) {
                throw new IllegalArgumentException(
                        "Элемент <senderId> отсутствует или пуст во входящем XML"
                );
            }

            return text.trim();

        } catch (IllegalArgumentException e) {
            throw e;

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Не удалось извлечь элемент <senderId> из входящего XML",
                    e
            );
        }
    }

    /**
     * Извлекает serviceId из внешнего SOAP XML.
     */
    public String extractServiceId(Document outerDoc) {
        try {
            String text = (String) xPath.evaluate(
                    "//*[local-name()='serviceId']",
                    outerDoc,
                    XPathConstants.STRING
            );

            if (text == null || text.isBlank()) {
                throw new IllegalArgumentException(
                        "Элемент <serviceId> отсутствует или пуст во входящем XML"
                );
            }

            return text.trim();

        } catch (IllegalArgumentException e) {
            throw e;

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Не удалось извлечь элемент <serviceId> из входящего XML",
                    e
            );
        }
    }
    public String extractErrorId(String responseXml) {
        try {
            if (responseXml == null || responseXml.isBlank()) {
                throw new IllegalArgumentException("Пустой ответ от downstream-системы");
            }

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(responseXml)));

            String errorId = (String) xPath.evaluate(
                    "//*[local-name()='error']/@id",
                    doc,
                    XPathConstants.STRING
            );

            if (errorId == null || errorId.isBlank()) {
                throw new IllegalArgumentException(
                        "Элемент <error id=\"...\"/> отсутствует в ответе downstream-системы"
                );
            }

            return errorId.trim();

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Не удалось разобрать ответ downstream-системы", e
            );
        }
    }
    /**
     * Извлекает password из внешнего SOAP XML.
     */
    public String extractPassword(Document outerDoc) {
        try {
            String text = (String) xPath.evaluate(
                    "//*[local-name()='password']",
                    outerDoc,
                    XPathConstants.STRING
            );

            if (text == null || text.isBlank()) {
                throw new IllegalArgumentException(
                        "Элемент <password> отсутствует или пуст во входящем XML"
                );
            }

            return text.trim();

        } catch (IllegalArgumentException e) {
            throw e;

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Не удалось извлечь элемент <password> из входящего XML",
                    e
            );
        }
    }

    /**
     * Извлекает содержимое элемента <data>
     * из внешнего SOAP XML.
     */
    public String extractDataElementText(Document outerDoc) {
        try {
            String text = (String) xPath.evaluate(
                    "//*[local-name()='data']",
                    outerDoc,
                    XPathConstants.STRING
            );

            if (text == null || text.isBlank()) {
                throw new IllegalArgumentException(
                        "Элемент <data> отсутствует или пуст во входящем XML"
                );
            }

            return text.trim();

        } catch (IllegalArgumentException e) {
            throw e;

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Не удалось извлечь элемент <data> из входящего XML",
                    e
            );
        }
    }

    /**
     * Извлекает код системы из vacancyCode
     * вложенного XML.
     */
    public String extractSystemCode(String innerXml) {
        try {
            if (innerXml == null || innerXml.isBlank()) {
                throw new IllegalArgumentException(
                        "Вложенный XML в элементе <data> отсутствует или пуст"
                );
            }

            DocumentBuilder builder = factory.newDocumentBuilder();

            Document innerDoc = builder.parse(
                    new InputSource(new StringReader(innerXml))
            );

            String vacancyCode = (String) xPath.evaluate(
                    "//*[local-name()='vacancyCode']",
                    innerDoc,
                    XPathConstants.STRING
            );

            if (vacancyCode == null || vacancyCode.isBlank()) {
                throw new IllegalArgumentException(
                        "Элемент <vacancyCode> отсутствует или пуст во вложенном XML"
                );
            }

            vacancyCode = vacancyCode.trim();

            /*
             * Для получения systemCode используется
             * символы с 3-го по 8-й позиции.
             *
             * Например:
             * vacancyCode = XX123456...
             * systemCode = 123456
             */
            if (vacancyCode.length() < 8) {
                throw new IllegalArgumentException(
                        "Некорректный формат <vacancyCode>: значение должно содержать не менее 8 символов"
                );
            }

            String systemCode = vacancyCode.substring(2, 8);

            /*
             * Специальное правило для GFC.
             */
            if (systemCode.substring(3, 6).equals("000")) {
                systemCode = "GFC";
            }

            return systemCode;

        } catch (IllegalArgumentException e) {
            throw e;

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Не удалось определить код системы из <vacancyCode>",
                    e
            );
        }
    }

    /**
     * Парсит вложенный XML из <data>,
     * преобразует данные cv/recruit в объект,
     * затем сериализует его в JSON.
     */
    public String extractCandidateDataAndParseJson(String dataPayload) {
        try {
            if (dataPayload == null || dataPayload.isBlank()) {
                throw new IllegalArgumentException(
                        "Вложенный XML в элементе <data> отсутствует или пуст"
                );
            }

            DocumentBuilder builder = factory.newDocumentBuilder();

            Document innerDoc = builder.parse(
                    new InputSource(new StringReader(dataPayload))
            );

            innerDoc.getDocumentElement().normalize();

            CvRecruitResponse response = new CvRecruitResponse();
            CvRecruitResponse.CvRecruit cvRecruit =
                    new CvRecruitResponse.CvRecruit();

            /*
             * Обработка <cv>.
             */
            Node cvNode = (Node) xPath.evaluate(
                    "//*[local-name()='cv']",
                    innerDoc,
                    XPathConstants.NODE
            );

            if (cvNode != null) {
                cvRecruit.setCv(
                        parseCv(cvNode, xPath)
                );
            }

            /*
             * Обработка <recruit>.
             */
            Node recruitNode = (Node) xPath.evaluate(
                    "//*[local-name()='recruit']",
                    innerDoc,
                    XPathConstants.NODE
            );

            if (recruitNode != null) {
                cvRecruit.setRecruit(
                        parseRecruit(recruitNode, xPath)
                );
            }

            /*
             * Если отсутствуют оба блока,
             * XML не содержит ожидаемых данных.
             */
            if (cvNode == null && recruitNode == null) {
                throw new IllegalArgumentException(
                        "Во вложенном XML отсутствуют элементы <cv> и <recruit>"
                );
            }

            response.setCvRecruit(cvRecruit);

            /*
             * Преобразование объекта в JSON.
             */
            try {
                ObjectMapper mapper = new ObjectMapper();

                return mapper.writeValueAsString(response);

            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "Не удалось преобразовать данные кандидата в JSON",
                        e
                );
            }

        } catch (IllegalArgumentException e) {
            throw e;

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Не удалось обработать данные кандидата из элемента <data>",
                    e
            );
        }
    }

    /**
     * Парсит элемент <cv>.
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
                textOf(cvNode, xPath, "consentRelocate");

        if (!consentRelocate.isEmpty()) {
            cv.setConsentRelocate(
                    Boolean.parseBoolean(consentRelocate)
            );
        }

        cv.setCode(
                textOf(cvNode, xPath, "code")
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
                textOf(cvNode, xPath, "desiredNote")
        );

        /*
         * Experience.
         */
        String experience =
                textOf(cvNode, xPath, "experience");

        if (!experience.isEmpty()) {
            try {
                cv.setExperience(
                        Integer.parseInt(experience)
                );
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Некорректное значение <experience>: ожидается целое число",
                        e
                );
            }
        }

        /*
         * Desired salary.
         */
        String salary =
                textOf(cvNode, xPath, "desiredSalary");

        if (!salary.isEmpty()) {
            try {
                cv.setDesiredSalary(
                        Integer.parseInt(salary)
                );
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Некорректное значение <desiredSalary>: ожидается целое число",
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
                textOf(cvNode, xPath, "dateCreate")
        );

        /*
         * Опыт работы.
         */
        NodeList expNodes =
                (NodeList) xPath.evaluate(
                        "./*[local-name()='cvExperienceList']",
                        cvNode,
                        XPathConstants.NODESET
                );

        List < CvRecruitResponse.CvExperience > expList =
                new ArrayList < > ();

        for (int i = 0; i < expNodes.getLength(); i++) {
            expList.add(
                    parseExperience(
                            expNodes.item(i),
                            xPath
                    )
            );
        }

        cv.setCvExperienceList(expList);

        return cv;
    }

    /**
     * Парсит элемент опыта работы.
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
                textOf(node, xPath, "profNote")
        );

        exp.setEmpName(
                textOf(node, xPath, "empName")
        );

        exp.setDuties(
                textOf(node, xPath, "duties")
        );

        exp.setBDate(
                textOf(node, xPath, "bDate")
        );

        exp.setEDate(
                textOf(node, xPath, "eDate")
        );

        String consentWork =
                textOf(node, xPath, "consentWork");

        if (!consentWork.isEmpty()) {
            exp.setConsentWork(
                    Boolean.parseBoolean(consentWork)
            );
        }

        return exp;
    }

    /**
     * Парсит элемент <recruit>.
     */
    private CvRecruitResponse.Recruit parseRecruit(
            Node node,
            XPath xPath
    ) throws Exception {

        CvRecruitResponse.Recruit recruit =
                new CvRecruitResponse.Recruit();

        recruit.setRecruitCode(
                textOf(node, xPath, "recruitCode")
        );

        recruit.setMsgDate(
                textOf(node, xPath, "msgDate")
        );

        recruit.setVacancyCode(
                textOf(node, xPath, "vacancyCode")
        );

        recruit.setCodeIin(
                textOf(node, xPath, "codeIin")
        );

        recruit.setCvCode(
                textOf(node, xPath, "cvCode")
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
                textOf(node, xPath, "msgText")
        );

        return recruit;
    }

    /**
     * Парсит справочник вида:
     *
     * <dictionary>
     *     <code>...</code>
     *     <name>...</name>
     * </dictionary>
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

        for (int i = 0; i < children.getLength(); i++) {

            Node child =
                    children.item(i);

            if ("code".equals(child.getLocalName())) {
                dict.setCode(
                        child.getTextContent()
                );
            }

            if ("name".equals(child.getLocalName())) {
                dict.setName(
                        child.getTextContent()
                );
            }
        }

        return dict;
    }

    /**
     * Получает текст дочернего элемента.
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

        return value == null ?
                "" :
                value.trim();
    }
}