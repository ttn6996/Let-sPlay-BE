package com.letsplay.media;

import com.letsplay.storage.ChainedStorageService;
import com.letsplay.storage.StoredFile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class MediaService {

    private final MediaAssetRepository mediaAssets;
    private final ChainedStorageService storageService;
    private final PhotoAuthService photoAuth;

    public MediaService(MediaAssetRepository mediaAssets, ChainedStorageService storageService, PhotoAuthService photoAuth) {
        this.mediaAssets = mediaAssets;
        this.storageService = storageService;
        this.photoAuth = photoAuth;
    }

    @Transactional
    public MediaDtos.MediaAssetResponse upload(MultipartFile file, String token) {
        UserIdentity user = photoAuth.resolveIdentity(token);

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File không được để trống.");
        }

        long size = file.getSize();
        if (size > 10 * 1024 * 1024) { // 10 MiB limit
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Kích thước ảnh tối đa là 10MB.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Định dạng không được hỗ trợ. Chỉ chấp nhận hình ảnh.");
        }

        try {
            byte[] bytes = file.getBytes();
            String sha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));

            Integer width = null;
            Integer height = null;
            try {
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
                if (img != null) {
                    width = img.getWidth();
                    height = img.getHeight();
                }
            } catch (Exception ignored) {}

            String filename = file.getOriginalFilename();
            if (filename == null || filename.isBlank()) {
                filename = "image_" + UUID.randomUUID() + ".jpg";
            }

            StoredFile stored = storageService.store(bytes, filename, contentType);

            MediaAsset asset = new MediaAsset(
                    user.accountId(),
                    stored.storageProvider(),
                    stored.fileId(),
                    stored.url(),
                    stored.thumbnailUrl(),
                    contentType,
                    size,
                    width,
                    height,
                    sha256
            );

            asset = mediaAssets.save(asset);

            return new MediaDtos.MediaAssetResponse(
                    asset.getId(),
                    asset.getUrl(),
                    asset.getThumbnailUrl(),
                    asset.getStatus(),
                    asset.getByteSize(),
                    asset.getContentType(),
                    asset.getWidth(),
                    asset.getHeight(),
                    asset.getStorageProvider(),
                    asset.getCreatedAt()
            );
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể lưu trữ hình ảnh: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public MediaDtos.MediaAssetResponse getAsset(String id, String token) {
        UserIdentity user = photoAuth.resolveIdentity(token);
        MediaAsset asset = mediaAssets.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy media asset."));
        return new MediaDtos.MediaAssetResponse(
                asset.getId(),
                asset.getUrl(),
                asset.getThumbnailUrl(),
                asset.getStatus(),
                asset.getByteSize(),
                asset.getContentType(),
                asset.getWidth(),
                asset.getHeight(),
                asset.getStorageProvider(),
                asset.getCreatedAt()
        );
    }
}
