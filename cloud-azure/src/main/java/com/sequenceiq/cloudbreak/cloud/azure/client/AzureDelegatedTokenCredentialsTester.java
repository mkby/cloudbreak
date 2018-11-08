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
        String refreshToken = "AQABAAAAAAC5una0EUFgTIF8ElaxtWjTFKPqUksF7EwnVAOMsunow-KxNIg2xTo6YOmYAfsQyZh3gJxO7KcBZ2LARTZQ-6vH_wRXEH5-0-7rrB1oA0EXEv5OC91dDgB8E9fmQtCBAbjh4okmC6M_XjdEWl-3Olieg-HVRY_d9ekLBIm5V-TjWC1mJs8AvE6-39ve2xqRBWEzOiD4hxR9WjPEQGtShtSsG7FtPcIMUbrlFhdwXa7CNKhcUg2qpZx4t_2ORTRaWECYdj2ZwEIW4dPWk6_YhJVg51So2rjtWrrm2WB7yTgdQxtzN6aXTBwuyh4ETaVkb9-dNyoMeQ_8QBpuJgt6iQewRsXbqIE1hbDt0JdiYCty-qB1yzo6rvO2XTHF2sxYJS34G5hrCBCApKVIb9TDjupdhkR1J5K8R-x9zJoScvcsUaSfcl3ZStKfENprrYj8tyyiAAF14bGdN7ROZAFG1YLYkx96q2kCuvP71QbU2-LXUtY0QyPsEcm9EdVxBXO-laMYCdgqTj0m6gbSoAcHVw5I4mjFtcyeLZ28_t6FKmFmBh31s592swINiqkpQGRiSGDU4RUEVos3Q9q1N2R71R_yHynOrMWnOB8LDFfmO9iYobS0yB_kvt344y4bFZLTKEOGPec9TOzpjX9XalAPvmRhTB8CzHR3MHazRYcU7uFwmgPFEhcbCQzDXnf3yBVx6tJz9z3FtQABWcoYCmUo-hpcIlqj9ch2DwjJECEHVC9S6ie6i0JrQ1cPXkRqUvnnDRALjwon4ZLTycQ-f6m1poMRQ-3txZv4uzp6-BHLuanz-BKD0xmt74uhWYO3P8dcsNTw51FyhNALPqhMqZxIyPSHHiqGBh3OR4gQ6LUlBtlNmOfuwr_x2ABZrWWl2UCmKdj8PP_H2U4GTdXR33FRn7D_IAA";

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
