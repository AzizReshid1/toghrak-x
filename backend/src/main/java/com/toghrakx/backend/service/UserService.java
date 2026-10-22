package com.toghrakx.backend.service;

import com.toghrakx.backend.dto.UpdateUserRequest;
import com.toghrakx.backend.exception.ResourceNotFoundException;
import com.toghrakx.backend.exception.UsernameAlreadyExistsException;
import com.toghrakx.backend.exception.EmailAlreadyExistsException;
import com.toghrakx.backend.model.Role;
import com.toghrakx.backend.model.User;
import com.toghrakx.backend.repository.RoleRepository;
import com.toghrakx.backend.repository.UserRepository;
import com.toghrakx.backend.repository.PostRepository;
import com.toghrakx.backend.repository.RefreshTokenRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PostRepository postRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
            RoleRepository roleRepository, PostRepository postRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.postRepository = postRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // 1️⃣ Create user (Editor/Admin)
    public User createUser(String username, String email, String password, String roleName) {
        email = normalizeEmail(email);
        // Check for duplicate username
        if (userRepository.existsByUsername(username)) {
            throw new UsernameAlreadyExistsException("Username already exists!");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyExistsException("Email already exists!");
        }

        // Retrieve role by name or throw an exception if not found
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));

        // Build and save the new user with hashed password
        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(role)
                .build();

        return userRepository.save(user);
    }

    // 2️⃣ Find user by ID
    public User getUser(Long id) {
        return userRepository.findWithRoleById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    // 3️⃣ List all users
    public List<User> getAllUsers() {
        return userRepository.findAllWithRole();
    }

    // 4️⃣ Update user (Users can update their own profile, Admins can update any
    // user)
    public User updateUser(Long userId, Long currentUserId, boolean currentUserAdmin, UpdateUserRequest request) {
        // Fetch the existing user
        User user = getUser(userId);

        // Ensure that only the user themselves or an admin can update
        if (!currentUserId.equals(userId) && !currentUserAdmin) {
            throw new AccessDeniedException("You can only update your own account!");
        }

        // Update username if provided and different
        String newUsername = request.getUsername();
        if (newUsername != null && !newUsername.isBlank() && !newUsername.equals(user.getUsername())) {
            if (userRepository.existsByUsername(newUsername)) {
                throw new UsernameAlreadyExistsException("Username already exists!");
            }
            user.setUsername(newUsername);
        }

        // Password or role changes end the user's existing sessions (see the end of this method).
        boolean credentialsChanged = false;

        // Update password if provided (after encoding)
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            credentialsChanged = true;
        }
        // Update email if provided and check uniqueness
        String newEmail = request.getEmail() == null ? null : normalizeEmail(request.getEmail());
        if (newEmail != null && !newEmail.isBlank() && !newEmail.equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmailIgnoreCase(newEmail)) {
                throw new EmailAlreadyExistsException("Email already exists!");
            }
            user.setEmail(newEmail);
        }

        // Update role only if current user is admin
        if (currentUserAdmin) {
            if (request.getRoleName() != null && !request.getRoleName().isBlank()) {
                Role newRole = roleRepository.findByName(request.getRoleName())
                        .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + request.getRoleName()));

                // Same rule as deleteUser: demoting the last admin would leave nobody able to manage the site.
                if ("ADMIN".equals(user.getRole().getName()) && !"ADMIN".equals(newRole.getName()) &&
                        userRepository.countByRole_Name("ADMIN") <= 1) {
                    throw new IllegalStateException("Cannot change the role of the last admin account.");
                }
                if (!newRole.getName().equals(user.getRole().getName())) {
                    credentialsChanged = true;
                }
                user.setRole(newRole);
            }
        } else {
            // Non-admin users are not allowed to update their role.
            if (request.getRoleName() != null) {
                throw new AccessDeniedException("You cannot change your role.");
            }
        }

        // Revoke refresh tokens so no session (including a stolen one) can renew itself with the
        // old password or role. The current access token still works until it expires (app.jwt.expiration).
        if (credentialsChanged) {
            refreshTokenRepository.revokeAllByUserId(userId);
        }

        // updatedAt is set automatically by @PreUpdate on User — no manual assignment needed.
        return userRepository.save(user);
    }

    // 5️⃣ Delete user (Admins only, cannot delete the last admin)
    public void deleteUser(Long userId) {
        User user = getUser(userId);

        // Ensure there is at least one admin remaining.
        // This is a business-integrity rule, not an authorisation failure — use
        // IllegalStateException so the global handler returns 409 Conflict, not 403 Forbidden.
        if ("ADMIN".equals(user.getRole().getName()) &&
                userRepository.countByRole_Name("ADMIN") <= 1) {
            throw new IllegalStateException("Cannot delete the last admin account.");
        }

        // Block deletion if the user still owns posts — this is a deliberate business rule,
        // not a DB constraint violation. IllegalStateException maps to 409 Conflict via the handler.
        long n = postRepository.countByAuthorId(userId);
        if (n > 0) {
            throw new IllegalStateException(
                    "Cannot delete user: they still own " + n + " post(s). " +
                            "Delete or transfer their posts first.");
        }

        // Every login stores a refresh token pointing at the user; remove them first or the
        // foreign key from refresh_tokens blocks the delete.
        refreshTokenRepository.deleteAllByUserId(userId);
        userRepository.deleteById(userId);
    }

    // Email addresses are case-insensitive in practice, so "Aziz@x.com" and "aziz@x.com" must be one user.
    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    // Utility: Check if the current user has admin privileges
    // Authorization decisions should be made at the controller/security layer.
}
