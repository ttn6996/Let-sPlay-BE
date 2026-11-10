package com.letsplay.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Component
public class LocalStorageProvider implements StorageProvider {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageProvider.class);

    private final Path uploadDirectory;
    private final String baseUrl;

    public LocalStorageProvider(
            @Value("${app.storage.local.upload-dir:./uploads/media}") String uploadDir,
            @Value("${app.storage.local.base-url:}") String baseUrl
    ) {
        this.uploadDirectory = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.baseUrl = baseUrl != null ? baseUrl.trim().replaceAll("/+$", "") : "";
        try {
            Files.createDirectories(this.uploadDirectory);
            log.info("Initialized local storage provider at: {}", this.uploadDirectory);
        } catch (IOException e) {
            log.error("Could not create local storage directory {}: {}", this.uploadDirectory, e.getMessage());
        }
    }

    @Override
    public String getProviderName() {
        return "LOCAL";
    }

    @Override
    public boolean isConfigured() {
        return true; // Local storage is always available as final resilient fallback
    }

    public Path getUploadDirectory() {
        return uploadDirectory;
    }

    @Override
    public StoredFile upload(byte[] bytes, String fileName, String contentType) throws Exception {
        String extension = "";
        if (fileName != null && fileName.contains(".")) {
            extension = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
        } else if ("image/jpeg".equalsIgnoreCase(contentType)) {
            extension = ".jpg";
        } else if ("image/png".equalsIgnoreCase(contentType)) {
            extension = ".png";
        } else if ("image/webp".equalsIgnoreCase(contentType)) {
            extension = ".webp";
        }

        String fileId = UUID.randomUUID().toString() + extension;
        Path targetPath = uploadDirectory.resolve(fileId).normalize();

        Files.write(targetPath, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        String relativePath = "/api/media/files/" + fileId;
        String publicUrl = baseUrl.isEmpty() ? relativePath : baseUrl + relativePath;

        log.info("Stored file locally: fileId={}, size={}", fileId, bytes.length);

        return new StoredFile(
                fileId,
                getProviderName(),
                publicUrl,
                publicUrl, // Local thumbnail uses the same served URL
                bytes.length,
                contentType
        );
    }

    @Override
    public boolean delete(String fileId) {
        try {
            Path targetPath = uploadDirectory.resolve(fileId).normalize();
            return Files.deleteIfExists(targetPath);
        } catch (IOException e) {
            log.warn("Failed to delete local file {}: {}", fileId, e.getMessage());
            return false;
        }
    }
}
