package com.tva.startrail.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / OpenAPI 文档配置，启动后访问 http://localhost:8080/doc.html
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI tvaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TVA 人生星轨 接口文档")
                        .description("本期范围：基础登录（JWT + Redis 双 Token + 邮箱验证码注册）；事件 / 时间节点模块后续迭代")
                        .version("V1.0")
                        .contact(new Contact().name("zxsoul")));
    }
}
