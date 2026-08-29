package com.toghrakx.backend.repository;

import com.toghrakx.backend.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    @EntityGraph(attributePaths = "role")
    Optional<User> findByUsername(String username);

    @EntityGraph(attributePaths = "role")
    @Query("select u from User u where u.id = :id")
    Optional<User> findWithRoleById(@Param("id") Long id);

    @EntityGraph(attributePaths = "role")
    @Query("select u from User u")
    List<User> findAllWithRole();

    Optional<User> findByEmail(String email); // Get user email

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    long countByRole_Name(String roleName);

    // Get users by role (using property path notation to access role's name)
    @EntityGraph(attributePaths = "role")
    List<User> findByRole_Name(String roleName); // Useful for admins listing editors, etc.
}
