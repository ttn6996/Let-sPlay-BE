package com.letsplay.storage;

import java.io.InputStream;
import java.util.Optional;

public interface StorageProvider {

    /**
     * Unique identifier of this provider: GOOGLE_DRIVE, CLOUDINARY, LOCAL.
     */
    String getProviderName();

    /**
     * Check if this provider has valid configuration to attempt upload.
     */
    boolean isConfigured();

    /**
     * Upload an object to storage.
     *
     * @param bytes       File binary data
     * @param fileName    Original filename or generated name
     * @param contentType MIME type (e.g. image/jpeg, image/png)
     * @return StoredFile metadata
     * @throws Exception if upload fails
     */
    StoredFile upload(byte[] bytes, String fileName, String contentType) throws Exception;

    /**
     * Optionally delete a file from storage.
     */
    default boolean delete(String fileId) {
        return false;
    }
}
