package com.letsplay.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChainedStorageServiceTest {

    @Mock
    private GoogleDriveStorageProvider googleDriveProvider;

    @Mock
    private CloudinaryStorageProvider cloudinaryProvider;

    @Mock
    private LocalStorageProvider localProvider;

    private ChainedStorageService chainedStorageService;

    @BeforeEach
    void setUp() {
        chainedStorageService = new ChainedStorageService(
                googleDriveProvider,
                cloudinaryProvider,
                localProvider,
                "GOOGLE_DRIVE,CLOUDINARY,LOCAL"
        );
    }

    @Test
    void whenGoogleDriveConfiguredAndSucceeds_shouldUseGoogleDrive() throws Exception {
        when(googleDriveProvider.isConfigured()).thenReturn(true);
        when(googleDriveProvider.getProviderName()).thenReturn("GOOGLE_DRIVE");
        StoredFile expected = new StoredFile("gdrive_123", "GOOGLE_DRIVE", "https://drive.google.com/view/123", "https://drive.google.com/view/123", 100, "image/jpeg");
        when(googleDriveProvider.upload(any(), anyString(), anyString())).thenReturn(expected);

        StoredFile result = chainedStorageService.store("test".getBytes(), "photo.jpg", "image/jpeg");

        assertThat(result).isNotNull();
        assertThat(result.storageProvider()).isEqualTo("GOOGLE_DRIVE");
        assertThat(result.fileId()).isEqualTo("gdrive_123");
        verify(googleDriveProvider).upload(any(), anyString(), anyString());
        verify(cloudinaryProvider, never()).upload(any(), anyString(), anyString());
        verify(localProvider, never()).upload(any(), anyString(), anyString());
    }

    @Test
    void whenGoogleDriveFails_shouldFallbackToCloudinary() throws Exception {
        when(googleDriveProvider.isConfigured()).thenReturn(true);
        when(googleDriveProvider.getProviderName()).thenReturn("GOOGLE_DRIVE");
        when(googleDriveProvider.upload(any(), anyString(), anyString())).thenThrow(new RuntimeException("Drive API quota exceeded"));

        when(cloudinaryProvider.isConfigured()).thenReturn(true);
        when(cloudinaryProvider.getProviderName()).thenReturn("CLOUDINARY");
        StoredFile expectedCloudinary = new StoredFile("cld_123", "CLOUDINARY", "https://res.cloudinary.com/photo.jpg", "https://res.cloudinary.com/thumb.jpg", 100, "image/jpeg");
        when(cloudinaryProvider.upload(any(), anyString(), anyString())).thenReturn(expectedCloudinary);

        StoredFile result = chainedStorageService.store("test".getBytes(), "photo.jpg", "image/jpeg");

        assertThat(result).isNotNull();
        assertThat(result.storageProvider()).isEqualTo("CLOUDINARY");
        verify(googleDriveProvider).upload(any(), anyString(), anyString());
        verify(cloudinaryProvider).upload(any(), anyString(), anyString());
        verify(localProvider, never()).upload(any(), anyString(), anyString());
    }

    @Test
    void whenGoogleDriveAndCloudinaryFail_shouldFallbackToLocal() throws Exception {
        when(googleDriveProvider.isConfigured()).thenReturn(false);
        when(cloudinaryProvider.isConfigured()).thenReturn(true);
        when(cloudinaryProvider.getProviderName()).thenReturn("CLOUDINARY");
        when(cloudinaryProvider.upload(any(), anyString(), anyString())).thenThrow(new RuntimeException("Cloudinary network timeout"));

        when(localProvider.isConfigured()).thenReturn(true);
        when(localProvider.getProviderName()).thenReturn("LOCAL");
        StoredFile expectedLocal = new StoredFile("loc_123", "LOCAL", "http://localhost:8082/api/media/files/photo.jpg", "http://localhost:8082/api/media/files/photo.jpg", 100, "image/jpeg");
        when(localProvider.upload(any(), anyString(), anyString())).thenReturn(expectedLocal);

        StoredFile result = chainedStorageService.store("test".getBytes(), "photo.jpg", "image/jpeg");

        assertThat(result).isNotNull();
        assertThat(result.storageProvider()).isEqualTo("LOCAL");
        verify(localProvider).upload(any(), anyString(), anyString());
    }
}
