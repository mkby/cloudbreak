package com.sequenceiq.cloudbreak.api.model.kdc;

import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription.KERBEROS_ADMIN_URL;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription.KERBEROS_KDC_URL;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription.KERBEROS_PRINCIPAL;
import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.StackModelDescription.KERBEROS_REALM;

import com.sequenceiq.cloudbreak.type.KerberosType;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class KerberosMitRequest extends KerberosRequestTypeBase {

    @ApiModelProperty(value = KERBEROS_KDC_URL, required = true)
    private String url;

    @ApiModelProperty(value = KERBEROS_ADMIN_URL, required = true)
    private String adminUrl;

    @ApiModelProperty(value = KERBEROS_REALM, required = true)
    private String realm;

    @ApiModelProperty(value = KERBEROS_PRINCIPAL, required = true)
    private String principal;

    @ApiModelProperty(hidden = true)
    @Override
    public KerberosType getType() {
        return KerberosType.MIT;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAdminUrl() {
        return adminUrl;
    }

    public void setAdminUrl(String adminUrl) {
        this.adminUrl = adminUrl;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getPrincipal() {
        return principal;
    }

    public void setPrincipal(String principal) {
        this.principal = principal;
    }
}
