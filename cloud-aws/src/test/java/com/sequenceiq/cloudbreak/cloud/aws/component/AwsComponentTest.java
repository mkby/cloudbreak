package com.sequenceiq.cloudbreak.cloud.aws.component;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.junit4.SpringRunner;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.AwsPlatformParameters;
import com.sequenceiq.cloudbreak.cloud.aws.AwsPlatformResources;
import com.sequenceiq.cloudbreak.cloud.aws.AwsTagValidator;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.component.AwsComponentTest.AwsTestContext;
import com.sequenceiq.cloudbreak.cloud.aws.resourceconnector.AwsResourceConnector;
import com.sequenceiq.cloudbreak.cloud.notification.ResourceNotifier;
import com.sequenceiq.cloudbreak.cloud.reactor.config.CloudReactorConfiguration;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.template.GroupResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.NetworkResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.transform.CloudResourceHelper;
import com.sequenceiq.cloudbreak.common.service.DefaultCostTaggingService;
import com.sequenceiq.cloudbreak.concurrent.MDCCleanerTaskDecorator;
import com.sequenceiq.cloudbreak.service.Retry;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = AwsTestContext.class)
public class AwsComponentTest {

    @Inject
    private AwsResourceConnector awsResourceConnector;

    @Inject
    private AwsClient awsClient;

    @Test
    public void checkTestContext() {
        assertNotNull(awsResourceConnector);
        System.out.println(awsClient);
    }

    @Configuration
    @ComponentScans(value = {
            @ComponentScan(basePackages = {"com.sequenceiq.cloudbreak.cloud.aws", "com.sequenceiq.cloudbreak.cloud.template"}),
            @ComponentScan(basePackages = "com.sequenceiq.cloudbreak.cloud.reactor.config", useDefaultFilters = false,
                    includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = {
                            CloudReactorConfiguration.class
                    })),
            @ComponentScan(basePackages = "com.sequenceiq.cloudbreak.cloud.scheduler", useDefaultFilters = false,
                    includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = {
                            SyncPollingScheduler.class
                    }))
    })
    public static class AwsTestContext{

        private static final int INTERMEDIATE_CORE_POOL_SIZE = 5;

        private static final int INTERMEDIATE_QUEUE_CAPACITY = 20;

        @MockBean(name="DefaultRetryService")
        private Retry defaultRetryService;

        @MockBean
        private freemarker.template.Configuration configurationProvider;

        @MockBean
        private DefaultCostTaggingService defaultCostTaggingService;

        @MockBean
        private NetworkResourceBuilder networkResourceBuilder;

        @MockBean
        private GroupResourceBuilder groupResourceBuilder;

        @MockBean
        private ResourceNotifier resourceNotifier;

        @MockBean
        private AwsPlatformResources awsPlatformResources;

        @MockBean
        private AwsPlatformParameters awsPlatformParameters;

        @MockBean
        private AwsTagValidator awsTagValidator;

        @MockBean
        private AmazonCloudFormationRetryClient amazonCloudFormationRetryClient;

        @Bean
        public AwsClient awsClient(){
            AwsClient awsClient= mock(AwsClient.class);
            when(awsClient.createAccess(any(), anyString())).thenReturn(mock(AmazonEC2Client.class));
            when(awsClient.createAccess(any())).thenReturn(mock(AmazonEC2Client.class));
            when(awsClient.createCloudFormationRetryClient(any(), anyString())).thenReturn(amazonCloudFormationRetryClient);
            return awsClient;
        }

        @Bean
        public AsyncTaskExecutor resourceBuilderExecutor() {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(INTERMEDIATE_CORE_POOL_SIZE);
            executor.setQueueCapacity(INTERMEDIATE_QUEUE_CAPACITY);
            executor.setThreadNamePrefix("intermediateBuilderExecutor-");
            executor.setTaskDecorator(new MDCCleanerTaskDecorator());
            executor.initialize();
            return executor;
        }

        @Bean
        public AsyncTaskExecutor intermediateBuilderExecutor() {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(INTERMEDIATE_CORE_POOL_SIZE);
            executor.setQueueCapacity(INTERMEDIATE_QUEUE_CAPACITY);
            executor.setThreadNamePrefix("intermediateBuilderExecutor-");
            executor.setTaskDecorator(new MDCCleanerTaskDecorator());
            executor.initialize();
            return executor;
        }

        @Bean
        public CloudResourceHelper cloudResourceHelper(){
            return new CloudResourceHelper();
        }

    }
}
