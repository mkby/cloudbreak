package com.sequenceiq.cloudbreak.converter.v2;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.kdc.KerberosRequest;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.service.kdc.KerberosTypeResolver;

@Component
public class KerberosRequestToKerberosConfigConverter extends AbstractConversionServiceAwareConverter<KerberosRequest, KerberosConfig> {

    @Inject
    private KerberosTypeResolver kerberosTypeResolver;

    @Override
    public KerberosConfig convert(KerberosRequest source) {
        KerberosConfig kerberosConfig = getConversionService().convert(kerberosTypeResolver.propagateKerberosConfiguration(source), KerberosConfig.class);
        kerberosConfig.setDescription(source.getDescription());
        return kerberosConfig;
    }

}
