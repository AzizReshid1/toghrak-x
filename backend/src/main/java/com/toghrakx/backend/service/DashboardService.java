package com.toghrakx.backend.service;

import com.toghrakx.backend.dto.DashboardStats;
import com.toghrakx.backend.repository.PostRepository;
import com.toghrakx.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;

    public DashboardService(UserRepository userRepository, PostRepository postRepository) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
    }

    // One read-only transaction so all counts come from the same consistent snapshot.
    @Transactional(readOnly = true)
    public DashboardStats getStats() {
        return new DashboardStats(
                userRepository.count(),
                userRepository.countByRole_Name("ADMIN"),
                userRepository.countByRole_Name("EDITOR"),
                postRepository.count());
    }
}
