package com.toghrakx.backend.controller;

import com.toghrakx.backend.dto.ImageUploadResponse;
import com.toghrakx.backend.service.ImageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    // This endpoint allows editors and admins to upload an image file.
    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('EDITOR', 'ADMIN')")
    public ResponseEntity<ImageUploadResponse> uploadImage(@RequestParam("file") MultipartFile file) {
        String imageUrl = imageService.storeFile(file);
        ImageUploadResponse response = new ImageUploadResponse(imageUrl);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
