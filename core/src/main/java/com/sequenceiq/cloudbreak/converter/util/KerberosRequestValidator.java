package com.sequenceiq.cloudbreak.converter.util;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.kdc.KerberosRequest;

@Component
public class KerberosRequestValidator {

    public boolean isKerberosRequestProperlyCreated(KerberosRequest req) {
        boolean result;
        if (req == null) {
            result = false;
        } else {
            result = List.of(req.getActiveDirectory() != null, req.getFreeIpa() != null, req.getMit() != null, req.getCustom() != null)
                    .stream()
                    .filter(fieldIsNotNull -> fieldIsNotNull)
                    .count() == 1;
        }
        return result;
    }

}
