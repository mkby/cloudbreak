package com.sequenceiq.cloudbreak.controller;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v3.KerberosConfigV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.KerberosResponse;
import com.sequenceiq.cloudbreak.api.model.KerberosResponseView;
import com.sequenceiq.cloudbreak.api.model.kdc.KerberosRequest;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.service.kdc.KerberosService;
import com.sequenceiq.cloudbreak.util.WorkspaceEntityType;

@Controller
@Transactional(Transactional.TxType.NEVER)
@WorkspaceEntityType(KerberosConfig.class)
public class KerberosConfigV3Controller extends NotificationController implements KerberosConfigV3Endpoint {

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Inject
    private KerberosService kerberosService;

    @Override
    public Set<KerberosResponseView> listByWorkspace(Long workspaceId, String environment, Boolean attachGlobal) {
        return kerberosService.findAllInWorkspaceAndEnvironment(workspaceId, environment, attachGlobal).stream()
                .map(kdcConfig -> conversionService.convert(kdcConfig, KerberosResponseView.class))
                .collect(Collectors.toSet());
    }

    @Override
    public KerberosResponse getByNameInWorkspace(Long workspaceId, String name) {
        KerberosConfig kerberosConfig = kerberosService.getByNameForWorkspaceId(name, workspaceId);
        return conversionService.convert(kerberosConfig, KerberosResponse.class);
    }

    @Override
    public KerberosResponse createInWorkspace(Long workspaceId, @Valid KerberosRequest request) {
        KerberosConfig newKdcConfig = conversionService.convert(request, KerberosConfig.class);
        KerberosConfig createdKdcConfig = kerberosService.createInEnvironment(newKdcConfig, request.getEnvironments(), workspaceId);
        return conversionService.convert(createdKdcConfig, KerberosResponse.class);
    }

    @Override
    public KerberosResponse deleteInWorkspace(Long workspaceId, String name) {
        KerberosConfig deleted = kerberosService.deleteByNameFromWorkspace(name, workspaceId);
        return conversionService.convert(deleted, KerberosResponse.class);
    }

    @Override
    public KerberosResponse attachToEnvironments(Long workspaceId, String name, @NotEmpty Set<String> environmentNames) {
        KerberosConfig attached = kerberosService.attachToEnvironments(name, environmentNames, workspaceId);
        return conversionService.convert(attached, KerberosResponse.class);
    }

    @Override
    public KerberosResponse detachFromEnvironments(Long workspaceId, String name, @NotEmpty Set<String> environmentNames) {
        KerberosConfig detached = kerberosService.detachFromEnvironments(name, environmentNames, workspaceId);
        return conversionService.convert(detached, KerberosResponse.class);
    }

}