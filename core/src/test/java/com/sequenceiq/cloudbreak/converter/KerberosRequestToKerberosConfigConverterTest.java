package com.sequenceiq.cloudbreak.converter;

import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.api.model.kdc.KerberosAdRequest;
import com.sequenceiq.cloudbreak.api.model.kdc.KerberosCustomRequest;
import com.sequenceiq.cloudbreak.api.model.kdc.KerberosFreeIpaRequest;
import com.sequenceiq.cloudbreak.api.model.kdc.KerberosMitRequest;
import com.sequenceiq.cloudbreak.api.model.kdc.KerberosRequest;
import com.sequenceiq.cloudbreak.api.model.kdc.KerberosRequestTypeBase;
import com.sequenceiq.cloudbreak.converter.v2.KerberosRequestToKerberosConfigConverter;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.service.kdc.KerberosTypeResolver;

@RunWith(Parameterized.class)
public class KerberosRequestToKerberosConfigConverterTest extends AbstractConverterTest {

    @Mock
    private KerberosTypeResolver kerberosTypeResolver;

    @Mock
    private ConversionService conversionService;

    @InjectMocks
    private KerberosRequestToKerberosConfigConverter underTest;

    private KerberosData testData;

    public KerberosRequestToKerberosConfigConverterTest(KerberosData testData) {
        this.testData = testData;
    }

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Parameterized.Parameters()
    public static Object[] data() {
        return new Object[]{
                KerberosData.FREEIPA,
                KerberosData.CUSTOM,
                KerberosData.ACTIVE_DIRECTORY,
                KerberosData.MIT,
        };
    }

    @Test
    public void testConverterWhenKerberosTypeResolverReturnsASpecificKerberosTypeThenThatShouldBeConvertIntoAKerberosConfig() {
        KerberosRequest request = testData.getRequest();
        KerberosRequestTypeBase actualType = testData.getActualType();
        KerberosConfig expected = testData.getExpected();
        when(kerberosTypeResolver.propagateKerberosConfiguration(request)).thenReturn(actualType);
        when(conversionService.convert(actualType, KerberosConfig.class)).thenReturn(expected);

        KerberosConfig result = underTest.convert(request);

        Assert.assertNotNull(result);
        Assert.assertEquals(expected, result);
    }

    private enum KerberosData {

        FREEIPA {
            @Override
            public KerberosRequest getRequest() {
                KerberosRequest request = new KerberosRequest();
                request.setFreeIpa(new KerberosFreeIpaRequest());
                return request;
            }

            @Override
            public KerberosConfig getExpected() {
                return new KerberosConfig();
            }

            @Override
            public KerberosRequestTypeBase getActualType() {
                return new KerberosFreeIpaRequest();
            }
        },

        MIT {
            @Override
            public KerberosRequest getRequest() {
                KerberosRequest request = new KerberosRequest();
                request.setMit(new KerberosMitRequest());
                return request;
            }

            @Override
            public KerberosConfig getExpected() {
                return new KerberosConfig();
            }

            @Override
            public KerberosRequestTypeBase getActualType() {
                return new KerberosMitRequest();
            }
        },

        ACTIVE_DIRECTORY {
            @Override
            public KerberosRequest getRequest() {
                KerberosRequest request = new KerberosRequest();
                request.setActiveDirectory(new KerberosAdRequest());
                return request;
            }

            @Override
            public KerberosConfig getExpected() {
                return new KerberosConfig();
            }

            @Override
            public KerberosRequestTypeBase getActualType() {
                return new KerberosAdRequest();
            }
        },

        CUSTOM {
            @Override
            public KerberosRequest getRequest() {
                KerberosRequest request = new KerberosRequest();
                request.setCustom(new KerberosCustomRequest());
                return request;
            }

            @Override
            public KerberosConfig getExpected() {
                return new KerberosConfig();
            }

            @Override
            public KerberosRequestTypeBase getActualType() {
                return new KerberosCustomRequest();
            }
        };

        public abstract KerberosRequest getRequest();

        public abstract KerberosConfig getExpected();

        public abstract KerberosRequestTypeBase getActualType();

    }

}