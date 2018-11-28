package com.sequenceiq.cloudbreak.converter.util;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.kdc.KdcConfigCreateRequest;

@Component
public class KdcConfigCreateValidator {

    public boolean isKdcConfigCreateRequestProperlyCreated(KdcConfigCreateRequest req) {
        boolean result;
        if (req == null) {
            result = false;
        } else {
            result = List.of(req.getExistingAd() != null, req.getExistingFreeIpa() != null, req.getExistingMit() != null, req.getCustom() != null)
                    .stream()
                    .filter(fieldIsNotNull -> fieldIsNotNull)
                    .count() == 1;
        }
        return result;
    }

}
