package com.sequenceiq.cloudbreak.api.model.kdc;

import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription.DESCRIPTOR;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription.KERBEROS_PRINCIPAL;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription.KRB_5_CONF;

import com.sequenceiq.cloudbreak.type.KerberosType;
import com.sequenceiq.cloudbreak.validation.ValidJson;
import com.sequenceiq.cloudbreak.validation.ValidKerberosDescriptor;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class KerberosCustomRequest extends KerberosRequestTypeBase {

    @ApiModelProperty(value = KERBEROS_PRINCIPAL, required = true)
    private String principal;

    @ValidKerberosDescriptor
    @ApiModelProperty(value = DESCRIPTOR, required = true)
    private String descriptor;

    @ValidJson(message = "The krb5 configuration must be a valid JSON")
    @ApiModelProperty(value = KRB_5_CONF, required = true)
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
