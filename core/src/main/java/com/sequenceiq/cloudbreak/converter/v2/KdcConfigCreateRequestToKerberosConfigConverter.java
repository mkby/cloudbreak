package com.sequenceiq.cloudbreak.converter.v2;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.kdc.KdcConfigCreateRequest;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.service.kdc.KdcResolver;

@Component
public class KdcConfigCreateRequestToKerberosConfigConverter extends AbstractConversionServiceAwareConverter<KdcConfigCreateRequest, KerberosConfig> {

    @Inject
    private KdcResolver kdcResolver;

    @Override
    public KerberosConfig convert(KdcConfigCreateRequest source) {
        return getConversionService().convert(kdcResolver.propagateKdcConfiguration(source), KerberosConfig.class);
    }

}
