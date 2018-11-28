package com.sequenceiq.cloudbreak.api.model.kdc;

import javax.validation.constraints.Size;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.type.KerberosType;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class KdcCbManaged extends KdcBase {

    @ApiModelProperty(ModelDescriptions.StackModelDescription.KERBEROS_ADMIN)
    @Size(max = 15, min = 5, message = "The length of the Kerberos admin has to be in range of 5 to 15")
    private String admin;

    @ApiModelProperty(hidden = true)
    @Override
    public KerberosType getType() {
        return KerberosType.CB_MANAGED;
    }

    public String getAdmin() {
        return admin;
    }

    public void setAdmin(String admin) {
        this.admin = admin;
    }
}
