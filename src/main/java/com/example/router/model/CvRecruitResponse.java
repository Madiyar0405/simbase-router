package com.example.router.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CvRecruitResponse {

    private CvRecruit cvRecruit;

    public CvRecruit getCvRecruit() {
        return cvRecruit;
    }

    public void setCvRecruit(CvRecruit cvRecruit) {
        this.cvRecruit = cvRecruit;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CvRecruit {

        private Cv cv;
        private Recruit recruit;

        public Cv getCv() {
            return cv;
        }

        public void setCv(Cv cv) {
            this.cv = cv;
        }

        public Recruit getRecruit() {
            return recruit;
        }

        public void setRecruit(Recruit recruit) {
            this.recruit = recruit;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Cv {

        private Dictionary cvType;
        private String iin;
        private String lastName;
        private String firstName;
        private String parentName;
        private String dateBirth;

        private Dictionary sex;
        private Dictionary country;
        private Dictionary region;
        private Dictionary education;

        private String email;
        private String mobile;
        private Boolean consentRelocate;
        private String code;

        private Dictionary profArea;
        private Dictionary profession;

        private String desiredNote;
        private Integer experience;
        private Integer desiredSalary;

        private Dictionary accountGoal;
        private Dictionary workSpec;

        private String dateCreate;

        private List<CvExperience> cvExperienceList;

        public Dictionary getCvType() {
            return cvType;
        }

        public void setCvType(Dictionary cvType) {
            this.cvType = cvType;
        }

        public String getIin() {
            return iin;
        }

        public void setIin(String iin) {
            this.iin = iin;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getParentName() {
            return parentName;
        }

        public void setParentName(String parentName) {
            this.parentName = parentName;
        }

        public String getDateBirth() {
            return dateBirth;
        }

        public void setDateBirth(String dateBirth) {
            this.dateBirth = dateBirth;
        }

        public Dictionary getSex() {
            return sex;
        }

        public void setSex(Dictionary sex) {
            this.sex = sex;
        }

        public Dictionary getCountry() {
            return country;
        }

        public void setCountry(Dictionary country) {
            this.country = country;
        }

        public Dictionary getRegion() {
            return region;
        }

        public void setRegion(Dictionary region) {
            this.region = region;
        }

        public Dictionary getEducation() {
            return education;
        }

        public void setEducation(Dictionary education) {
            this.education = education;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getMobile() {
            return mobile;
        }

        public void setMobile(String mobile) {
            this.mobile = mobile;
        }

        public Boolean getConsentRelocate() {
            return consentRelocate;
        }

        public void setConsentRelocate(Boolean consentRelocate) {
            this.consentRelocate = consentRelocate;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public Dictionary getProfArea() {
            return profArea;
        }

        public void setProfArea(Dictionary profArea) {
            this.profArea = profArea;
        }

        public Dictionary getProfession() {
            return profession;
        }

        public void setProfession(Dictionary profession) {
            this.profession = profession;
        }

        public String getDesiredNote() {
            return desiredNote;
        }

        public void setDesiredNote(String desiredNote) {
            this.desiredNote = desiredNote;
        }

        public Integer getExperience() {
            return experience;
        }

        public void setExperience(Integer experience) {
            this.experience = experience;
        }

        public Integer getDesiredSalary() {
            return desiredSalary;
        }

        public void setDesiredSalary(Integer desiredSalary) {
            this.desiredSalary = desiredSalary;
        }

        public Dictionary getAccountGoal() {
            return accountGoal;
        }

        public void setAccountGoal(Dictionary accountGoal) {
            this.accountGoal = accountGoal;
        }

        public Dictionary getWorkSpec() {
            return workSpec;
        }

        public void setWorkSpec(Dictionary workSpec) {
            this.workSpec = workSpec;
        }

        public String getDateCreate() {
            return dateCreate;
        }

        public void setDateCreate(String dateCreate) {
            this.dateCreate = dateCreate;
        }

        public List<CvExperience> getCvExperienceList() {
            return cvExperienceList;
        }

        public void setCvExperienceList(List<CvExperience> cvExperienceList) {
            this.cvExperienceList = cvExperienceList;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CvExperience {

        private Dictionary profession;
        private String profNote;
        private String empName;
        private String duties;
        private String bDate;
        private String eDate;
        private Boolean consentWork;

        public Dictionary getProfession() {
            return profession;
        }

        public void setProfession(Dictionary profession) {
            this.profession = profession;
        }

        public String getProfNote() {
            return profNote;
        }

        public void setProfNote(String profNote) {
            this.profNote = profNote;
        }

        public String getEmpName() {
            return empName;
        }

        public void setEmpName(String empName) {
            this.empName = empName;
        }

        public String getDuties() {
            return duties;
        }

        public void setDuties(String duties) {
            this.duties = duties;
        }

        public String getBDate() {
            return bDate;
        }

        public void setBDate(String bDate) {
            this.bDate = bDate;
        }

        public String getEDate() {
            return eDate;
        }

        public void setEDate(String eDate) {
            this.eDate = eDate;
        }

        public Boolean getConsentWork() {
            return consentWork;
        }

        public void setConsentWork(Boolean consentWork) {
            this.consentWork = consentWork;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Recruit {

        private String recruitCode;
        private String msgDate;
        private String vacancyCode;
        private String codeIin;
        private String cvCode;
        private Dictionary status;
        private String msgText;

        public String getRecruitCode() {
            return recruitCode;
        }

        public void setRecruitCode(String recruitCode) {
            this.recruitCode = recruitCode;
        }

        public String getMsgDate() {
            return msgDate;
        }

        public void setMsgDate(String msgDate) {
            this.msgDate = msgDate;
        }

        public String getVacancyCode() {
            return vacancyCode;
        }

        public void setVacancyCode(String vacancyCode) {
            this.vacancyCode = vacancyCode;
        }

        public String getCodeIin() {
            return codeIin;
        }

        public void setCodeIin(String codeIin) {
            this.codeIin = codeIin;
        }

        public String getCvCode() {
            return cvCode;
        }

        public void setCvCode(String cvCode) {
            this.cvCode = cvCode;
        }

        public Dictionary getStatus() {
            return status;
        }

        public void setStatus(Dictionary status) {
            this.status = status;
        }

        public String getMsgText() {
            return msgText;
        }

        public void setMsgText(String msgText) {
            this.msgText = msgText;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Dictionary {

        private String code;
        private String name;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}