package com.sequenceiq.cloudbreak.converter.util;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.sequenceiq.cloudbreak.api.model.kdc.KerberosRequest;
import com.sequenceiq.cloudbreak.api.model.kdc.KerberosCustomRequest;
import com.sequenceiq.cloudbreak.api.model.kdc.KerberosAdRequest;
import com.sequenceiq.cloudbreak.api.model.kdc.KerberosFreeIpaRequest;
import com.sequenceiq.cloudbreak.api.model.kdc.KerberosMitRequest;

@RunWith(Parameterized.class)
public class KerberosRequestValidatorTest {

    private static final KerberosFreeIpaRequest EXISTING_FREE_IPA = new KerberosFreeIpaRequest();

    private static final KerberosMitRequest EXISTING_MIT = new KerberosMitRequest();

    private static final KerberosAdRequest EXISTING_AD = new KerberosAdRequest();

    private static final KerberosCustomRequest CUSTOM = new KerberosCustomRequest();

    private KerberosRequestValidator underTest;

    private KerberosRequest request;

    private boolean expected;

    public KerberosRequestValidatorTest(KerberosRequest request, boolean expected) {
        this.request = request;
        this.expected = expected;
    }

    @Parameterized.Parameters(name = "[{index}] Test KerberosRequest: {0}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {null, false},
                {createRequest(EXISTING_AD, EXISTING_FREE_IPA, EXISTING_MIT, CUSTOM), false},
                {createRequest(EXISTING_AD, EXISTING_FREE_IPA, EXISTING_MIT, null), false},
                {createRequest(EXISTING_AD, null, null, CUSTOM), false},
                {createRequest(EXISTING_AD, null, EXISTING_MIT, null), false},
                {createRequest(null, EXISTING_FREE_IPA, EXISTING_MIT, CUSTOM), false},
                {createRequest(null, null, EXISTING_MIT, CUSTOM), false},
                {createRequest(EXISTING_AD, EXISTING_FREE_IPA, null, null), false},
                {createRequest(null, EXISTING_FREE_IPA, EXISTING_MIT, null), false},
                {createRequest(null, EXISTING_FREE_IPA, null, CUSTOM), false},
                {createRequest(null, null, null, null), false},
                {createRequest(EXISTING_AD, null, null, null), true},
                {createRequest(null, null, null, CUSTOM), true},
                {createRequest(null, null, EXISTING_MIT, null), true},
                {createRequest(null, EXISTING_FREE_IPA, null, null), true}
        });
    }

    @Before
    public void setUp() {
        underTest = new KerberosRequestValidator();
    }

    @Test
    public void testAgainstDifferentInputs() {
        Assert.assertEquals(expected, underTest.isKerberosRequestProperlyCreated(request));
    }

    private static KerberosRequest createRequest(KerberosAdRequest existingAd, KerberosFreeIpaRequest existingFreeIpa, KerberosMitRequest existingMit,
            KerberosCustomRequest custom) {
        KerberosRequest request = new KerberosRequest();
        request.setMit(existingMit);
        request.setFreeIpa(existingFreeIpa);
        request.setActiveDirectory(existingAd);
        request.setCustom(custom);
        return request;
    }

}