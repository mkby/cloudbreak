package com.sequenceiq.cloudbreak.cloud.azure.client;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.rest.LogLevel;

import okhttp3.JavaNetAuthenticator;

public class AzureDelegatedTokenCredentialsTester {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureDelegatedTokenCredentialsTester.class);

    private AzureDelegatedTokenCredentialsTester() { }

    //CHECKSTYLE:OFF
    public static void main(String[] args) {

        LogLevel logLevel = LogLevel.BASIC;
        String subscriptionId = "a9d4456e-349f-44f6-bc73-54a8d523e504";
        String tenantId = "b60c9401-2154-40aa-9cff-5e3d1a20085d";
        String clientId = "5770ee06-a427-4f45-9a84-e20babfc41db";
        String clientSecret = "Cloudbreak123!";
        String refreshToken = "AQABAAAAAAC5una0EUFgTIF8ElaxtWjTOVsC3gfzRrc7piWPEhu3be1MJ7oCZZJDoMnR3v6vyL9tkDmOwfF0-41QKkJsajkavDxqrii1SDOH1rKyHFVLp02F4JX5q7RUk3Xf9MxFRrUh-X6Mz7SsrxFb7XC4k1y74htLMAOaWkLJQiKDHJMxB6vOc_1reeLdddCOxBqwyYi9U9hs2jgp1RfIS9kwQg9QtxYhWVB8Vh2135afY76GuSap9ZK7QjbWRE9TRQ71T7QJChThTfW1Fixqq1fCUNdtIVp1L0oZlxm3ud4AJkbqKmxn_D3RIFCb7BT3tmU7qdodncecgYKsSczADQ3mh2Qu0cVXfnSRcITAAkowvf9XzwTjYLg1CbILQp_GnFlc1MFeGPai4JpPB-A0ZhpZfzON2HhG8jW46G1skIQhVCe_pU2Kq44g1mn-W1PF6AjKwaihj51PEhuAjRBrwQI8ogu-xZQHtGbEviayUvoTucZzYJu-DZWiUMZAoTY5E9R1LvN_grx8E9o4Iqm0T0-UfuXLFvY_T18rXNbs9M3bWK89LMQ7MJdS_s1rWUAShSkl5gXReFiFNsS1GFEbRHBD3Y7wV6zhhWOS6nCldXEAXHxpkMGf8RAITLdI-26TxovQWDEHm7fjPUi6UClMy5tUNP9CK8Ipz9HSi5PkJ1yWetCf96EbvQqYwKgmi7p1rDTPfBCuhv-zja8wNO_UwADFKguzbU62XLFwH_sRuBhnirUrCxeeKB9UCeMYP7tXZkTESfujvKiAw5duiTTI9o6cvKKDCePWZacgHJ-hHjFwwcIfNhB72pG9R3mBdD8d4wM3xjEODxnhAon9A-3TuUm2mUuaCVS38Sc19Cfr1JY7-6DpPjtYv1fQWb3lIL3MJhJxTa4dcV0TQAGo8pabxmB3cfkxIAA";

        AzureEnvironment azureEnv = AzureEnvironment.AZURE;
        JavaNetAuthenticator proxyAuthenticator = new JavaNetAuthenticator();

        CBRefreshTokenClient refreshTokenClient = new CBRefreshTokenClient(azureEnv.activeDirectoryEndpoint(), null);

        String resource = azureEnv.managementEndpoint();
        AuthenticationResult authenticationResult = refreshTokenClient.refreshToken(tenantId, clientId, clientSecret, resource, refreshToken, false);
        LOGGER.info("AccessToken:{}", authenticationResult.getAccessToken());
        LOGGER.info("RefreshToken: {}", authenticationResult.getRefreshToken());

        Map<String, AuthenticationResult> tokens = Map.of(resource, authenticationResult);
        ApplicationTokenCredentials applicationCredentials = new ApplicationTokenCredentials(clientId, tenantId, clientSecret, azureEnv);
        AzureTokenCredentials creds = new CbDelegatedTokenCredentials(applicationCredentials, resource, tokens, clientSecret)
                .withDefaultSubscriptionId(subscriptionId);


        Azure azure = Azure.configure()
                .withProxyAuthenticator(proxyAuthenticator)
                .withLogLevel(logLevel)
                .authenticate(creds)
                .withSubscription(subscriptionId);

        PagedList<StorageAccount> list = azure.storageAccounts().list();
        list.forEach(storageAccount -> LOGGER.info("SA:{}", storageAccount.name()));

    }
    //CHECKSTYLE:ON
}
