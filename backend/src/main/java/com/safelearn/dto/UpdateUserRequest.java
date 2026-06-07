package com.safelearn.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UpdateUserRequest {
    private String username;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String password;
    private String role;
    private String company;
    private String department;
}
