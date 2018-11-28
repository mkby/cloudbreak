package com.sequenceiq.cloudbreak.api.model.kdc;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.api.model.JsonEntity;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.type.KerberosType;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true, value = "type")
public abstract class KdcBase implements JsonEntity {

    @ApiModelProperty
    @NotNull
    private String name;

    @ApiModelProperty(ModelDescriptions.StackModelDescription.KERBEROS_KDC_VERIFY_KDC_TRUST)
    private Boolean verifyKdcTrust = true;

    @ApiModelProperty(ModelDescriptions.StackModelDescription.KERBEROS_DOMAIN)
    private String domain;

    @Pattern(regexp = "(^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(,((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}"
            + "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?))*$)")
    @ApiModelProperty(ModelDescriptions.StackModelDescription.KERBEROS_NAMESERVERS)
    private String nameServers;

    @ApiModelProperty(ModelDescriptions.StackModelDescription.KERBEROS_PASSWORD)
    @Size(max = 50, min = 5, message = "The length of the Kerberos password has to be in range of 5 to 50")
    private String password;

    private Boolean tcpAllowed = false;

    abstract KerberosType getType();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getVerifyKdcTrust() {
        return verifyKdcTrust;
    }

    public void setVerifyKdcTrust(Boolean verifyKdcTrust) {
        this.verifyKdcTrust = verifyKdcTrust;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getNameServers() {
        return nameServers;
    }

    public void setNameServers(String nameServers) {
        this.nameServers = nameServers;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean getTcpAllowed() {
        return tcpAllowed;
    }

    public void setTcpAllowed(Boolean tcpAllowed) {
        this.tcpAllowed = tcpAllowed;
    }
}
