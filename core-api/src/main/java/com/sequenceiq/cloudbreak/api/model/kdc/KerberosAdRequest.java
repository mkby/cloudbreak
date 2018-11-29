package com.sequenceiq.cloudbreak.api.model.kdc;

import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.type.KerberosType;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class KerberosAdRequest extends KerberosRequestTypeBase {

    @ApiModelProperty(ModelDescriptions.StackModelDescription.KERBEROS_KDC_URL)
    private String url;

    @ApiModelProperty(ModelDescriptions.StackModelDescription.KERBEROS_ADMIN_URL)
    private String adminUrl;

    @ApiModelProperty
    private String realm;

    @ApiModelProperty
    private String ldapUrl;

    @ApiModelProperty
    private String containerDn;

    @ApiModelProperty(ModelDescriptions.StackModelDescription.KERBEROS_PRINCIPAL)
    private String principal;

    @ApiModelProperty(hidden = true)
    @Override
    public KerberosType getType() {
        return KerberosType.ACTIVE_DIRECTORY;
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

    public String getLdapUrl() {
        return ldapUrl;
    }

    public void setLdapUrl(String ldapUrl) {
        this.ldapUrl = ldapUrl;
    }

    public String getContainerDn() {
        return containerDn;
    }

    public void setContainerDn(String containerDn) {
        this.containerDn = containerDn;
    }

    public String getPrincipal() {
        return principal;
    }

    public void setPrincipal(String principal) {
        this.principal = principal;
    }
}
