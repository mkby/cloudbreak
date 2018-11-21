package com.sequenceiq.cloudbreak.cloud.azure.client;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.microsoft.rest.LogLevel;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureCredentialView;

public class AzureClientCredentialsTest {

    private static final String TENANT_ID = "1";

    private static final String ACCESS_KEY = "123";

    private static final String SECRET_KEY = "someSecretKey";

    private static final String SUBSCRIPTION_ID = "4321";

    @Mock
    private AzureCredentialView credentialView;

    private AzureClientCredentials underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(credentialView.getTenantId()).thenReturn(TENANT_ID);
        when(credentialView.getAccessKey()).thenReturn(ACCESS_KEY);
        when(credentialView.getSecretKey()).thenReturn(SECRET_KEY);
        when(credentialView.getSubscriptionId()).thenReturn(SUBSCRIPTION_ID);

        underTest = new AzureClientCredentials(credentialView, LogLevel.NONE);
    }

    @Test
    public void testGetRefreshTokenWhenCredentialFlowIsNotCodeGrantFlowThenEmptyOptionalReturns() {
        when(credentialView.getCodeGrantFlow()).thenReturn(false);

        Optional<String> result = underTest.getRefreshToken();

        assertFalse(result.isPresent());
    }

    public void testGetRefreshTokenWhenThereIsNoDelegatedCredentialTokenThenEmptyOptionalReturns() {
        when(credentialView.getCodeGrantFlow()).thenReturn(true);
        when(credentialView.getRefreshToken()).thenReturn("someRefreshTokenStuff");

        Optional<String> result = underTest.getRefreshToken();

        assertFalse(result.isPresent());
    }

}