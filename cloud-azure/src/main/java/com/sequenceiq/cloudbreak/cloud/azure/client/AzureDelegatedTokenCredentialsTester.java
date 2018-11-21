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
        String refreshToken = "AQABAAAAAAC5una0EUFgTIF8ElaxtWjTHsQHtO0xCk7E9G3rYE_Qn3M12vo_DzrbGOQJSED1nFDOE-QqboIq2rbOwShtpNmZ7DLSZVaGWjPVV4ayGhekObZxI_DhliEPSUYFzGXH3I12moQCqGdJ0uVWaYrotVlKcXTKnZHEiEPKVkepX4wB3_f45Vcl_mpQja5F-9TDvnUfTGeYA6YCIWZ6YTSO9g_cxMU5C4tlDIQTCOT9_K8dSv55TCSg3lTCS5K4PvM4_hzGaob1Eo9NQojRt2nFajfhgy1CsvnIshoBCfLxdR3PZYQAaXCYBJaGBIrcyB9Sbxrno-1IBxzmuAGQ3VrJ9a87hIXB4CAFL3jvH7dLwb3H_ADjF9E697H0s_FFrC82IxT53yFCSlP-9EdTfGHuGsUxoz6cFC97DBY2nrgv5DX7sy6Qknpw50KpA76hmv0YZasBFA558TtveB80W4mKpqCdI27dmBCtl2wrev0a6YoOoGIEpDeLkU5xYb6HFrJVb7gVZgbEbfqVI3-3iWqI0_tnru2TxHex7eGWUtDkOSvCOAMwb7MY179bjq0ZsGE5vX2LH95UQe8Tq9oV8leFCrG4T5YABPC4CDezdHCk5A1BKHv2_bhlIJpr1zDlhDWcuFU0IOIVTzoLnFsrqFe9RtSRruN2Z1bjCkQxcN8x9oE_YRzwfJ112pZMRxEPwN-bUKrsIrlDWYrn0huHjc0Xgno4gh9zSeQij-zLFJr1IwgNKKrgL7EYeJMn-_bfn3uCPnf-O2luHuRmYxxL4HEDD20wtE53rxcgtcuV9mEC1GMEeYq-Dd6qAt4OZDHo8o";

        AzureEnvironment azureEnv = AzureEnvironment.AZURE;
        JavaNetAuthenticator proxyAuthenticator = new JavaNetAuthenticator();

        CBRefreshTokenClient refreshTokenClient = new CBRefreshTokenClient(azureEnv.activeDirectoryEndpoint(), null);

        String resource = azureEnv.managementEndpoint();
        AuthenticationResult authenticationResult = refreshTokenClient.refreshToken(tenantId, clientId, clientSecret, resource, refreshToken, false);
        LOGGER.info("AccessToken:{}", authenticationResult.getAccessToken());
        LOGGER.info("RefreshToken: {}", authenticationResult.getRefreshToken());

        Map<String, AuthenticationResult> tokens = Map.of(resource, authenticationResult);
        ApplicationTokenCredentials applicationCredentials = new ApplicationTokenCredentials(clientId, tenantId, clientSecret, azureEnv);
        AzureTokenCredentials creds = new CbDelegatedTokenCredentials(
                applicationCredentials,
                resource,
                tokens,
                clientSecret,
                new AuthenticationContextProvider(),
                new CBRefreshTokenClientProvider())
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
