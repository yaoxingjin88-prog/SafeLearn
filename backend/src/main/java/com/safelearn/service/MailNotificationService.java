package com.safelearn.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MailNotificationService {

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.mail.dev-expose-token:true}")
    private boolean devExposeToken;

    public void sendPasswordResetEmail(String email, String token) {
        String resetUrl = frontendUrl + "/reset-password?token=" + token;
        log.info("[密码重置] 邮箱: {} | 重置链接: {}", email, resetUrl);
        // 生产环境可接入 JavaMailSender / 第三方邮件服务
    }

    public boolean isDevExposeToken() {
        return devExposeToken;
    }

    public String buildResetUrl(String token) {
        return frontendUrl + "/reset-password?token=" + token;
    }
}
