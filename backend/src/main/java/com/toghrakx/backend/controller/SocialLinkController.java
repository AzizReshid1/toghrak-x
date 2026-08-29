package com.toghrakx.backend.controller;

import com.toghrakx.backend.dto.SocialLinkDto;
import com.toghrakx.backend.model.SocialLink;
import com.toghrakx.backend.service.SocialLinkService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/social-links")
public class SocialLinkController {

    private final SocialLinkService service;

    public SocialLinkController(SocialLinkService service) {
        this.service = service;
    }

    @GetMapping
    public List<SocialLinkDto> list() {
        return service.list().stream().map(this::toDto).toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SocialLinkDto> create(@Valid @RequestBody SocialLinkDto dto) {
        SocialLink created = service.create(dto.getLabel(), dto.getUrl(), dto.getIcon());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SocialLinkDto> update(@PathVariable Long id, @Valid @RequestBody SocialLinkDto dto) {
        SocialLink updated = service.update(id, dto.getLabel(), dto.getUrl(), dto.getIcon());
        return ResponseEntity.ok(toDto(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    private SocialLinkDto toDto(SocialLink link) {
        SocialLinkDto dto = new SocialLinkDto();
        dto.setId(link.getId());
        dto.setLabel(link.getLabel());
        dto.setUrl(link.getUrl());
        dto.setIcon(link.getIcon());
        return dto;
    }
}
