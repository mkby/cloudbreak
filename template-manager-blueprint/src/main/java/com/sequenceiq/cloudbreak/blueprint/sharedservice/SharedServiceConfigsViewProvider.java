package com.sequenceiq.cloudbreak.blueprint.sharedservice;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.blueprint.BlueprintProcessorFactory;
import com.sequenceiq.cloudbreak.blueprint.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.template.BlueprintProcessingException;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.template.views.SharedServiceConfigsView;

@Component
public class SharedServiceConfigsViewProvider {

    private static final String DEFAULT_RANGER_PORT = "6080";

    @Inject
    private BlueprintProcessorFactory blueprintProcessorFactory;

    @Inject
    private BlueprintUtils blueprintUtils;

    public SharedServiceConfigsView createSharedServiceConfigs(Blueprint blueprint, String ambariPassword, Stack dataLakeStack) {
        SharedServiceConfigsView sharedServiceConfigsView = new SharedServiceConfigsView();
        if (dataLakeStack != null) {
            String blueprintText = dataLakeStack.getCluster().getBlueprint().getBlueprintText();
            String rangerPort = getRangerPort(dataLakeStack, blueprintText);
            sharedServiceConfigsView.setRangerAdminPassword(dataLakeStack.getCluster().getPassword());
            sharedServiceConfigsView.setAttachedCluster(true);
            sharedServiceConfigsView.setDatalakeCluster(false);
            sharedServiceConfigsView.setDatalakeAmbariIp(dataLakeStack.getAmbariIp());
            sharedServiceConfigsView.setDatalakeAmbariFqdn(dataLakeStack.getGatewayInstanceMetadata().isEmpty()
                    ? dataLakeStack.getAmbariIp() : dataLakeStack.getGatewayInstanceMetadata().iterator().next().getDiscoveryFQDN());
            sharedServiceConfigsView.setDatalakeComponents(prepareComponents(blueprintText));
            sharedServiceConfigsView.setRangerAdminPort(rangerPort);
            sharedServiceConfigsView.setRangerAdminHost(dataLakeStack.getPrimaryGatewayInstance().getDiscoveryFQDN());
        } else if (blueprintUtils.isSharedServiceReadyBlueprint(blueprint)) {
            sharedServiceConfigsView.setRangerAdminPassword(ambariPassword);
            sharedServiceConfigsView.setAttachedCluster(false);
            sharedServiceConfigsView.setDatalakeCluster(true);
            sharedServiceConfigsView.setRangerAdminPort(DEFAULT_RANGER_PORT);
        } else {
            sharedServiceConfigsView.setRangerAdminPassword(ambariPassword);
            sharedServiceConfigsView.setAttachedCluster(false);
            sharedServiceConfigsView.setDatalakeCluster(false);
            sharedServiceConfigsView.setRangerAdminPort(DEFAULT_RANGER_PORT);
        }

        return sharedServiceConfigsView;
    }

    private String getRangerPort(Stack dataLakeStack, String blueprintText) {
        BlueprintTextProcessor blueprintTextProcessor = blueprintProcessorFactory.get(blueprintText);
        Map<String, Map<String, String>> configurationEntries = blueprintTextProcessor.getConfigurationEntries();
        Map<String, String> rangerAdminConfigs = configurationEntries.getOrDefault("ranger-admin-site", new HashMap<>());
        return rangerAdminConfigs.getOrDefault("ranger.service.http.port", getDatalakeRangerPort(dataLakeStack));
    }

    public SharedServiceConfigsView createSharedServiceConfigs(Stack source, Stack dataLakeStack) {
        Cluster cluster = source.getCluster();
        return createSharedServiceConfigs(cluster.getBlueprint(), cluster.getPassword(), dataLakeStack);
    }

    private String getDatalakeRangerPort(Stack datalake) {
        Cluster dataLakeCluster = datalake.getCluster();
        String blueprintText = dataLakeCluster.getBlueprint().getBlueprintText();
        BlueprintTextProcessor blueprintTextProcessor = blueprintProcessorFactory.get(blueprintText);
        Map<String, String> rangerAdminConfigs = blueprintTextProcessor.getConfigurationEntries().getOrDefault("ranger-admin-site", new HashMap<>());
        return rangerAdminConfigs.getOrDefault("ranger.service.http.port", DEFAULT_RANGER_PORT);
    }

    private Set<String> prepareComponents(String blueprintText) {
        Set<String> result = new HashSet<>();
        try {
            BlueprintTextProcessor blueprintTextProcessor = new BlueprintTextProcessor(blueprintText);
            Map<String, Set<String>> componentsByHostGroup = blueprintTextProcessor.getComponentsByHostGroup();
            componentsByHostGroup.values().forEach(result::addAll);
        } catch (BlueprintProcessingException exception) {
            result = new HashSet<>();
        }
        return result;
    }
}
