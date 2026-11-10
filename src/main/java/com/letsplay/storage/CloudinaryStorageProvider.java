package com.letsplay.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CloudinaryStorageProvider implements StorageProvider {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryStorageProvider.class);

    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;

    private volatile Cloudinary cloudinary;

    public CloudinaryStorageProvider(
            @Value("${app.storage.cloudinary.cloud-name:}") String cloudName,
            @Value("${app.storage.cloudinary.api-key:}") String apiKey,
            @Value("${app.storage.cloudinary.api-secret:}") String apiSecret
    ) {
        this.cloudName = cloudName != null ? cloudName.trim() : "";
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.apiSecret = apiSecret != null ? apiSecret.trim() : "";
    }

    @Override
    public String getProviderName() {
        return "CLOUDINARY";
    }

    @Override
    public boolean isConfigured() {
        return !cloudName.isEmpty() && !apiKey.isEmpty() && !apiSecret.isEmpty();
    }

    private synchronized Cloudinary getCloudinary() {
        if (cloudinary != null) {
            return cloudinary;
        }
        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
        return cloudinary;
    }

    @Override
    public StoredFile upload(byte[] bytes, String fileName, String contentType) throws Exception {
        Cloudinary client = getCloudinary();

        Map<?, ?> uploadParams = ObjectUtils.asMap(
                "folder", "letsplay/moments",
                "resource_type", "auto",
                "use_filename", true,
                "unique_filename", true
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) client.uploader().upload(bytes, uploadParams);

        String publicId = (String) result.get("public_id");
        String secureUrl = (String) result.get("secure_url");

        // Cloudinary dynamic thumbnail URL
        String thumbnailUrl = secureUrl;
        if (secureUrl != null && secureUrl.contains("/upload/")) {
            thumbnailUrl = secureUrl.replace("/upload/", "/upload/c_fill,w_400,h_400,q_auto,f_auto/");
        }

        log.info("Uploaded to Cloudinary successfully: publicId={}, secureUrl={}", publicId, secureUrl);

        return new StoredFile(
                publicId,
                getProviderName(),
                secureUrl,
                thumbnailUrl,
                bytes.length,
                contentType
        );
    }

    @Override
    public boolean delete(String fileId) {
        try {
            Cloudinary client = getCloudinary();
            client.uploader().destroy(fileId, ObjectUtils.emptyMap());
            return true;
        } catch (Exception e) {
            log.warn("Failed to delete file {} from Cloudinary: {}", fileId, e.getMessage());
            return false;
        }
    }
}
