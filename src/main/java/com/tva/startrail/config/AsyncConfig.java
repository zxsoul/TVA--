package com.tva.startrail.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 开启异步支持（邮件发送 @Async，避免 SMTP 延迟拖慢接口响应）
 */
@EnableAsync
@Configuration
public class AsyncConfig {
}
