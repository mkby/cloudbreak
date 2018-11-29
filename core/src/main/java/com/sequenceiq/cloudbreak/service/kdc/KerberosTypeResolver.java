package com.sequenceiq.cloudbreak.service.kdc;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.kdc.KerberosRequest;
import com.sequenceiq.cloudbreak.api.model.kdc.KerberosRequestTypeBase;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.util.KerberosRequestValidator;

@Component
public class KerberosTypeResolver {

    @Inject
    private KerberosRequestValidator kerberosRequestValidator;

    public KerberosRequestTypeBase propagateKerberosConfiguration(KerberosRequest request) {
        if (!kerberosRequestValidator.isKerberosRequestProperlyCreated(request)) {
            throw new BadRequestException("Improper KerberosRequest!");
        }
        KerberosRequestTypeBase kdc;
        if (request.getCustom() != null) {
            kdc = request.getCustom();
        } else if (request.getFreeIpa() != null) {
            kdc = request.getFreeIpa();
        } else if (request.getActiveDirectory() != null) {
            kdc = request.getActiveDirectory();
        } else if (request.getMit() != null) {
            kdc = request.getMit();
        } else {
            throw new BadRequestException("Unable to determine KdcConfiguration since none of them are provided in the request!");
        }
        return kdc;
    }

}
