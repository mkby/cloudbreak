package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils.processTemplateIntoString;

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

    private static final String CB_AZ_APP_AUTH_URI = "/api/v3/utils/auth";

    private static final String DELIMITER = "/";

    @Value("${cb.arm.app.creation.template.path:}")
    private String appCreationTemplatePath;

    @Value("${cb.deployment.address:https://192.168.64.2}")
    private String deploymentAddress;

    @Inject
    private Configuration freemarkerConfiguration;

    public String generate() {
        try {
            Template template = freemarkerConfiguration.getTemplate(appCreationTemplatePath, "UTF-8");
            Map<String, Object> model = buildModel();
            return processTemplateIntoString(template, model);
        } catch (IOException | TemplateException e) {
            String message = String.format("Failed to process the Azure AD App creation template from path: '%s'", appCreationTemplatePath);
            throw new CloudConnectorException(message, e);
        }
    }

    private Map<String, Object> buildModel() {
        Map<String, Object> model = new HashBiMap<>();
        model.put("cloudbreakAddress", deploymentAddress);
        model.put("identifierURI", deploymentAddress.concat(DELIMITER).concat(UUID.randomUUID().toString()));
        model.put("cloudbreakReplyUrl", deploymentAddress.concat(CB_AZ_APP_AUTH_URI));
        model.put("expirationDate", getExpirationDate());
        return model;
    }

    private String getExpirationDate() {
        LocalDate date = LocalDate.now().plusYears(YEARS_OF_EXPIRATION);
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
