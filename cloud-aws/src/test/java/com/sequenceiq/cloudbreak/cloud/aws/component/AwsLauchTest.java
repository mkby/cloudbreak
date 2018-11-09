package com.sequenceiq.cloudbreak.cloud.aws.component;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AWS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;

import com.amazonaws.AmazonServiceException;
import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupType;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.encryption.EncryptedImageCopyService;
import com.sequenceiq.cloudbreak.cloud.aws.resourceconnector.AwsResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Subnet;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;

public class AwsLauchTest extends AwsComponentTest {

    private static final String LOGIN_USER_NAME = "loginusername";

    private static final String PUBLIC_KEY = "pubkey";

    private static final int ROOT_VOLUME_SIZE = 50;

    private static final String CORE_CUSTOM_DATA = "CORE";

    private static final String GATEWAY_CUSTOM_DATA = "GATEWAY";

    public static final String CIDR = "10.10.10.10/16";

    @Inject
    private AwsResourceConnector awsResourceConnector;

    @Inject
    private PersistenceNotifier persistenceNotifier;

    @Inject
    private AmazonCloudFormationRetryClient amazonCloudFormationRetryClient;

    @Inject
    private EncryptedImageCopyService encryptedImageCopyService;

    @Test
    public void launchStack() throws Exception {
        when(amazonCloudFormationRetryClient.describeStacks(any())).thenThrow(new AmazonServiceException("stack is supposed to exist in test"));

//        awsResourceConnector.launch(getAuthenticatedContext(), getStack(), persistenceNotifier, AdjustmentType.EXACT, Long.MAX_VALUE);
    }

    private AuthenticatedContext getAuthenticatedContext() {
        Location location = location(region("region"), availabilityZone("availabilityZone"));
        CloudContext cloudContext = new CloudContext(1L, "cloudContextName", AWS, "owner@company.com", "variant", location, "", 5L);
        CloudCredential cloudCredential = new CloudCredential(3L, "credentialName");
        return new AuthenticatedContext(cloudContext, cloudCredential);
    }

    private CloudStack getStack() {
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication(PUBLIC_KEY, "pubkeyid", LOGIN_USER_NAME);
        List<Group> groups = List.of(new Group("group1", InstanceGroupType.CORE, Collections.emptyList(), null, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE));
        Map<String, Object> networkParameters = new HashMap<>();
        networkParameters.put("vpcId", "vpc-12345678");
        networkParameters.put("internetGatewayId", "igw-12345678");
        Network network = new Network(new Subnet(CIDR));

        Map<InstanceGroupType, String> userData = ImmutableMap.of(
                InstanceGroupType.CORE, CORE_CUSTOM_DATA,
                InstanceGroupType.GATEWAY, GATEWAY_CUSTOM_DATA
        );
        Image image = new Image("cb-centos66-amb200-2015-05-25", userData, "redhat6", "redhat6", "", "default", "default-id", new HashMap<>());

        return new CloudStack(groups, network, image, Map.of(), Map.of(), "template", instanceAuthentication, LOGIN_USER_NAME, PUBLIC_KEY, null);
    }
}
