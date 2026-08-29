package com.toghrakx.backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "site_settings")
@Getter
@Setter
public class SiteSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 120)
    private String title;

    @Column(length = 2048)
    private String logoUrl;

    @Column(length = 200)
    private String slogan;
}
