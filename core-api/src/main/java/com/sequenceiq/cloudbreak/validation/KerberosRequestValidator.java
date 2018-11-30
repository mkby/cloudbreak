package com.sequenceiq.cloudbreak.validation;

import java.util.List;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.sequenceiq.cloudbreak.api.model.kdc.KerberosRequest;

public class KerberosRequestValidator implements ConstraintValidator<ValidKerberosRequest, KerberosRequest> {

    @Override
    public boolean isValid(KerberosRequest req, ConstraintValidatorContext constraintValidatorContext) {
        return List.of(req.getActiveDirectory() != null, req.getFreeIpa() != null, req.getMit() != null, req.getCustom() != null)
                .stream()
                .filter(fieldIsNotNull -> fieldIsNotNull)
                .count() == 1;
    }

}
