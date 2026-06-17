package com.example._rdproject.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                // ==========================================
                                // 1. 기존 V1 도메인 API (인증, 유저, 캐릭터, 레벨테스트)
                                // ==========================================
                                "/api/v1/auth/**",
                                "/api/v1/users/**",
                                "/api/v1/characters/**",
                                "/api/v1/level-test/**",

                                // ==========================================
                                // 2. 채팅 및 세션 진행 관련 API
                                // ==========================================
                                "/api/chat/**",      // 메시지 전송, 채팅 내역 조회 등
                                "/api/sessions/**",  // 세션 종료 (/api/sessions/{sessionId}/end) 등

                                // ==========================================
                                // 3. 리포트, 오답노트, 분석 API (새로 분리된 도메인들)
                                // ==========================================
                                "/api/reports/**",       // 세션 리포트 조회
                                "/api/corrections/**",   // 오답노트 목록, 수정, 북마크
                                "/api/analysis/**",      // 발음 분석 등

                                // ==========================================
                                // 4. 개발/문서화 도구 및 에러 허용
                                // ==========================================
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/error"
                        ).permitAll()
                        .anyRequest().authenticated() // 위 목록에 없는 건 모두 인증(로그인) 필요!
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}