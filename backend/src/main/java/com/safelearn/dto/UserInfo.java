package com.safelearn.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserInfo {
    private String id;
    private String username;
    private String email;
    private String role;
    private String company;
    private String department;
    private LocalDateTime createdAt;
}
