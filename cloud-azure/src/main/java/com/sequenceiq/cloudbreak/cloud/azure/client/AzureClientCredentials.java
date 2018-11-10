package com.sequenceiq.cloudbreak.cloud.azure.client;

import java.util.Optional;

import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.rest.LogLevel;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureCredentialView;

import okhttp3.JavaNetAuthenticator;

public class AzureClientCredentials {

    private final AzureCredentialView credentialView;

    private final LogLevel logLevel;

    private final AzureTokenCredentials azureClientCredentials;

    public AzureClientCredentials(AzureCredentialView credentialView, LogLevel logLevel) {
        this.credentialView = credentialView;
        this.logLevel = logLevel;
        this.azureClientCredentials = getAzureCredentials();
    }

    public Azure getAzure() {
        return Azure
                .configure()
                .withProxyAuthenticator(new JavaNetAuthenticator())
                .withLogLevel(logLevel)
                .authenticate(azureClientCredentials)
                .withSubscription(credentialView.getSubscriptionId());
    }

    private AzureTokenCredentials getAzureCredentials() {
        String tenantId = credentialView.getTenantId();
        String clientId = credentialView.getAccessKey();
        String secretKey = credentialView.getSecretKey();
        String subscriptionId = credentialView.getSubscriptionId();
        ApplicationTokenCredentials applicationTokenCredentials = new ApplicationTokenCredentials(clientId, tenantId, secretKey, AzureEnvironment.AZURE);
        Optional<Boolean> codeGrantFlow = Optional.ofNullable(credentialView.getCodeGrantFlow());

        AzureTokenCredentials result = applicationTokenCredentials.withDefaultSubscriptionId(subscriptionId);
        if (codeGrantFlow.orElse(Boolean.FALSE)) {
            //TODO Handle existing refresh token
            result = new CbDelegatedTokenCredentials(applicationTokenCredentials, credentialView.getAppReplyUrl(), credentialView.getAuthorizationCode())
                    .withDefaultSubscriptionId(subscriptionId);
        }
        return result;
    }

    public Optional<String> getRefreshToken() {
        String refreshToken = null;
        Optional<Boolean> codeGrantFlow = Optional.ofNullable(credentialView.getCodeGrantFlow());
        if (codeGrantFlow.orElse(Boolean.FALSE)) {
            CbDelegatedTokenCredentials delegatedCredentials = (CbDelegatedTokenCredentials) azureClientCredentials;
            Optional<AuthenticationResult> authenticationResult = delegatedCredentials.getTokens()
                    .values()
                    .stream()
                    .findFirst();

            if (authenticationResult.isPresent()) {
                refreshToken = authenticationResult.get().getRefreshToken();
            }
        }
        return Optional.ofNullable(refreshToken);
    }
}
