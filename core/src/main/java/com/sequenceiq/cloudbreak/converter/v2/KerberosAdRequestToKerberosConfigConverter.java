package com.sequenceiq.cloudbreak.converter.v2;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.kdc.KerberosAdRequest;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;

@Component
public class KerberosAdRequestToKerberosConfigConverter extends AbstractConversionServiceAwareConverter<KerberosAdRequest, KerberosConfig> {

    @Override
    public KerberosConfig convert(KerberosAdRequest source) {
        KerberosConfig config = new KerberosConfig();
        config.setAdminUrl(source.getAdminUrl());
        config.setContainerDn(source.getContainerDn());
        config.setLdapUrl(source.getLdapUrl());
        config.setRealm(source.getRealm());
        config.setUrl(source.getUrl());
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
