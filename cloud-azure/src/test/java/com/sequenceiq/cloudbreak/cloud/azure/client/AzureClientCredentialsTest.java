package com.sequenceiq.cloudbreak.cloud.azure.client;

import static java.lang.String.format;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.microsoft.rest.LogLevel;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureCredentialView;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

public class AzureClientCredentialsTest {

    private static final String TENANT_ID = "1";

    private static final String ACCESS_KEY = "123";

    private static final String SECRET_KEY = "someSecretKey";

    private static final String SUBSCRIPTION_ID = "4321";

    private static final String CREDENTIAL_NAME = "someCredName";

    private static final LogLevel LOG_LEVEL = LogLevel.NONE;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private AzureCredentialView credentialView;

    private AzureClientCredentials underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(credentialView.getTenantId()).thenReturn(TENANT_ID);
        when(credentialView.getAccessKey()).thenReturn(ACCESS_KEY);
        when(credentialView.getSecretKey()).thenReturn(SECRET_KEY);
        when(credentialView.getName()).thenReturn(CREDENTIAL_NAME);
        when(credentialView.getSubscriptionId()).thenReturn(SUBSCRIPTION_ID);
        underTest = Mockito.spy(new AzureClientCredentials(credentialView, LOG_LEVEL));
    }

    @Test
    public void testGetRefreshTokenWhenCredentialFlowIsNotCodeGrantFlowThenEmptyOptionalReturns() {
        when(credentialView.getCodeGrantFlow()).thenReturn(false);
        underTest = new AzureClientCredentials(credentialView, LOG_LEVEL);

        Optional<String> result = underTest.getRefreshToken();

        assertFalse(result.isPresent());
    }

    @Test
    public void testGetRefreshTokenWhenUnableToObtainAuthenticationResultThenCloudConnectorExceptionComes() {
        when(credentialView.getCodeGrantFlow()).thenReturn(true);
        when(credentialView.getRefreshToken()).thenReturn("someRefreshTokenStuff");

        thrown.expect(CloudConnectorException.class);
        thrown.expectMessage(format("New token couldn't be obtain with refresh token for credential: %s", CREDENTIAL_NAME));

        underTest = new AzureClientCredentials(credentialView, LOG_LEVEL);
    }

}