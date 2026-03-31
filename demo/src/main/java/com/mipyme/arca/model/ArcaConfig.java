package com.mipyme.arca.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "arca_config")
public class ArcaConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "cuit")
    private String cuit;

    @JsonIgnore
    @Column(name = "ta_ciphertext", columnDefinition = "TEXT")
    private String taCiphertext;

    @JsonIgnore
    @Column(name = "ta_iv", length = 24)
    private String taIv;

    @JsonIgnore
    @Column(name = "ta_key_version")
    private Integer taKeyVersion;

    @Column(name = "token_expiration")
    private LocalDateTime tokenExpiration;

    @Column(name = "current_step")
    private Integer currentStep = 1;

    @Column(name = "wizard_completed")
    private Boolean wizardCompleted = false;

    @Column(name = "environment")
    private String environment = "HOMOLOGATION";

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCuit() {
        return cuit;
    }

    public void setCuit(String cuit) {
        this.cuit = cuit;
    }

    public String getTaCiphertext() {
        return taCiphertext;
    }

    public void setTaCiphertext(String taCiphertext) {
        this.taCiphertext = taCiphertext;
    }

    public String getTaIv() {
        return taIv;
    }

    public void setTaIv(String taIv) {
        this.taIv = taIv;
    }

    public Integer getTaKeyVersion() {
        return taKeyVersion;
    }

    public void setTaKeyVersion(Integer taKeyVersion) {
        this.taKeyVersion = taKeyVersion;
    }

    public LocalDateTime getTokenExpiration() {
        return tokenExpiration;
    }

    public void setTokenExpiration(LocalDateTime tokenExpiration) {
        this.tokenExpiration = tokenExpiration;
    }

    public Integer getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(Integer currentStep) {
        this.currentStep = currentStep;
    }

    public Boolean getWizardCompleted() {
        return wizardCompleted;
    }

    public void setWizardCompleted(Boolean wizardCompleted) {
        this.wizardCompleted = wizardCompleted;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public boolean isComplete() {
        return Boolean.TRUE.equals(wizardCompleted);
    }
}
