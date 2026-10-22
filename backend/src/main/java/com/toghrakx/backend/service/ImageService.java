package com.toghrakx.backend.service;

import com.toghrakx.backend.config.UploadPathResolver;
import com.toghrakx.backend.repository.ImageReferenceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ImageService {

    private final Path fileStorageLocation;
    private final String imageBaseUrl;
    private final ImageReferenceRepository imageReferences;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".png", ".jpg", ".jpeg", ".webp", ".gif");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/png", "image/jpeg", "image/webp",
            "image/gif");
    private static final Pattern STORED_FILE_NAME = Pattern.compile(
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(png|jpg|jpeg|webp|gif)",
            Pattern.CASE_INSENSITIVE);

    public ImageService(@Value("${app.image.upload-dir}") String uploadDir,
            @Value("${app.image.base-url}") String imageBaseUrl,
            ImageReferenceRepository imageReferences) {
        this.fileStorageLocation = UploadPathResolver.resolve(uploadDir);
        this.imageBaseUrl = normalizeBaseUrl(imageBaseUrl);
        this.imageReferences = imageReferences;
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        validateFileType(file);

        // Normalize file name and remove any path traversal characters
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        if (originalFileName.contains("..")) {
            throw new IllegalArgumentException("Invalid file name");
        }

        // Create a unique file name (UUID + extension)
        String fileExtension = "";
        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex > 0) {
            fileExtension = originalFileName.substring(dotIndex);
        }
        String fileName = UUID.randomUUID().toString() + fileExtension;

        try {
            // Copy file to the target location (Replacing existing file with the same name)
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Return the public URL of the file
            return imageBaseUrl + fileName;
        } catch (IOException ex) {
            throw new IllegalStateException("Could not store file. Please try again.", ex);
        }
    }

    /**
     * Deletes the given uploaded images if nothing references them any more. Runs after the current
     * transaction commits, so a change that is rolled back never loses its images. URLs that do not
     * point at our own storage (external images) are ignored.
     */
    public void deleteIfUnreferencedAfterCommit(Collection<String> imageUrls) {
        List<String> urls = imageUrls.stream().filter(Objects::nonNull).distinct().toList();
        if (urls.isEmpty()) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    urls.forEach(ImageService.this::deleteIfUnreferenced);
                }
            });
        } else {
            urls.forEach(this::deleteIfUnreferenced);
        }
    }

    private void deleteIfUnreferenced(String url) {
        if (!url.startsWith(imageBaseUrl)) {
            return;
        }
        String fileName = url.substring(imageBaseUrl.length());
        // Only names this service generates (UUID + image extension): never follow a path elsewhere.
        if (!STORED_FILE_NAME.matcher(fileName).matches() || imageReferences.isReferenced(fileName)) {
            return;
        }
        try {
            Files.deleteIfExists(fileStorageLocation.resolve(fileName));
        } catch (IOException ex) {
            // A leftover file only wastes disk space; never fail the user's request because of it.
            log.warn("Could not delete unused image {}: {}", fileName, ex.getMessage());
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("Image base URL is not configured");
        }
        return baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }

    private void validateFileType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Unsupported file type");
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new IllegalArgumentException("File name is required");
        }
        String cleaned = StringUtils.cleanPath(originalFileName);
        int dotIndex = cleaned.lastIndexOf('.');
        if (dotIndex < 0) {
            throw new IllegalArgumentException("File extension is required");
        }
        String ext = cleaned.substring(dotIndex).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("Unsupported file extension");
        }
    }
}
