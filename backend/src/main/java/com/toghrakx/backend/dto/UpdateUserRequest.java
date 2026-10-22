package com.toghrakx.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRequest {

    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    private String username;

    // Same rule as CreateUserRequest. Optional: null keeps the current password.
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
    private String password;

    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must be at most 255 characters")
    private String email;

    private String roleName; // // Optional: Only admins may update role, hence not mandatory for every
                             // update

    public UpdateUserRequest() {
    }

}