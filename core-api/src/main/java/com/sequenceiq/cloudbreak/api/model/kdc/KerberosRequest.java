package com.sequenceiq.cloudbreak.api.model.kdc;

import java.util.HashSet;
import java.util.Set;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.validation.ValidKerberosRequest;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ValidKerberosRequest
public class KerberosRequest implements JsonEntity {

    @Valid
    @ApiModelProperty
    private KerberosAdRequest activeDirectory;

    @Valid
    @ApiModelProperty
    private KerberosFreeIpaRequest freeIpa;

    @Valid
    @ApiModelProperty
    private KerberosMitRequest mit;

    @Valid
    @ApiModelProperty
    private KerberosCustomRequest custom;

    @ApiModelProperty(ModelDescriptions.ENVIRONMENTS)
    private Set<String> environments = new HashSet<>();

    public Set<String> getEnvironments() {
        return environments;
    }

    public void setEnvironments(Set<String> environments) {
        this.environments = environments;
    }

    public KerberosAdRequest getActiveDirectory() {
        return activeDirectory;
    }

    public void setActiveDirectory(KerberosAdRequest activeDirectory) {
        this.activeDirectory = activeDirectory;
    }

    public KerberosFreeIpaRequest getFreeIpa() {
        return freeIpa;
    }

    public void setFreeIpa(KerberosFreeIpaRequest freeIpa) {
        this.freeIpa = freeIpa;
    }

    public KerberosMitRequest getMit() {
        return mit;
    }

    public void setMit(KerberosMitRequest mit) {
        this.mit = mit;
    }

    public KerberosCustomRequest getCustom() {
        return custom;
    }

    public void setCustom(KerberosCustomRequest custom) {
        this.custom = custom;
    }
}
