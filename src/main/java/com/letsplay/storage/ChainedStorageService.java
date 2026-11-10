package com.letsplay.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class ChainedStorageService {

    private static final Logger log = LoggerFactory.getLogger(ChainedStorageService.class);

    private final GoogleDriveStorageProvider googleDriveProvider;
    private final CloudinaryStorageProvider cloudinaryProvider;
    private final LocalStorageProvider localProvider;
    private final List<String> preferredOrder;

    public ChainedStorageService(
            GoogleDriveStorageProvider googleDriveProvider,
            CloudinaryStorageProvider cloudinaryProvider,
            LocalStorageProvider localProvider,
            @Value("${app.storage.order:google_drive,cloudinary,local}") String orderConfig
    ) {
        this.googleDriveProvider = googleDriveProvider;
        this.cloudinaryProvider = cloudinaryProvider;
        this.localProvider = localProvider;

        List<String> parsedOrder = new ArrayList<>();
        if (orderConfig != null && !orderConfig.isBlank()) {
            Arrays.stream(orderConfig.split(","))
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .filter(s -> !s.isEmpty())
                    .forEach(parsedOrder::add);
        }
        if (!parsedOrder.contains("LOCAL")) {
            parsedOrder.add("LOCAL"); // Ensure local is always a fallback
        }
        this.preferredOrder = List.copyOf(parsedOrder);
        log.info("Chained storage initialized with provider order: {}", this.preferredOrder);
    }

    private StorageProvider findProviderByName(String name) {
        return switch (name) {
            case "GOOGLE_DRIVE", "GDRIVE", "GOOGLE-DRIVE" -> googleDriveProvider;
            case "CLOUDINARY" -> cloudinaryProvider;
            case "LOCAL" -> localProvider;
            default -> null;
        };
    }

    /**
     * Attempts upload through the prioritized chain: Google Drive -> Cloudinary -> Local fallback.
     */
    public StoredFile store(byte[] bytes, String fileName, String contentType) {
        List<String> errors = new ArrayList<>();

        for (String providerName : preferredOrder) {
            StorageProvider provider = findProviderByName(providerName);
            if (provider == null) {
                continue;
            }

            if (!provider.isConfigured()) {
                log.debug("Storage provider {} is not configured, skipping to next provider", providerName);
                continue;
            }

            try {
                log.info("Attempting storage upload with provider: {}", provider.getProviderName());
                StoredFile stored = provider.upload(bytes, fileName, contentType);
                if (stored != null) {
                    log.info("Storage succeeded with provider: {}", provider.getProviderName());
                    return stored;
                }
            } catch (Exception ex) {
                String errorMsg = String.format("Provider %s failed: %s", provider.getProviderName(), ex.getMessage());
                log.warn("{}", errorMsg);
                errors.add(errorMsg);
            }
        }

        // Final safety net: if preferred order failed, forcefully attempt Local
        try {
            log.info("Attempting emergency local storage fallback");
            return localProvider.upload(bytes, fileName, contentType);
        } catch (Exception ex) {
            throw new RuntimeException("All storage providers failed. Errors: " + String.join("; ", errors), ex);
        }
    }

    public boolean delete(String providerName, String fileId) {
        StorageProvider provider = findProviderByName(providerName.toUpperCase());
        if (provider != null) {
            return provider.delete(fileId);
        }
        return false;
    }
}
