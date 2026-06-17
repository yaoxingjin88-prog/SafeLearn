package com.safelearn.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @Email(message = "邮箱格式不正确")
    private String email;

    @Size(max = 20, message = "手机号长度不能超过 20 位")
    private String phone;

    @Size(max = 500, message = "头像地址过长")
    private String avatarUrl;
}
