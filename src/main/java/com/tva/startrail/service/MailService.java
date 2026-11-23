package com.tva.startrail.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 邮件服务：注册邮箱验证码发送
 * 未配置 SMTP 账号时降级为日志输出验证码，保证开发环境流程可跑通
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String username;

    @Value("${tva.mail.from:}")
    private String from;

    @Value("${tva.mail.from-name:TVA 人生星轨}")
    private String fromName;

    @Async
    public void sendEmailCode(String email, String code) {
        // 未配置 SMTP 账号（开发环境）：直接打日志，不阻断流程
        if (!StringUtils.hasText(username)) {
            log.info("【开发模式】SMTP 未配置，邮箱验证码 -> {} : {}（5 分钟内有效）", email, code);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            String fromAddr = StringUtils.hasText(from) ? from : username;
            helper.setFrom(fromAddr, fromName);
            helper.setTo(email);
            helper.setSubject("【TVA 人生星轨】注册验证码");
            helper.setText("""
                    <p>您好，</p>
                    <p>您正在注册 TVA 人生星轨账号，验证码为：</p>
                    <p style="font-size:24px;font-weight:bold;letter-spacing:4px;">%s</p>
                    <p>验证码 5 分钟内有效，请勿泄露给他人。如非本人操作，请忽略本邮件。</p>
                    """.formatted(code), true);
            mailSender.send(message);
            log.info("邮箱验证码已发送：{}", email);
        } catch (Exception e) {
            // 验证码已写入 Redis，邮件失败只记日志；用户可在冷却期后重发
            log.error("验证码邮件发送失败：{}", email, e);
        }
    }
}
