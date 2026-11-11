package com.teamchat.realtime_chat.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Tạm tắt CSRF để dễ dàng test API ở môi trường dev
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/","/api/me","/index.html", "/login**", "/error", "/css/**", "/js/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .defaultSuccessUrl("/", true) // Đăng nhập Google xong sẽ chuyển hướng tới trang gốc (index.html)
            );
        return http.build();
    }
}