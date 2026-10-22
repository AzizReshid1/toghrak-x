package com.toghrakx.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Key platform metrics shown on the admin dashboard. */
@Getter
@AllArgsConstructor
public class DashboardStats {
    private long totalUsers;
    private long totalAdmins;
    private long totalEditors;
    private long totalPosts;
}
