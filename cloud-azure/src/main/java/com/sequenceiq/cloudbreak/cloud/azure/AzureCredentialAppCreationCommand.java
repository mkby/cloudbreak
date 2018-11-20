package com.sequenceiq.cloudbreak.cloud.azure;

import static org.springframework.ui.freemarker.FreeMarkerTemplateUtils.processTemplateIntoString;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.gs.collections.impl.bimap.mutable.HashBiMap;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

@Service
public class AzureCredentialAppCreationCommand {

    private static final long YEARS_OF_EXPIRATION = 3L;

    private static final String CB_AZ_APP_REDIRECT_URI_PATTERN = "api/v3/%s/credentials/codegrantflow/authorization/azure";

    private static final String CB_AZ_APP_REPLY_URI = "api/v3/*";

    private static final String DELIMITER = "/";

    @Value("${cb.arm.app.creation.template.command.path:}")
    private String appCreationCommandTemplatePath;

    @Value("${cb.arm.app.creation.template.json.path:}")
    private String appCreationJSONTemplatePath;

    @Value("${cb.deployment.address:https://192.168.64.2}")
    private String deploymentAddress;

    @Inject
    private Configuration freemarkerConfiguration;

    public String generate() {
        try {
            Template template = freemarkerConfiguration.getTemplate(appCreationCommandTemplatePath, "UTF-8");
            Map<String, Object> model = buildModel();
            return processTemplateIntoString(template, model);
        } catch (IOException | TemplateException e) {
            String message = String.format("Failed to process the Azure AD App creation template from path: '%s'", appCreationCommandTemplatePath);
            throw new CloudConnectorException(message, e);
        }
    }

    public String generateJSON(String appSecret) {
        try {
            Template template = freemarkerConfiguration.getTemplate(appCreationJSONTemplatePath, "UTF-8");
            Map<String, Object> model = buildModel();
            model.put("appSecret", appSecret);
            model.put("keyId", UUID.randomUUID().toString());
            return processTemplateIntoString(template, model);
        } catch (IOException | TemplateException e) {
            String message = String.format("Failed to process the Azure AD App creation template from path: '%s'", appCreationJSONTemplatePath);
            throw new CloudConnectorException(message, e);
        }
    }

    public String getRedirectURL(String workspaceId) {
        String cbAzAppAuthUri = String.format(CB_AZ_APP_REDIRECT_URI_PATTERN, workspaceId);
        String replyUrl = deploymentAddress.endsWith(DELIMITER) ? deploymentAddress : deploymentAddress.concat(DELIMITER);
        return replyUrl.concat(cbAzAppAuthUri);
    }

    private Map<String, Object> buildModel() {
        Map<String, Object> model = new HashBiMap<>();
        model.put("cloudbreakAddress", deploymentAddress);
        model.put("identifierURI", deploymentAddress.concat(DELIMITER).concat(UUID.randomUUID().toString()));
        model.put("cloudbreakReplyUrl", getReplyURL());
        model.put("expirationDate", getExpirationDate());
        return model;
    }

    private String getReplyURL() {
        String replyUrl = deploymentAddress.endsWith(DELIMITER) ? deploymentAddress : deploymentAddress.concat(DELIMITER);
        return replyUrl.concat(CB_AZ_APP_REPLY_URI);
    }

    private String getExpirationDate() {
        LocalDate date = LocalDate.now().plusYears(YEARS_OF_EXPIRATION);
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
