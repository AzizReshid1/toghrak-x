package com.toghrakx.backend.repository;

import com.toghrakx.backend.model.SocialLink;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialLinkRepository extends JpaRepository<SocialLink, Long> {
    java.util.List<SocialLink> findTop10ByLabelContainingIgnoreCase(String label);
}
