package com.sequenceiq.cloudbreak.converter.v2;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.kdc.KdcCbManaged;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;

@Component
public class KdcCbManagedToKerberosConfigConverter extends AbstractConversionServiceAwareConverter<KdcCbManaged, KerberosConfig> {

    @Override
    public KerberosConfig convert(KdcCbManaged source) {
        KerberosConfig config = new KerberosConfig();
        config.setName(source.getName());
        config.setAdmin(source.getAdmin());
        config.setType(source.getType());
        config.setDomain(source.getDomain());
        config.setNameServers(source.getNameServers());
        config.setPassword(source.getPassword());
        config.setVerifyKdcTrust(source.getVerifyKdcTrust());
        config.setTcpAllowed(source.getTcpAllowed());
        return config;
    }

}
