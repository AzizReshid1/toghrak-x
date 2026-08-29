package com.toghrakx.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class ImageService {

    private final Path fileStorageLocation;
    private final String imageBaseUrl;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".png", ".jpg", ".jpeg", ".webp", ".gif");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/png", "image/jpeg", "image/webp",
            "image/gif");

    public ImageService(@Value("${app.image.upload-dir}") String uploadDir,
            @Value("${app.image.base-url}") String imageBaseUrl) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.imageBaseUrl = normalizeBaseUrl(imageBaseUrl);
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
