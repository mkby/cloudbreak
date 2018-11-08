package com.sequenceiq.cloudbreak.cloud.aws.resourceconnector;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.context.AwsContextBuilder;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsNetworkView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.transform.CloudResourceHelper;

@Service
public class AwsUpscaleService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsUpscaleService.class);

    private static final List<String> UPSCALE_PROCESSES = asList("Launch");

    @Inject
    private AwsNetworkService awsNetworkService;

    @Inject
    private AwsClient awsClient;

    @Inject
    private CloudFormationStackUtil cfStackUtil;

    @Inject
    private CloudResourceHelper cloudResourceHelper;

    @Inject
    private AwsAutoScalingService awsAutoScalingService;

    @Inject
    private AwsContextBuilder contextBuilder;

    @Inject
    private AwsComputeResourceService awsComputeResourceService;

    public List<CloudResourceStatus> upscale(AuthenticatedContext ac, CloudStack stack, List<CloudResource> resources) {
        AmazonCloudFormationRetryClient cloudFormationClient = awsClient.createCloudFormationRetryClient(new AwsCredentialView(ac.getCloudCredential()),
                ac.getCloudContext().getLocation().getRegion().value());
        AmazonAutoScalingRetryClient amazonASClient = awsClient.createAutoScalingRetryClient(new AwsCredentialView(ac.getCloudCredential()),
                ac.getCloudContext().getLocation().getRegion().value());
        AmazonEC2Client amazonEC2Client = awsClient.createAccess(new AwsCredentialView(ac.getCloudCredential()),
                ac.getCloudContext().getLocation().getRegion().value());

        List<Group> scaledGroups = cloudResourceHelper.getScaledGroups(stack);
        Map<String, Group> groupMap = scaledGroups.stream().collect(
                Collectors.toMap(g -> cfStackUtil.getAutoscalingGroupName(ac, cloudFormationClient, g.getName()), g -> g));
        awsAutoScalingService.resumeAutoScaling(amazonASClient, groupMap.keySet(), UPSCALE_PROCESSES);
        for (Map.Entry<String, Group> groupEntry : groupMap.entrySet()) {
            Group group = groupEntry.getValue();
            amazonASClient.updateAutoScalingGroup(new UpdateAutoScalingGroupRequest()
                    .withAutoScalingGroupName(groupEntry.getKey())
                    .withMaxSize(group.getInstancesSize())
                    .withDesiredCapacity(group.getInstancesSize()));
            LOGGER.info("Updated Auto Scaling group's desiredCapacity: [stack: '{}', to: '{}']", ac.getCloudContext().getId(),
                    group.getInstancesSize());
        }
        awsAutoScalingService.scheduleStatusChecks(stack, ac, cloudFormationClient);
        awsAutoScalingService.suspendAutoScaling(ac, stack);

        List<CloudResource> instances =
                cfStackUtil.getInstanceCloudResources(ac, cloudFormationClient, amazonASClient, scaledGroups);

        boolean mapPublicIpOnLaunch = awsNetworkService.isMapPublicOnLaunch(new AwsNetworkView(stack.getNetwork()), amazonEC2Client);
        List<Group> gateways = awsNetworkService.getGatewayGroups(scaledGroups);
        Map<String, List<String>> gatewayGroupInstanceMapping = instances.stream()
                .filter(instance -> gateways.stream().anyMatch(gw -> gw.getName().equals(instance.getGroup())))
                .filter(instance -> {
                    Group gateway = gateways.stream().filter(gw -> gw.getName().equals(instance.getGroup())).findFirst().get();
                    return gateway.getInstances().stream().noneMatch(inst -> instance.getInstanceId().equals(inst.getInstanceId()));
                })
                .collect(Collectors.toMap(
                        CloudResource::getGroup,
                        instance -> List.of(instance.getInstanceId()),
                        (listOne, listTwo) -> Stream.concat(listOne.stream(), listTwo.stream()).collect(Collectors.toList())));
        if (mapPublicIpOnLaunch && !gateways.isEmpty()) {
            String cFStackName = cfStackUtil.getCloudFormationStackResource(resources).getName();
            Map<String, String> eipAllocationIds = awsNetworkService.getElasticIpAllocationIds(cfStackUtil.getOutputs(cFStackName, cloudFormationClient), cFStackName);
            for (Group gateway : gateways) {
                List<String> eips = awsNetworkService.getEipsForGatewayGroup(eipAllocationIds, gateway);
                List<String> freeEips = awsNetworkService.getFreeIps(eips, amazonEC2Client);
                List<String> newInstances = gatewayGroupInstanceMapping.get(gateway.getName());
                awsNetworkService.associateElasticIpsToInstances(amazonEC2Client, freeEips, newInstances);
            }
        }

        awsComputeResourceService.buildComputeResourcesForUpscale(ac, stack, scaledGroups, instances);

        return singletonList(new CloudResourceStatus(cfStackUtil.getCloudFormationStackResource(resources), ResourceStatus.UPDATED));
    }
}
