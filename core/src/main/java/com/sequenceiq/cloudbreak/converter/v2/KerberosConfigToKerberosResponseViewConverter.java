package com.sequenceiq.cloudbreak.converter.v2;

import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.KerberosResponseView;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.view.CompactView;

@Component
public class KerberosConfigToKerberosResponseViewConverter extends AbstractConversionServiceAwareConverter<KerberosConfig, KerberosResponseView> {

    @Override
    public KerberosResponseView convert(KerberosConfig source) {
        KerberosResponseView view = new KerberosResponseView();
        view.setName(source.getName());
        view.setType(source.getType());
        view.setEnvironments(source.getEnvironments().stream().map(CompactView::getName).collect(Collectors.toSet()));
        return view;
    }

}
