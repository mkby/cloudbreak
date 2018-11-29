package com.sequenceiq.it.cloudbreak.newway.testcase;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.force;
import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;

import javax.ws.rs.BadRequestException;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.Environment;
import com.sequenceiq.it.cloudbreak.newway.EnvironmentEntity;
import com.sequenceiq.it.cloudbreak.newway.LdapConfigEntity;
import com.sequenceiq.it.cloudbreak.newway.RdsConfigEntity;
import com.sequenceiq.it.cloudbreak.newway.Recipe;
import com.sequenceiq.it.cloudbreak.newway.RecipeEntity;
import com.sequenceiq.it.cloudbreak.newway.StackEntity;
import com.sequenceiq.it.cloudbreak.newway.action.ClusterTemplateCreateAction;
import com.sequenceiq.it.cloudbreak.newway.action.ClusterTemplateDeleteAction;
import com.sequenceiq.it.cloudbreak.newway.action.ClusterTemplateListAction;
import com.sequenceiq.it.cloudbreak.newway.action.DeleteClusterFromTemplateAction;
import com.sequenceiq.it.cloudbreak.newway.action.LaunchClusterFromTemplateAction;
import com.sequenceiq.it.cloudbreak.newway.action.LdapConfigCreateIfNotExistsAction;
import com.sequenceiq.it.cloudbreak.newway.action.RdsConfigCreateIfNotExistsAction;
import com.sequenceiq.it.cloudbreak.newway.assertion.CheckClusterTemplateFirstResponse;
import com.sequenceiq.it.cloudbreak.newway.assertion.CheckClusterTemplateResponses;
import com.sequenceiq.it.cloudbreak.newway.assertion.CheckStackTemplateAfterClusterTemplateCreation;
import com.sequenceiq.it.cloudbreak.newway.assertion.CheckStackTemplateAfterClusterTemplateCreationWithProperties;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.ClusterTemplateEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.GeneralSettingsEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.PlacementSettingsEntity;
import com.sequenceiq.it.cloudbreak.newway.entity.StackTemplateEntity;

public class ClusterTemplateTest extends AbstractIntegrationTest {

    public static final String EUROPE = "Europe";

    private static final Set<String> VALID_REGION = Collections.singleton(EUROPE);

    private static final String VALID_LOCATION = "London";

    @BeforeMethod
    public void beforeMethod(Method method, Object[] data) {
        TestContext testContext = (TestContext) data[0];

        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
    }

    @Test(dataProvider = "testContext")
    public void testClusterTemplateCreateAndListAndDelete(TestContext testContext) {
        testContext
                .given("environment", EnvironmentEntity.class)
                .withRegions(VALID_REGION)
                .withLocation(VALID_LOCATION)
                .when(Environment::post)
                .given("generalSettings", GeneralSettingsEntity.class).withEnvironmentKey("environment")
                .given("stackTemplate", StackEntity.class).withGeneralSettings("generalSettings")
                .given(ClusterTemplateEntity.class).withStackTemplate("stackTemplate")
                .when(new ClusterTemplateCreateAction())
                .when(new ClusterTemplateListAction())
                .then(new CheckClusterTemplateResponses(1))
                .then(new CheckClusterTemplateFirstResponse())
                .then(new CheckStackTemplateAfterClusterTemplateCreation())
                .when(new ClusterTemplateDeleteAction())
                .when(new ClusterTemplateListAction())
                .then(new CheckClusterTemplateResponses(0))
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testLaunchClusterFromTemplate(TestContext testContext) {
        testContext.given("environment", EnvironmentEntity.class).withRegions(VALID_REGION).withLocation(VALID_LOCATION)
                .when(Environment::post)
                .given("generalSettings", GeneralSettingsEntity.class).withEnvironmentKey("environment")
                .given("placementSettings", PlacementSettingsEntity.class).withRegion(EUROPE)
                .given("stackTemplate", StackEntity.class).withGeneralSettings("generalSettings").withPlacementSettings("placementSettings")
                .given(ClusterTemplateEntity.class).withStackTemplate("stackTemplate")
                .when(new ClusterTemplateCreateAction())
                .when(new LaunchClusterFromTemplateAction("stackTemplate"))
                .await(STACK_AVAILABLE, key("stackTemplate"))
                .when(new DeleteClusterFromTemplateAction("stackTemplate"))
                .await(STACK_DELETED, key("stackTemplate"))
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testCreateClusterTemplateWithoutEnvironment(TestContext testContext) {
        testContext.given("stackTemplate", StackEntity.class)
                .given(ClusterTemplateEntity.class).withStackTemplate("stackTemplate")
                .when(new ClusterTemplateCreateAction(), key("ENVIRONMENT_NULL"))
                .except(BadRequestException.class, key("ENVIRONMENT_NULL"))
                .validate();
    }

    @Test(dataProvider = "testContext")
    public void testLaunchClusterFromTemplateWithProperties(TestContext testContext) {
        testContext
                .given(LdapConfigEntity.class).withName("mock-test-ldap")
                .when(new LdapConfigCreateIfNotExistsAction())
                .given(RecipeEntity.class).withName("mock-test-recipe")
                .when(Recipe.postV2())
                .given(RdsConfigEntity.class).withName("mock-test-rds")
                .when(new RdsConfigCreateIfNotExistsAction())
                .given("environment", EnvironmentEntity.class).withRegions(VALID_REGION).withLocation(VALID_LOCATION)
                .when(Environment::post)
                .given("stackTemplate", StackTemplateEntity.class).withEveryProperties()
                .given(ClusterTemplateEntity.class).withStackTemplate("stackTemplate")
                .when(new ClusterTemplateCreateAction())
                .when(new ClusterTemplateListAction())
                .then(new CheckClusterTemplateResponses(1))
                .then(new CheckStackTemplateAfterClusterTemplateCreationWithProperties())
                .when(new LaunchClusterFromTemplateAction("stackTemplate"))
                .await(STACK_AVAILABLE, key("stackTemplate"))
                .when(new DeleteClusterFromTemplateAction("stackTemplate"), force())
                .await(STACK_DELETED, key("stackTemplate").withSkipOnFail(false))
                .validate();
    }

    @AfterMethod(alwaysRun = true)
    public void tear(Object[] data) {
        TestContext testContext = (TestContext) data[0];
        testContext.cleanupTestContextEntity();
    }
}
