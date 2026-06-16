package com.example._rdproject.config;

import lombok.Builder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    @Bean
    public WebClient aiServerWebClient() {
        return WebClient.builder()
                .baseUrl("https://simspeak-production.up.railway.app")
                .build();
    }
}
