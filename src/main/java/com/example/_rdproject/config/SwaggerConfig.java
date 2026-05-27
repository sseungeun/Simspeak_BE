package com.example._rdproject.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API 명세서")
                        .description("AI 기반 언어 학습 시뮬레이션 API 문서입니다.")
                        .version("v1.0.0"));
    }
}