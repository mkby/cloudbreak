package com.sequenceiq.cloudbreak.validation;

import java.util.List;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.sequenceiq.cloudbreak.api.model.kdc.KdcConfigCreateRequest;

public class KdcConfigCreateRequestValidator implements ConstraintValidator<ValidKdcConfigCreateRequest, KdcConfigCreateRequest> {

    @Override
    public boolean isValid(KdcConfigCreateRequest req, ConstraintValidatorContext constraintValidatorContext) {
        return List.of(req.getExistingAd() != null, req.getExistingFreeIpa() != null, req.getExistingMit() != null, req.getCustom() != null)
                .stream()
                .filter(fieldIsNotNull -> fieldIsNotNull)
                .count() == 1;
    }

}
