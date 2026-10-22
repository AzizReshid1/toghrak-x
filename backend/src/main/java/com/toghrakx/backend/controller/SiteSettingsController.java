package com.toghrakx.backend.controller;

import com.toghrakx.backend.dto.SiteSettingsDto;
import com.toghrakx.backend.model.SiteSettings;
import com.toghrakx.backend.service.SiteSettingsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/site-settings")
public class SiteSettingsController {

    private final SiteSettingsService service;

    public SiteSettingsController(SiteSettingsService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<SiteSettingsDto> get() {
        return ResponseEntity.ok(toDto(service.get()));
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SiteSettingsDto> update(@Valid @RequestBody SiteSettingsDto dto) {
        SiteSettings updated = service.update(dto.getTitle(), dto.getLogoUrl(), dto.getSlogan());
        return ResponseEntity.ok(toDto(updated));
    }

    private SiteSettingsDto toDto(SiteSettings settings) {
        SiteSettingsDto dto = new SiteSettingsDto();
        dto.setId(settings.getId());
        dto.setTitle(settings.getTitle());
        dto.setLogoUrl(settings.getLogoUrl());
        dto.setSlogan(settings.getSlogan());
        return dto;
    }
}
