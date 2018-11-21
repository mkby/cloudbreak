package com.sequenceiq.cloudbreak.cloud.azure.client;

import static java.lang.String.format;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.microsoft.aad.adal4j.AuthenticationException;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.UserInfo;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;

public class CbDelegatedTokenCredentialsTest {

    private static final String HTTPS = "https";

    private static final String HTTP = "http";

    private static final String TEST_AD_ENDPOINT = "%s://192.168.0.1";

    private static final String TEST_DOMAIN = "testdomain";

    private static final String REDIRECT_URL = "someotherurl.toredirect";

    private static final String CLIENT_SECRET = "someSecret";

    private static final String RESOURCE = "someResource";

    private static final String ACCESS_TOKEN = "someAccessTokenValue";

    private static final String REFRESH_TOKEN = "someRefreshToken";

    private static final String AUTHORIZATION_CODE = "someAuthCode";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private ApplicationTokenCredentials applicationTokenCredentials;

    private Map<String, AuthenticationResult> tokens;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        tokens = new LinkedHashMap<>();
        tokens.put(RESOURCE, new AuthenticationResult("type", ACCESS_TOKEN, REFRESH_TOKEN, 123456789L, "1", mock(UserInfo.class), true));
        when(applicationTokenCredentials.environment()).thenReturn(new AzureEnvironment(Map.of("activeDirectoryEndpointUrl", format(TEST_AD_ENDPOINT, HTTPS))));
        when(applicationTokenCredentials.domain()).thenReturn(TEST_DOMAIN);
    }

    @Test
    public void testAcquireNewAccessTokenWhenNoAuthorizationCodeThenExceptionComes() throws IOException {
        CbDelegatedTokenCredentials underTest = new CbDelegatedTokenCredentials(applicationTokenCredentials, REDIRECT_URL, tokens, CLIENT_SECRET);

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("You must acquire an authorization code by redirecting to the authentication URL");

        underTest.acquireNewAccessToken(RESOURCE);
    }

    @Test
    public void testGetTokenWhenAccessTokenExistsThenItComesBack() throws IOException {
        CbDelegatedTokenCredentials underTest = new CbDelegatedTokenCredentials(applicationTokenCredentials, REDIRECT_URL, tokens, CLIENT_SECRET);

        String result = underTest.getToken(RESOURCE);

        Assert.assertEquals(ACCESS_TOKEN, result);
    }

    @Test
    public void testGetTokenWhenNoTokenAndAuthCodeProvidedThenIllegalArgumentExceptionComes() throws IOException {
        CbDelegatedTokenCredentials underTest = new CbDelegatedTokenCredentials(applicationTokenCredentials, REDIRECT_URL, Collections.emptyMap(),
                CLIENT_SECRET);

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("You must acquire an authorization code by redirecting to the authentication URL");

        underTest.getToken(RESOURCE);
    }

    @Test
    public void testGetTokenWhenAuthCodeGivenButNoTokenProvidedAndHttpUsedAsActiveDirectoryEndpointProtocolInsteadOfHttpsThenExceptionComes()
                    throws IOException {
        when(applicationTokenCredentials.environment()).thenReturn(new AzureEnvironment(Map.of("activeDirectoryEndpointUrl", format(TEST_AD_ENDPOINT, HTTP))));

        CbDelegatedTokenCredentials underTest = new CbDelegatedTokenCredentials(applicationTokenCredentials, REDIRECT_URL);
        underTest.setAuthorizationCode(AUTHORIZATION_CODE);

        thrown.expectMessage("'authority' should use the 'https' scheme");
        thrown.expect(IllegalArgumentException.class);

        underTest.getToken(RESOURCE);
    }

    @Test
    public void testGetTokenWhenNoAuthorizationCodeProvidedThenIllegalArgumentExceptionComes() throws IOException {
        CbDelegatedTokenCredentials underTest = new CbDelegatedTokenCredentials(applicationTokenCredentials, REDIRECT_URL, Collections.emptyMap(),
                CLIENT_SECRET);
        underTest.setAuthorizationCode(AUTHORIZATION_CODE);

        thrown.expectMessage("You must provide a valid Application token credential");
        thrown.expect(IllegalArgumentException.class);

        underTest.getToken(RESOURCE);
    }

    @Test
    public void testAcquireNewAccessTokenWhenNoAuthorizationCodeProvidedThenIllegalArgumentExceptionComes() throws IOException {
        CbDelegatedTokenCredentials underTest = new CbDelegatedTokenCredentials(applicationTokenCredentials, REDIRECT_URL, Collections.emptyMap(),
                CLIENT_SECRET);
        underTest.setAuthorizationCode(AUTHORIZATION_CODE);

        thrown.expectMessage("You must provide a valid Application token credential");
        thrown.expect(IllegalArgumentException.class);

        underTest.acquireNewAccessToken(RESOURCE);
    }

    @Test
    public void testGetTokenWhenNoSecretProvidedThenAuthenticationExceptionComes() throws IOException {
        CbDelegatedTokenCredentials underTest = new CbDelegatedTokenCredentials(applicationTokenCredentials, REDIRECT_URL);
        underTest.setAuthorizationCode(AUTHORIZATION_CODE);

        thrown.expect(AuthenticationException.class);
        thrown.expectMessage("Please provide either a non-null secret.");

        underTest.getToken(RESOURCE);
    }

}