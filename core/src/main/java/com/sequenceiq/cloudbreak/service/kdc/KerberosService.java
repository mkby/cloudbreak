package com.sequenceiq.cloudbreak.service.kdc;

import static java.lang.String.format;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.repository.KerberosConfigRepository;
import com.sequenceiq.cloudbreak.repository.environment.EnvironmentResourceRepository;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.environment.AbstractEnvironmentAwareService;

@Service
public class KerberosService extends AbstractEnvironmentAwareService<KerberosConfig> {

    private static final Logger LOGGER = LoggerFactory.getLogger(KerberosService.class);

    @Inject
    private KerberosConfigRepository kerberosConfigRepository;

    @Inject
    private ClusterService clusterService;

    @Override
    protected EnvironmentResourceRepository<KerberosConfig, Long> repository() {
        return kerberosConfigRepository;
    }

    @Override
    protected void prepareCreation(KerberosConfig resource) {
    }

    @Override
    public KerberosConfig createInEnvironment(KerberosConfig resource, Set<String> environments, @NotNull Long workspaceId) {
        Optional.ofNullable(repository().findByNameAndWorkspaceId(resource.getName(), workspaceId))
                .ifPresent(kerberosConfig -> {
                    LOGGER.info(format("KerberosConfig – in the given workspace – with name [%s] is already exists", resource.getName()));
                    throw new AccessDeniedException("Access denied");
                });
        return super.createInEnvironment(resource, environments, workspaceId);
    }

    @Override
    public Set<Cluster> getClustersUsingResource(KerberosConfig resource) {
        return clusterService.findByKdcConfig(resource.getId());
    }

    @Override
    public Set<Cluster> getClustersUsingResourceInEnvironment(KerberosConfig resource, Long environmentId) {
        return clusterService.findAllClustersByKerberosConfigInEnvironment(resource, environmentId);
    }

    @Override
    public WorkspaceResource resource() {
        return WorkspaceResource.KERBEROS_CONFIG;
    }

}
