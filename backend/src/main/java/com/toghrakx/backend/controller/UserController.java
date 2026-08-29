package com.toghrakx.backend.controller;

import com.toghrakx.backend.dto.CreateUserRequest;
import com.toghrakx.backend.dto.UpdateUserRequest;
import com.toghrakx.backend.dto.AdminUserResponse;
import com.toghrakx.backend.dto.UserProfileResponse;
import com.toghrakx.backend.model.User;
import com.toghrakx.backend.security.UserPrincipal;
import com.toghrakx.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // 1️⃣ Admin can create a new user (Editor/Admin)
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminUserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        User newUser = userService.createUser(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                request.getRoleName());
        // Only admins can create users, so we return the admin DTO.
        return ResponseEntity.status(HttpStatus.CREATED).body(toAdminUserResponse(newUser));
    }

    // 2️⃣ Get all users (Admins only)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<AdminUserResponse> getAllUsers() {
        return userService.getAllUsers().stream()
                .map(this::toAdminUserResponse)
                .toList();
    }

    // 3️⃣ Get user profile (Only authenticated user can see their own info)
    @GetMapping("/me")
    public ResponseEntity<Object> getCurrentUser(@AuthenticationPrincipal UserPrincipal currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "unauthorized"));
        }
        User user = userService.getUser(currentUser.getId());
        if (isAdmin(currentUser)) {
            return ResponseEntity.ok(toAdminUserResponse(user));
        } else {
            return ResponseEntity.ok(toUserProfileResponse(user));
        }
    }

    // 4️⃣ Admins can get any user by ID
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminUserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(toAdminUserResponse(userService.getUser(id)));
    }

    // 5️⃣ Update user (Users can update their own profile, Admins can update any
    // user)
    @PutMapping("/{id}")
    public ResponseEntity<Object> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        User updatedUser = userService.updateUser(id, currentUser.getId(), isAdmin(currentUser), request);
        if (isAdmin(currentUser)) {
            return ResponseEntity.ok(toAdminUserResponse(updatedUser));
        } else {
            return ResponseEntity.ok(toUserProfileResponse(updatedUser));
        }
    }

    // 6️⃣ Delete user (Admins can delete users, but cannot delete the last admin)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // --- Helper Methods for response mapping ---

    private boolean isAdmin(UserPrincipal currentUser) {
        return currentUser.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
    }

    private AdminUserResponse toAdminUserResponse(User user) {
        AdminUserResponse response = new AdminUserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        // Include role info for admin users
        response.setRoleName(user.getRole().getName());
        return response;
    }

    private UserProfileResponse toUserProfileResponse(User user) {
        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRoleName(user.getRole() != null ? user.getRole().getName() : null);
        return response;
    }
}
