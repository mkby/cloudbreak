package com.sequenceiq.cloudbreak.api.model.kdc;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.type.KerberosType;
import com.sequenceiq.cloudbreak.validation.ValidJson;
import com.sequenceiq.cloudbreak.validation.ValidKerberosDescriptor;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class KdcCustom extends KdcBase {

    @ApiModelProperty(ModelDescriptions.StackModelDescription.KERBEROS_PRINCIPAL)
    private String principal;

    @ValidKerberosDescriptor
    @ApiModelProperty(ModelDescriptions.StackModelDescription.DESCRIPTOR)
    private String descriptor;

    @ValidJson(message = "The krb5 configuration must be a valid JSON")
    @ApiModelProperty(ModelDescriptions.StackModelDescription.KRB_5_CONF)
    private String krb5Conf;

    @ApiModelProperty(hidden = true)
    @Override
    public KerberosType getType() {
        return KerberosType.CUSTOM;
    }

    public String getPrincipal() {
        return principal;
    }

    public void setPrincipal(String principal) {
        this.principal = principal;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(String descriptor) {
        this.descriptor = descriptor;
    }

    public String getKrb5Conf() {
        return krb5Conf;
    }

    public void setKrb5Conf(String krb5Conf) {
        this.krb5Conf = krb5Conf;
    }
}
