package com.toghrakx.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUserResponse {
    private Long id;
    private String username;
    private String email;
    private String roleName; // Only admins see this.
}
