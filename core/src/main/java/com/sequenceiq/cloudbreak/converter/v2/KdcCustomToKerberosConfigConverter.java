package com.sequenceiq.cloudbreak.converter.v2;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.kdc.KdcCustom;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;

@Component
public class KdcCustomToKerberosConfigConverter extends AbstractConversionServiceAwareConverter<KdcCustom, KerberosConfig> {

    @Override
    public KerberosConfig convert(KdcCustom source) {
        KerberosConfig config = new KerberosConfig();
        config.setDescriptor(source.getDescriptor());
        config.setKrb5Conf(source.getKrb5Conf());
        config.setPrincipal(source.getPrincipal());
        config.setName(source.getName());
        config.setType(source.getType());
        config.setDomain(source.getDomain());
        config.setNameServers(source.getNameServers());
        config.setPassword(source.getPassword());
        config.setVerifyKdcTrust(source.getVerifyKdcTrust());
        config.setTcpAllowed(source.getTcpAllowed());
        return config;
    }

}
