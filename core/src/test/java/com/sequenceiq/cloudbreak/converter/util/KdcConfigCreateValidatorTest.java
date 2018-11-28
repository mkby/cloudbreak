package com.sequenceiq.cloudbreak.converter.util;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.sequenceiq.cloudbreak.api.model.kdc.KdcConfigCreateRequest;
import com.sequenceiq.cloudbreak.api.model.kdc.KdcCustom;
import com.sequenceiq.cloudbreak.api.model.kdc.KdcExistingAd;
import com.sequenceiq.cloudbreak.api.model.kdc.KdcExistingFreeIpa;
import com.sequenceiq.cloudbreak.api.model.kdc.KdcExistingMit;

@RunWith(Parameterized.class)
public class KdcConfigCreateValidatorTest {

    private static final KdcExistingFreeIpa EXISTING_FREE_IPA = new KdcExistingFreeIpa();

    private static final KdcExistingMit EXISTING_MIT = new KdcExistingMit();

    private static final KdcExistingAd EXISTING_AD = new KdcExistingAd();

    private static final KdcCustom CUSTOM = new KdcCustom();

    private KdcConfigCreateValidator underTest;

    private KdcConfigCreateRequest request;

    private boolean expected;

    public KdcConfigCreateValidatorTest(KdcConfigCreateRequest request, boolean expected) {
        this.request = request;
        this.expected = expected;
    }

    @Parameterized.Parameters(name = "[{index}] Test KdcConfigCreateRequest: {0}")
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
        underTest = new KdcConfigCreateValidator();
    }

    @Test
    public void testAgainstDifferentInputs() {
        Assert.assertEquals(expected, underTest.isKdcConfigCreateRequestProperlyCreated(request));
    }

    private static KdcConfigCreateRequest createRequest(KdcExistingAd existingAd, KdcExistingFreeIpa existingFreeIpa, KdcExistingMit existingMit,
            KdcCustom custom) {
        KdcConfigCreateRequest request = new KdcConfigCreateRequest();
        request.setExistingMit(existingMit);
        request.setExistingFreeIpa(existingFreeIpa);
        request.setExistingAd(existingAd);
        request.setCustom(custom);
        return request;
    }

}