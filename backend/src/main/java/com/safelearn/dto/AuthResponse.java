package com.safelearn.dto;

import lombok.Data;

@Data
public class AuthResponse {
    private String token;
    private UserInfo user;
}
