package com.sequenceiq.cloudbreak.cloud.aws;

import static com.amazonaws.services.cloudformation.model.StackStatus.CREATE_FAILED;
import static com.amazonaws.services.cloudformation.model.StackStatus.DELETE_COMPLETE;
import static com.amazonaws.services.cloudformation.model.StackStatus.DELETE_FAILED;
import static com.amazonaws.services.cloudformation.model.StackStatus.ROLLBACK_COMPLETE;
import static com.amazonaws.services.cloudformation.model.StackStatus.ROLLBACK_FAILED;
import static com.amazonaws.services.cloudformation.model.StackStatus.ROLLBACK_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.cloud.aws.AwsInstanceConnector.INSTANCE_NOT_FOUND_ERROR_CODE;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DeleteLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DetachInstancesRequest;
import com.amazonaws.services.autoscaling.model.ResumeProcessesRequest;
import com.amazonaws.services.autoscaling.model.SuspendProcessesRequest;
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.StackStatus;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DeleteKeyPairRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.sequenceiq.cloudbreak.api.model.AdjustmentType;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.context.AwsContextBuilder;
import com.sequenceiq.cloudbreak.cloud.aws.encryption.EncryptedImageCopyService;
import com.sequenceiq.cloudbreak.cloud.aws.encryption.EncryptedSnapshotService;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.AwsBackoffSyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.aws.task.AwsPollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsNetworkView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.TlsInfo;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;
import com.sequenceiq.cloudbreak.cloud.template.compute.ComputeResourceService;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.transform.CloudResourceHelper;
import com.sequenceiq.cloudbreak.common.type.CommonResourceType;
import com.sequenceiq.cloudbreak.common.type.ResourceType;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.cloudbreak.service.Retry.ActionWentFailException;

import freemarker.template.Configuration;

@Service
public class AwsResourceConnector implements ResourceConnector<Object> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AwsResourceConnector.class);

    private static final List<String> CAPABILITY_IAM = singletonList("CAPABILITY_IAM");

    private static final List<String> UPSCALE_PROCESSES = asList("Launch");

    private static final List<String> SUSPENDED_PROCESSES = asList("Launch", "HealthCheck", "ReplaceUnhealthy", "AZRebalance", "AlarmNotification",
            "ScheduledActions", "AddToLoadBalancer", "RemoveFromLoadBalancerLowPriority");

    private static final List<StackStatus> ERROR_STATUSES = asList(CREATE_FAILED, ROLLBACK_IN_PROGRESS, ROLLBACK_FAILED, ROLLBACK_COMPLETE, DELETE_FAILED);

    private static final String CFS_OUTPUT_EIPALLOCATION_ID = "EIPAllocationID";

    private static final String S3_ACCESS_ROLE = "S3AccessRole";

    private static final String CREATED_VPC = "CreatedVpc";

    private static final String CREATED_SUBNET = "CreatedSubnet";

    @Inject
    private AwsNetworkService awsNetworkService;

    @Inject
    private Configuration freemarkerConfiguration;

    @Inject
    private AwsClient awsClient;

    @Inject
    private CloudFormationStackUtil cfStackUtil;

    @Inject
    private AwsBackoffSyncPollingScheduler<Boolean> awsBackoffSyncPollingScheduler;

    @Inject
    private CloudFormationTemplateBuilder cloudFormationTemplateBuilder;

    @Inject
    private AwsPollTaskFactory awsPollTaskFactory;

    @Inject
    private CloudFormationStackUtil cloudFormationStackUtil;

    @Inject
    private AwsTagPreparationService awsTagPreparationService;

    @Inject
    private EncryptedSnapshotService encryptedSnapshotService;

    @Inject
    private EncryptedImageCopyService encryptedImageCopyService;

    @Value("${cb.aws.vpc:}")
    private String cloudbreakVpc;

    @Value("${cb.aws.cf.template.new.path:}")
    private String awsCloudformationTemplatePath;

    @Inject
    @Qualifier("DefaultRetryService")
    private Retry retryService;

    @Inject
    private AwsImageUpdateService awsImageUpdateService;

    @Inject
    private CloudResourceHelper cloudResourceHelper;

    @Inject
    private AwsContextBuilder contextBuilder;

    @Inject
    private ComputeResourceService computeResourceService;

    @Inject
    private AwsResourceLaunchService awsResourceLaunchService;

    @Override
    public List<CloudResourceStatus> launch(AuthenticatedContext ac, CloudStack stack, PersistenceNotifier resourceNotifier,
            AdjustmentType adjustmentType, Long threshold) throws Exception {
        return awsResourceLaunchService.launch(ac, stack, resourceNotifier, adjustmentType, threshold);
    }

    private void addInstancesToContext(List<CloudResource> instances, ResourceBuilderContext context, List<Group> groups) {
        groups.forEach(group -> {
            List<Long> ids = group.getInstances().stream()
                    .filter(instance -> Objects.isNull(instance.getInstanceId()))
                    .map(CloudInstance::getTemplate).map(InstanceTemplate::getPrivateId).collect(Collectors.toList());
            List<CloudResource> groupInstances = instances.stream().filter(inst -> inst.getGroup().equals(group.getName())).collect(Collectors.toList());
            for (int i = 0; i < ids.size(); i++) {
                context.addComputeResources(ids.get(i), List.of(groupInstances.get(i)));
            }
        });
    }

    private List<CloudResourceStatus> buildComputeResourcesForUpscale(AuthenticatedContext ac, CloudStack stack, List<Group> scaledGroups,
            List<CloudResource> instances) {
        CloudContext cloudContext = ac.getCloudContext();
        ResourceBuilderContext context = contextBuilder.contextInit(cloudContext, ac, stack.getNetwork(), null, true);

        List<Group> groupsWithNewInstances = scaledGroups.stream().map(group -> {
            List<CloudInstance> newInstances = group.getInstances().stream()
                    .filter(instance -> Objects.isNull(instance.getInstanceId())).collect(Collectors.toList());

            return new Group(group.getName(), group.getType(), newInstances, group.getSecurity(), null, group.getParameters(),
                    group.getInstanceAuthentication(), group.getLoginUserName(), group.getPublicKey(), group.getRootVolumeSize());
        }).collect(Collectors.toList());

        List<CloudResource> newInstances = instances.stream().filter(instance -> {
            Group group = scaledGroups.stream().filter(scaledGroup -> scaledGroup.getName().equals(instance.getGroup())).findFirst().get();
            return group.getInstances().stream().noneMatch(inst -> instance.getInstanceId().equals(inst.getInstanceId()));
        }).collect(Collectors.toList());

        addInstancesToContext(newInstances, context, groupsWithNewInstances);
        return computeResourceService.buildResourcesForUpscale(context, ac, stack, groupsWithNewInstances);
    }

    private List<CloudResourceStatus> deleteComputeResources(AuthenticatedContext ac, CloudStack stack, List<CloudResource> cloudResources) {
        CloudContext cloudContext = ac.getCloudContext();
        ResourceBuilderContext context = contextBuilder.contextInit(cloudContext, ac, stack.getNetwork(), null, true);

        return computeResourceService.deleteResources(context, ac, cloudResources, false);
    }



    private boolean deployingToSameVPC(AwsNetworkView awsNetworkView, boolean existingVPC) {
        return StringUtils.isNoneEmpty(cloudbreakVpc) && existingVPC && awsNetworkView.getExistingVPC().equals(cloudbreakVpc);
    }


    private String getCreatedSubnet(String cFStackName, AmazonCloudFormationRetryClient client) {
        Map<String, String> outputs = cfStackUtil.getOutputs(cFStackName, client);
        if (outputs.containsKey(CREATED_SUBNET)) {
            return outputs.get(CREATED_SUBNET);
        } else {
            String outputKeyNotFound = String.format("Subnet could not be found in the Cloudformation stack('%s') output.", cFStackName);
            throw new CloudConnectorException(outputKeyNotFound);
        }
    }

    private String getCreatedS3AccessRoleArn(String cFStackName, AmazonCloudFormationRetryClient client) {
        Map<String, String> outputs = cfStackUtil.getOutputs(cFStackName, client);
        if (outputs.containsKey(S3_ACCESS_ROLE)) {
            return outputs.get(S3_ACCESS_ROLE);
        } else {
            String outputKeyNotFound = String.format("S3AccessRole arn could not be found in the Cloudformation stack('%s') output.", cFStackName);
            throw new CloudConnectorException(outputKeyNotFound);
        }
    }

    private Supplier<CloudConnectorException> getCloudConnectorExceptionSupplier(String msg) {
        return () -> new CloudConnectorException(msg);
    }

    private void suspendAutoScaling(AuthenticatedContext ac, CloudStack stack) {
        AmazonAutoScalingRetryClient amazonASClient = awsClient.createAutoScalingRetryClient(new AwsCredentialView(ac.getCloudCredential()),
                ac.getCloudContext().getLocation().getRegion().value());
        for (Group group : stack.getGroups()) {
            String asGroupName = cfStackUtil.getAutoscalingGroupName(ac, group.getName(), ac.getCloudContext().getLocation().getRegion().value());
            amazonASClient.suspendProcesses(new SuspendProcessesRequest().withAutoScalingGroupName(asGroupName).withScalingProcesses(SUSPENDED_PROCESSES));
        }
    }

    private void resumeAutoScaling(AmazonAutoScalingRetryClient amazonASClient, Collection<String> groupNames, List<String> autoScalingPolicies) {
        for (String groupName : groupNames) {
            amazonASClient.resumeProcesses(new ResumeProcessesRequest().withAutoScalingGroupName(groupName).withScalingProcesses(autoScalingPolicies));
        }
    }

    @Override
    public List<CloudResourceStatus> check(AuthenticatedContext authenticatedContext, List<CloudResource> resources) {
        return new ArrayList<>();
    }

    @Override
    public List<CloudResourceStatus> terminate(AuthenticatedContext ac, CloudStack stack, List<CloudResource> resources) {
        LOGGER.info("Deleting stack: {}", ac.getCloudContext().getId());
        AwsCredentialView credentialView = new AwsCredentialView(ac.getCloudCredential());
        String regionName = ac.getCloudContext().getLocation().getRegion().value();
        if (resources != null && !resources.isEmpty()) {
            deleteComputeResources(ac, stack, resources);

            AmazonCloudFormationRetryClient cfRetryClient = awsClient.createCloudFormationRetryClient(credentialView, regionName);
            CloudResource stackResource = cfStackUtil.getCloudFormationStackResource(resources);
            AmazonEC2Client amazonEC2Client = awsClient.createAccess(credentialView, regionName);
            if (stackResource == null) {
                cleanupEncryptedResources(ac, resources, regionName, amazonEC2Client);
                return Collections.emptyList();
            }
            String cFStackName = stackResource.getName();
            LOGGER.info("Deleting CloudFormation stack for stack: {} [cf stack id: {}]", cFStackName, ac.getCloudContext().getId());
            DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest().withStackName(cFStackName);
            try {
                retryService.testWith2SecDelayMax15Times(() -> {
                    try {
                        cfRetryClient.describeStacks(describeStacksRequest);
                    } catch (AmazonServiceException e) {
                        if (!e.getErrorMessage().contains(cFStackName + " does not exist")) {
                            throw e;
                        }
                        throw new ActionWentFailException("Stack not exists");
                    }
                    return Boolean.TRUE;
                });
            } catch (ActionWentFailException ignored) {
                LOGGER.info("Stack not found with name: {}", cFStackName);
                awsNetworkService.releaseReservedIp(amazonEC2Client, resources);
                cleanupEncryptedResources(ac, resources, regionName, amazonEC2Client);
                return Collections.emptyList();
            }
            resumeAutoScalingPolicies(ac, stack);
            DeleteStackRequest deleteStackRequest = new DeleteStackRequest().withStackName(cFStackName);
            cfRetryClient.deleteStack(deleteStackRequest);

            AmazonCloudFormationClient cfClient = awsClient.createCloudFormationClient(credentialView, regionName);
            PollTask<Boolean> task = awsPollTaskFactory.newAwsTerminateStackStatusCheckerTask(ac, cfClient, DELETE_COMPLETE, DELETE_FAILED, ERROR_STATUSES,
                    cFStackName);
            try {
                awsBackoffSyncPollingScheduler.schedule(task);
            } catch (Exception e) {
                throw new CloudConnectorException(e.getMessage(), e);
            }
            awsNetworkService.releaseReservedIp(amazonEC2Client, resources);
            cleanupEncryptedResources(ac, resources, regionName, amazonEC2Client);
            deleteKeyPair(ac, stack);
            deleteLaunchConfiguration(resources, ac);
        } else if (resources != null) {
            AmazonEC2Client amazonEC2Client = awsClient.createAccess(credentialView, regionName);
            awsNetworkService.releaseReservedIp(amazonEC2Client, resources);
            LOGGER.info("No CloudFormation stack saved for stack.");
        } else {
            LOGGER.info("No resources to release.");
        }
        return check(ac, resources);
    }

    private void cleanupEncryptedResources(AuthenticatedContext ac, List<CloudResource> resources, String regionName, AmazonEC2Client amazonEC2Client) {
        encryptedSnapshotService.deleteResources(ac, amazonEC2Client, resources);
        encryptedImageCopyService.deleteResources(regionName, amazonEC2Client, resources);
    }

    private void deleteLaunchConfiguration(List<CloudResource> resources, AuthenticatedContext ac) {
        AmazonAutoScalingClient autoScalingClient = awsClient.createAutoScalingClient(new AwsCredentialView(ac.getCloudCredential()),
                ac.getCloudContext().getLocation().getRegion().value());
        resources.stream().filter(cloudResource -> cloudResource.getType() == ResourceType.AWS_LAUNCHCONFIGURATION).forEach(cloudResource ->
                autoScalingClient.deleteLaunchConfiguration(
                        new DeleteLaunchConfigurationRequest().withLaunchConfigurationName(cloudResource.getName())));
    }

    private void deleteKeyPair(AuthenticatedContext ac, CloudStack stack) {
        AwsCredentialView awsCredential = new AwsCredentialView(ac.getCloudCredential());
        String region = ac.getCloudContext().getLocation().getRegion().value();
        if (!awsClient.existingKeyPairNameSpecified(stack.getInstanceAuthentication())) {
            try {
                AmazonEC2Client client = awsClient.createAccess(awsCredential, region);
                DeleteKeyPairRequest deleteKeyPairRequest = new DeleteKeyPairRequest(awsClient.getKeyPairName(ac));
                client.deleteKeyPair(deleteKeyPairRequest);
            } catch (Exception e) {
                String errorMessage = String.format("Failed to delete public key [roleArn:'%s', region: '%s'], detailed message: %s",
                        awsCredential.getRoleArn(), region, e.getMessage());
                LOGGER.warn(errorMessage, e);
            }
        }
    }

    private void resumeAutoScalingPolicies(AuthenticatedContext ac, CloudStack stack) {
        for (Group instanceGroup : stack.getGroups()) {
            try {
                String asGroupName = cfStackUtil.getAutoscalingGroupName(ac, instanceGroup.getName(), ac.getCloudContext().getLocation().getRegion().value());
                if (asGroupName != null) {
                    AmazonAutoScalingRetryClient amazonASClient = awsClient.createAutoScalingRetryClient(new AwsCredentialView(ac.getCloudCredential()),
                            ac.getCloudContext().getLocation().getRegion().value());
                    List<AutoScalingGroup> asGroups = amazonASClient.describeAutoScalingGroups(new DescribeAutoScalingGroupsRequest()
                            .withAutoScalingGroupNames(asGroupName)).getAutoScalingGroups();
                    if (!asGroups.isEmpty()) {
                        if (!asGroups.get(0).getSuspendedProcesses().isEmpty()) {
                            amazonASClient.updateAutoScalingGroup(new UpdateAutoScalingGroupRequest()
                                    .withAutoScalingGroupName(asGroupName)
                                    .withMinSize(0)
                                    .withDesiredCapacity(0));
                            amazonASClient.resumeProcesses(new ResumeProcessesRequest().withAutoScalingGroupName(asGroupName));
                        }
                    }
                } else {
                    LOGGER.info("Autoscaling Group's physical id is null (the resource doesn't exist), it is not needed to resume scaling policies.");
                }
            } catch (AmazonServiceException e) {
                if (e.getErrorMessage().matches("Resource.*does not exist for stack.*") || e.getErrorMessage().matches("Stack '.*' does not exist.*")) {
                    LOGGER.info(e.getMessage());
                } else {
                    throw e;
                }
            }
        }
    }

    @Override
    public List<CloudResourceStatus> update(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources) {
        ArrayList<CloudResourceStatus> cloudResourceStatuses = new ArrayList<>();
        if (!resources.isEmpty() && resources.stream().anyMatch(resource -> CommonResourceType.TEMPLATE == resource.getType().getCommonResourceType()
                && StringUtils.isNotBlank(resource.getStringParameter(CloudResource.IMAGE)))) {

            List<CloudResource> launchConfigurationResources = resources.stream()
                    .filter(resource -> CommonResourceType.TEMPLATE == resource.getType().getCommonResourceType()
                            && StringUtils.isNotBlank(resource.getStringParameter(CloudResource.IMAGE))).collect(Collectors.toList());

            CloudResource cfResource = resources.stream().filter(resource -> ResourceType.CLOUDFORMATION_STACK == resource.getType()).findFirst().orElseThrow();
            awsImageUpdateService.updateImage(authenticatedContext, stack, cfResource);

            launchConfigurationResources.forEach(cloudResource -> cloudResourceStatuses.add(new CloudResourceStatus(cloudResource, ResourceStatus.UPDATED)));
        }
        return cloudResourceStatuses;
    }

    @Override
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
        resumeAutoScaling(amazonASClient, groupMap.keySet(), UPSCALE_PROCESSES);
        for (Map.Entry<String, Group> groupEntry : groupMap.entrySet()) {
            Group group = groupEntry.getValue();
            amazonASClient.updateAutoScalingGroup(new UpdateAutoScalingGroupRequest()
                    .withAutoScalingGroupName(groupEntry.getKey())
                    .withMaxSize(group.getInstancesSize())
                    .withDesiredCapacity(group.getInstancesSize()));
            LOGGER.info("Updated Auto Scaling group's desiredCapacity: [stack: '{}', to: '{}']", ac.getCloudContext().getId(),
                    group.getInstancesSize());
        }
        scheduleStatusChecks(stack, ac, cloudFormationClient);
        suspendAutoScaling(ac, stack);

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

        buildComputeResourcesForUpscale(ac, stack, scaledGroups, instances);

        return singletonList(new CloudResourceStatus(cfStackUtil.getCloudFormationStackResource(resources), ResourceStatus.UPDATED));
    }

    @Override
    public Object collectResourcesToRemove(AuthenticatedContext authenticatedContext, CloudStack stack,
            List<CloudResource> resources, List<CloudInstance> vms) {
        return null;
    }

    @Override
    public List<CloudResourceStatus> downscale(AuthenticatedContext auth, CloudStack stack, List<CloudResource> resources, List<CloudInstance> vms,
            Object resourcesToRemove) {
        if (!vms.isEmpty()) {
            List<String> instanceIds = new ArrayList<>();
            for (CloudInstance vm : vms) {
                instanceIds.add(vm.getInstanceId());
            }

            List<CloudResource> resourcesToDownscale = resources.stream()
                    .filter(resource -> instanceIds.contains(resource.getInstanceId()))
                    .collect(Collectors.toList());
            deleteComputeResources(auth, stack, resourcesToDownscale);

            String asGroupName = cfStackUtil.getAutoscalingGroupName(auth, vms.get(0).getTemplate().getGroupName(),
                    auth.getCloudContext().getLocation().getRegion().value());
            DetachInstancesRequest detachInstancesRequest = new DetachInstancesRequest().withAutoScalingGroupName(asGroupName).withInstanceIds(instanceIds)
                    .withShouldDecrementDesiredCapacity(true);
            AmazonAutoScalingRetryClient amazonASClient = awsClient.createAutoScalingRetryClient(new AwsCredentialView(auth.getCloudCredential()),
                    auth.getCloudContext().getLocation().getRegion().value());
            detachInstances(instanceIds, detachInstancesRequest, amazonASClient);
            AmazonEC2Client amazonEC2Client = awsClient.createAccess(new AwsCredentialView(auth.getCloudCredential()),
                    auth.getCloudContext().getLocation().getRegion().value());
            terminateInstances(instanceIds, amazonEC2Client);
            LOGGER.info("Terminated instances in stack '{}': '{}'", auth.getCloudContext().getId(), instanceIds);
            try {
                amazonASClient.updateAutoScalingGroup(new UpdateAutoScalingGroupRequest()
                        .withAutoScalingGroupName(asGroupName)
                        .withMaxSize(getInstanceCount(stack, vms.get(0).getTemplate().getGroupName())));
            } catch (AmazonServiceException e) {
                LOGGER.warn(e.getErrorMessage());
            }
        }
        return check(auth, resources);
    }

    private void terminateInstances(List<String> instanceIds, AmazonEC2Client amazonEC2Client) {
        try {
            amazonEC2Client.terminateInstances(new TerminateInstancesRequest().withInstanceIds(instanceIds));
        } catch (AmazonServiceException e) {
            if (!INSTANCE_NOT_FOUND_ERROR_CODE.equals(e.getErrorCode())) {
                throw e;
            }
            LOGGER.info(e.getErrorMessage());
        }
    }

    private void detachInstances(List<String> instanceIds, DetachInstancesRequest detachInstancesRequest, AmazonAutoScalingRetryClient amazonASClient) {
        try {
            amazonASClient.detachInstances(detachInstancesRequest);
        } catch (AmazonServiceException e) {
            if (!"ValidationError".equals(e.getErrorCode())
                    || !e.getErrorMessage().contains("not part of Auto Scaling")
                    || instanceIds.stream().anyMatch(id -> !e.getErrorMessage().contains(id))) {
                throw e;
            }
            LOGGER.info(e.getErrorMessage());
        }
    }

    private int getInstanceCount(CloudStack stack, String groupName) {
        int result = -1;
        Optional<Group> group = stack.getGroups().stream().filter(g -> g.getName().equals(groupName)).findFirst();
        if (group.isPresent()) {
            result = (int) group.get().getInstances().stream().filter(inst -> !inst.getTemplate().getStatus().equals(InstanceStatus.DELETE_REQUESTED)).count();
        }
        return result;
    }

    @Override
    public TlsInfo getTlsInfo(AuthenticatedContext authenticatedContext, CloudStack cloudStack) {
        Network network = cloudStack.getNetwork();
        AwsNetworkView networkView = new AwsNetworkView(network);
        boolean sameVPC = deployingToSameVPC(networkView, networkView.isExistingVPC());
        return new TlsInfo(sameVPC);
    }

    @Override
    public String getStackTemplate() {
        try {
            return freemarkerConfiguration.getTemplate(awsCloudformationTemplatePath, "UTF-8").toString();
        } catch (IOException e) {
            throw new CloudConnectorException("can't get freemarker template", e);
        }
    }

    private List<String> getInstancesForGroup(AuthenticatedContext ac, AmazonAutoScalingRetryClient amazonASClient, AmazonCloudFormationRetryClient client,
            Group group) {
        return cfStackUtil.getInstanceIds(amazonASClient, cfStackUtil.getAutoscalingGroupName(ac, client, group.getName()));
    }

    private void scheduleStatusChecks(CloudStack stack, AuthenticatedContext ac, AmazonCloudFormationRetryClient cloudFormationClient) {
        for (Group group : stack.getGroups()) {
            String asGroupName = cfStackUtil.getAutoscalingGroupName(ac, cloudFormationClient, group.getName());
            LOGGER.info("Polling Auto Scaling group until new instances are ready. [stack: {}, asGroup: {}]", ac.getCloudContext().getId(),
                    asGroupName);
            PollTask<Boolean> task = awsPollTaskFactory.newASGroupStatusCheckerTask(ac, asGroupName, group.getInstancesSize(), awsClient, cfStackUtil);
            try {
                awsBackoffSyncPollingScheduler.schedule(task);
            } catch (Exception e) {
                throw new CloudConnectorException(e.getMessage(), e);
            }
        }
    }
}
