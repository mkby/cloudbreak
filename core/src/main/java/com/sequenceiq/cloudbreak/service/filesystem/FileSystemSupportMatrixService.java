package com.sequenceiq.cloudbreak.service.filesystem;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.filesystem.CloudStorageSupportedResponse;
import com.sequenceiq.cloudbreak.cloud.VersionComparator;
import com.sequenceiq.cloudbreak.cloud.model.Versioned;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;
import com.sequenceiq.cloudbreak.service.filesystem.resource.definition.CloudFileSystemSupportConfigEntry;
import com.sequenceiq.cloudbreak.service.filesystem.resource.definition.CloudFileSystemSupportMatrix;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Service
public class FileSystemSupportMatrixService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemSupportMatrixService.class);

    private CloudFileSystemSupportMatrix cloudFileSystemSupportMatrix;

    @Inject
    private CloudbreakResourceReaderService cloudbreakResourceReaderService;

    @PostConstruct
    public void init() {
        String configDefinitions = cloudbreakResourceReaderService.resourceDefinition("cloud-storage-support-matrix");
        try {
            cloudFileSystemSupportMatrix = JsonUtil.readValue(configDefinitions, CloudFileSystemSupportMatrix.class);
        } catch (IOException e) {
            LOGGER.warn("Cannot initialize cloudFileSystemSupportMatrix", e);
            cloudFileSystemSupportMatrix = new CloudFileSystemSupportMatrix();
        }
    }

    public Set<CloudStorageSupportedResponse> getCloudStorageMatrix(String stackVersion) {
        VersionComparator versionComparator = new VersionComparator();
        Set<CloudStorageSupportedResponse> response = new HashSet<>();
        cloudFileSystemSupportMatrix.getProviders().forEach(supportConfigEntries -> {
            Set<String> supportedFileSystems = supportConfigEntries.getConfigEntries().stream()
                    .filter(supportConfigEntry -> {
                        Versioned minVersion = supportConfigEntry::getMinVersion;
                        Versioned currentVersion = () -> stackVersion.length() > supportConfigEntry.getMinVersion().length()
                                ? stackVersion.substring(0, supportConfigEntry.getMinVersion().length()) : stackVersion;
                        int compared = versionComparator.compare(minVersion, currentVersion);
                        return compared <= 0;
                    })
                    .map(CloudFileSystemSupportConfigEntry::getSupportedFileSytem)
                    .collect(Collectors.toSet());
            if (!supportedFileSystems.isEmpty()) {
                CloudStorageSupportedResponse cloudStorageSupportedResponse = new CloudStorageSupportedResponse();
                cloudStorageSupportedResponse.setProvider(supportConfigEntries.getProvider());
                cloudStorageSupportedResponse.setFileSystemType(supportedFileSystems);
                response.add(cloudStorageSupportedResponse);
            }
        });
        return response;
    }
}
