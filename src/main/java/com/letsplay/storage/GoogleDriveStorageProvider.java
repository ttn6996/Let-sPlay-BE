package com.letsplay.storage;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Permission;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Component
public class GoogleDriveStorageProvider implements StorageProvider {

    private static final Logger log = LoggerFactory.getLogger(GoogleDriveStorageProvider.class);

    private final String folderId;
    private final String credentialsJson;
    private final String credentialsFile;

    private volatile Drive driveService;

    public GoogleDriveStorageProvider(
            @Value("${app.storage.google-drive.folder-id:}") String folderId,
            @Value("${app.storage.google-drive.credentials-json:}") String credentialsJson,
            @Value("${app.storage.google-drive.credentials-file:}") String credentialsFile
    ) {
        this.folderId = folderId != null ? folderId.trim() : "";
        this.credentialsJson = credentialsJson != null ? credentialsJson.trim() : "";
        this.credentialsFile = credentialsFile != null ? credentialsFile.trim() : "";
    }

    @Override
    public String getProviderName() {
        return "GOOGLE_DRIVE";
    }

    @Override
    public boolean isConfigured() {
        return (!credentialsJson.isEmpty() || !credentialsFile.isEmpty()) && !folderId.isEmpty();
    }

    private synchronized Drive getDriveService() throws Exception {
        if (driveService != null) {
            return driveService;
        }

        InputStream credentialsStream;
        if (!credentialsJson.isEmpty()) {
            credentialsStream = new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8));
        } else if (!credentialsFile.isEmpty()) {
            credentialsStream = new FileInputStream(credentialsFile);
        } else {
            throw new IllegalStateException("Google Drive credentials not provided");
        }

        GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream)
                .createScoped(Collections.singletonList(DriveScopes.DRIVE));

        driveService = new Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials)
        ).setApplicationName("LetsPlay-API").build();

        return driveService;
    }

    @Override
    public StoredFile upload(byte[] bytes, String fileName, String contentType) throws Exception {
        Drive service = getDriveService();

        File fileMetadata = new File();
        fileMetadata.setName(fileName);
        if (!folderId.isEmpty()) {
            fileMetadata.setParents(List.of(folderId));
        }

        ByteArrayContent mediaContent = new ByteArrayContent(contentType, bytes);
        File uploadedFile = service.files().create(fileMetadata, mediaContent)
                .setFields("id, name, webViewLink, webContentLink, thumbnailLink")
                .execute();

        String fileId = uploadedFile.getId();

        // Make file public to anyone with link for viewing
        try {
            Permission permission = new Permission()
                    .setType("anyone")
                    .setRole("reader");
            service.permissions().create(fileId, permission).execute();
        } catch (Exception ex) {
            log.warn("Could not set public permission on Google Drive file {}: {}", fileId, ex.getMessage());
        }

        // Direct streamable Google User Content URL
        String directUrl = "https://lh3.googleusercontent.com/d/" + fileId;
        String thumbnailUrl = uploadedFile.getThumbnailLink() != null
                ? uploadedFile.getThumbnailLink()
                : directUrl + "=s400";

        log.info("Uploaded to Google Drive successfully: fileId={}, name={}", fileId, fileName);

        return new StoredFile(
                fileId,
                getProviderName(),
                directUrl,
                thumbnailUrl,
                bytes.length,
                contentType
        );
    }

    @Override
    public boolean delete(String fileId) {
        try {
            Drive service = getDriveService();
            service.files().delete(fileId).execute();
            return true;
        } catch (Exception e) {
            log.warn("Failed to delete file {} from Google Drive: {}", fileId, e.getMessage());
            return false;
        }
    }
}
