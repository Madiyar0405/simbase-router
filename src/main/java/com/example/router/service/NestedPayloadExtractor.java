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
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.List;

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


    public String extractLogin(Document outerDoc){
        try {
            String text = (String) xPath.evaluate("//*[local-name()='senderId']", outerDoc, XPathConstants.STRING);
            if (text == null || text.isBlank()) {
                throw new IllegalArgumentException("Элемент <data> не найден или пуст во входящем XML");
            }
            return text.trim();
        } catch (Exception e) {
            throw new IllegalArgumentException("Ошибка извлечения <data> из входящего XML", e);
        }
    }

    public String extractPassword(Document outerDoc){
        try {
            String text = (String) xPath.evaluate("//*[local-name()='password']", outerDoc, XPathConstants.STRING);
            if (text == null || text.isBlank()) {
                throw new IllegalArgumentException("Элемент <data> не найден или пуст во входящем XML");
            }
            return text.trim();
        } catch (Exception e) {
            throw new IllegalArgumentException("Ошибка извлечения <data> из входящего XML", e);
        }
    }

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
    public String extractSystemCode(String innerXml) {
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document innerDoc = builder.parse(new InputSource(new StringReader(innerXml)));
            String vacancyCode = (String) xPath.evaluate("//*[local-name()='vacancyCode']", innerDoc, XPathConstants.STRING);
            String systemCodeFromVacancyCode = vacancyCode.substring(2,8);
            if(systemCodeFromVacancyCode.substring(3,6).equals("000")){
                systemCodeFromVacancyCode = "GFC";

            }
            if (vacancyCode == null || vacancyCode.isBlank()) {
                throw new IllegalArgumentException("Элемент <vacancyCode> не найден во вложенном XML");
            }
            System.out.println(systemCodeFromVacancyCode);
            return systemCodeFromVacancyCode.trim();
        } catch (Exception e) {
            throw new IllegalArgumentException("Ошибка парсинга вложенного XML из <data>", e);
        }
    }

    public String extractCandidateDataAndParseJson(String dataPayload) {
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document innerDoc = builder.parse(new InputSource(new StringReader(dataPayload)));
            innerDoc.getDocumentElement().normalize();

            CvRecruitResponse response = new CvRecruitResponse();
            CvRecruitResponse.CvRecruit cvRecruit = new CvRecruitResponse.CvRecruit();

            Node cvNode = (Node) xPath.evaluate("//*[local-name()='cv']", innerDoc, XPathConstants.NODE);
            if (cvNode != null) {
                cvRecruit.setCv(parseCv(cvNode, xPath));
            }

            Node recruitNode = (Node) xPath.evaluate("//*[local-name()='recruit']", innerDoc, XPathConstants.NODE);
            if (recruitNode != null) {
                cvRecruit.setRecruit(parseRecruit(recruitNode, xPath));
            }

            response.setCvRecruit(cvRecruit);

            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(response);
            System.out.println(json);
            return json;

        } catch (Exception e) {
            throw new IllegalArgumentException("Ошибка парсинга вложенного XML из <data>", e);
        }
    } // <-- метод extractCandidateDataAndParseJson закрывается здесь

// Дальше методы идут на уровне класса, а не внутри предыдущего метода:

    private CvRecruitResponse.Cv parseCv(Node cvNode, XPath xPath) throws Exception {
        CvRecruitResponse.Cv cv = new CvRecruitResponse.Cv();

        cv.setCvType(parseDictionary((Node) xPath.evaluate("./*[local-name()='cvType']", cvNode, XPathConstants.NODE)));
        cv.setIin(textOf(cvNode, xPath, "iin"));
        cv.setLastName(textOf(cvNode, xPath, "lastName"));
        cv.setFirstName(textOf(cvNode, xPath, "firstName"));
        cv.setParentName(textOf(cvNode, xPath, "parentName"));
        cv.setDateBirth(textOf(cvNode, xPath, "dateBirth"));

        cv.setSex(parseDictionary((Node) xPath.evaluate("./*[local-name()='sex']", cvNode, XPathConstants.NODE)));
        cv.setCountry(parseDictionary((Node) xPath.evaluate("./*[local-name()='country']", cvNode, XPathConstants.NODE)));
        cv.setRegion(parseDictionary((Node) xPath.evaluate("./*[local-name()='region']", cvNode, XPathConstants.NODE)));
        cv.setEducation(parseDictionary((Node) xPath.evaluate("./*[local-name()='education']", cvNode, XPathConstants.NODE)));

        cv.setEmail(textOf(cvNode, xPath, "email"));
        cv.setMobile(textOf(cvNode, xPath, "mobile"));

        String consentRelocate = textOf(cvNode, xPath, "consentRelocate");
        if (!consentRelocate.isEmpty()) cv.setConsentRelocate(Boolean.parseBoolean(consentRelocate));

        cv.setCode(textOf(cvNode, xPath, "code"));

        cv.setProfArea(parseDictionary((Node) xPath.evaluate("./*[local-name()='profArea']", cvNode, XPathConstants.NODE)));
        cv.setProfession(parseDictionary((Node) xPath.evaluate("./*[local-name()='profession']", cvNode, XPathConstants.NODE)));

        cv.setDesiredNote(textOf(cvNode, xPath, "desiredNote"));

        String experience = textOf(cvNode, xPath, "experience");
        if (!experience.isEmpty()) cv.setExperience(Integer.parseInt(experience));

        String salary = textOf(cvNode, xPath, "desiredSalary");
        if (!salary.isEmpty()) cv.setDesiredSalary(Integer.parseInt(salary));

        cv.setAccountGoal(parseDictionary((Node) xPath.evaluate("./*[local-name()='accountGoal']", cvNode, XPathConstants.NODE)));
        cv.setWorkSpec(parseDictionary((Node) xPath.evaluate("./*[local-name()='workSpec']", cvNode, XPathConstants.NODE)));

        cv.setDateCreate(textOf(cvNode, xPath, "dateCreate"));

        NodeList expNodes = (NodeList) xPath.evaluate("./*[local-name()='cvExperienceList']", cvNode, XPathConstants.NODESET);
        List<CvRecruitResponse.CvExperience> expList = new ArrayList<>();
        for (int i = 0; i < expNodes.getLength(); i++) {
            expList.add(parseExperience(expNodes.item(i), xPath));
        }
        cv.setCvExperienceList(expList);

        return cv;
    }

    private CvRecruitResponse.CvExperience parseExperience(Node node, XPath xPath) throws Exception {
        CvRecruitResponse.CvExperience exp = new CvRecruitResponse.CvExperience();
        exp.setProfession(parseDictionary((Node) xPath.evaluate("./*[local-name()='profession']", node, XPathConstants.NODE)));
        exp.setProfNote(textOf(node, xPath, "profNote"));
        exp.setEmpName(textOf(node, xPath, "empName"));
        exp.setDuties(textOf(node, xPath, "duties"));
        exp.setBDate(textOf(node, xPath, "bDate"));
        exp.setEDate(textOf(node, xPath, "eDate"));

        String consentWork = textOf(node, xPath, "consentWork");
        if (!consentWork.isEmpty()) exp.setConsentWork(Boolean.parseBoolean(consentWork));

        return exp;
    }

    private CvRecruitResponse.Recruit parseRecruit(Node node, XPath xPath) throws Exception {
        CvRecruitResponse.Recruit recruit = new CvRecruitResponse.Recruit();
        recruit.setRecruitCode(textOf(node, xPath, "recruitCode"));
        recruit.setMsgDate(textOf(node, xPath, "msgDate"));
        recruit.setVacancyCode(textOf(node, xPath, "vacancyCode"));
        recruit.setCodeIin(textOf(node, xPath, "codeIin"));
        recruit.setCvCode(textOf(node, xPath, "cvCode"));
        recruit.setStatus(parseDictionary((Node) xPath.evaluate("./*[local-name()='status']", node, XPathConstants.NODE)));
        recruit.setMsgText(textOf(node, xPath, "msgText"));
        return recruit;
    }

    private CvRecruitResponse.Dictionary parseDictionary(Node node) {
        if (node == null) return null;
        CvRecruitResponse.Dictionary dict = new CvRecruitResponse.Dictionary();
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if ("code".equals(child.getLocalName())) dict.setCode(child.getTextContent());
            if ("name".equals(child.getLocalName())) dict.setName(child.getTextContent());
        }
        return dict;
    }

    private String textOf(Node contextNode, XPath xPath, String tag) throws Exception {
        return (String) xPath.evaluate("./*[local-name()='" + tag + "']", contextNode, XPathConstants.STRING);
    }
}