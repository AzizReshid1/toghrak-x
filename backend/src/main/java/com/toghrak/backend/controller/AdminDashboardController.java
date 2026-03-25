package com.toghrak.backend.controller;

import com.toghrak.backend.repository.PostRepository;
import com.toghrak.backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminDashboardController {

    private final UserRepository userRepository;
    private final PostRepository postRepository;

    public AdminDashboardController(UserRepository userRepository, PostRepository postRepository) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
    }

    /**
     * Returns a summary of key platform metrics for the admin dashboard.
     * All counts are read-only queries — no writes are performed.
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> getDashboard() {
        long totalUsers = userRepository.count();
        long totalAdmins = userRepository.countByRole_Name("ADMIN");
        long totalEditors = userRepository.countByRole_Name("EDITOR");
        long totalPosts = postRepository.count();

        return ResponseEntity.ok(Map.of(
                "totalUsers", totalUsers,
                "totalAdmins", totalAdmins,
                "totalEditors", totalEditors,
                "totalPosts", totalPosts
        ));
    }
}
