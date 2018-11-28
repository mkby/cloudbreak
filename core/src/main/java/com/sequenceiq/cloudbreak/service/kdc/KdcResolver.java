package com.sequenceiq.cloudbreak.service.kdc;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.kdc.KdcBase;
import com.sequenceiq.cloudbreak.api.model.kdc.KdcConfigCreateRequest;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.util.KdcConfigCreateValidator;

@Component
public class KdcResolver {

    @Inject
    private KdcConfigCreateValidator kdcConfigCreateValidator;

    public KdcBase propagateKdcConfiguration(KdcConfigCreateRequest request) {
        if (!kdcConfigCreateValidator.isKdcConfigCreateRequestProperlyCreated(request)) {
            throw new BadRequestException("Improper KdcConfigCreateRequest!");
        }
        KdcBase kdc;
        if (request.getCustom() != null) {
            kdc = request.getCustom();
        } else if (request.getExistingFreeIpa() != null) {
            kdc = request.getExistingFreeIpa();
        } else if (request.getExistingAd() != null) {
            kdc = request.getExistingAd();
        } else if (request.getExistingMit() != null) {
            kdc = request.getExistingMit();
        } else {
            throw new BadRequestException("Unable to determine KdcConfiguration since none of them are provided in the request!");
        }
        return kdc;
    }

}
