package com.toghrakx.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchResultItem {
    private String type; // POST, PAGE, SOCIAL
    private String title;
    private String url;
    private String snippet;
}
