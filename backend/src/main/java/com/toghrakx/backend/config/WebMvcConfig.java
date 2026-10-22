package com.toghrakx.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.lang.NonNull;

import java.nio.file.Path;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final Path uploadPath;

    public WebMvcConfig(@Value("${app.image.upload-dir}") String uploadDir) {
        this.uploadPath = UploadPathResolver.resolve(uploadDir);
    }

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // Serve files under "uploads" directory when requested via /images/**
        registry.addResourceHandler("/images/**")
                .addResourceLocations(uploadPath.toUri().toString());
    }
}
