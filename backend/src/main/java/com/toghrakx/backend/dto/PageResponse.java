package com.toghrakx.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class PageResponse {
    private String slug;
    private String title;
    private String content;
    private LocalDateTime updatedAt;
    private List<String> images;
}
