package com.sequenceiq.cloudbreak.api.model.kdc;

import java.util.HashSet;
import java.util.Set;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.validation.ValidKdcConfigCreateRequest;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ValidKdcConfigCreateRequest
public class KdcConfigCreateRequest implements JsonEntity {

    @Valid
    @ApiModelProperty
    private KdcExistingAd existingAd;

    @Valid
    @ApiModelProperty
    private KdcExistingFreeIpa existingFreeIpa;

    @Valid
    @ApiModelProperty
    private KdcExistingMit existingMit;

    @Valid
    @ApiModelProperty
    private KdcCustom custom;

    @ApiModelProperty(ModelDescriptions.ENVIRONMENTS)
    private Set<String> environments = new HashSet<>();

    public Set<String> getEnvironments() {
        return environments;
    }

    public void setEnvironments(Set<String> environments) {
        this.environments = environments;
    }

    public KdcExistingAd getExistingAd() {
        return existingAd;
    }

    public void setExistingAd(KdcExistingAd existingAd) {
        this.existingAd = existingAd;
    }

    public KdcExistingFreeIpa getExistingFreeIpa() {
        return existingFreeIpa;
    }

    public void setExistingFreeIpa(KdcExistingFreeIpa existingFreeIpa) {
        this.existingFreeIpa = existingFreeIpa;
    }

    public KdcExistingMit getExistingMit() {
        return existingMit;
    }

    public void setExistingMit(KdcExistingMit existingMit) {
        this.existingMit = existingMit;
    }

    public KdcCustom getCustom() {
        return custom;
    }

    public void setCustom(KdcCustom custom) {
        this.custom = custom;
    }
}
