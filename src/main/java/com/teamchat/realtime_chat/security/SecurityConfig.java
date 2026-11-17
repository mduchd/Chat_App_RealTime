package com.teamchat.realtime_chat.security;

import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;
import java.util.Set;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${chat.frontend-url:/}")
    private String frontendUrl;

    @Bean
    public CookieSerializer cookieSerializer(
            @Value("${chat.session.cookie-same-site}") String sameSite,
            @Value("${chat.session.cookie-secure}") boolean secure) {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("SESSION");
        serializer.setCookiePath("/");
        serializer.setCookieMaxAge(30 * 24 * 60 * 60);
        serializer.setSameSite(sameSite);
        serializer.setUseSecureCookie(secure);
        serializer.setUseHttpOnlyCookie(true);
        return serializer;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .addFilterBefore(new OncePerRequestFilter() {
                @Override
                protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                                FilterChain chain) throws ServletException, IOException {
                    if (request.getRequestURI().startsWith("/api/")
                            && !Set.of("GET", "HEAD", "OPTIONS").contains(request.getMethod())) {
                        String origin = request.getHeader("Origin");
                        if (origin != null && !(origin.equals("https://realtime-chat-six-sand.vercel.app")
                                || origin.equals("https://chatapprealtime-production-ea7b4569.up.railway.app")
                                || origin.matches("http://(localhost|127\\.0\\.0\\.1)(:[0-9]+)?"))) {
                            response.sendError(403);
                            return;
                        }
                    }
                    chain.doFilter(request, response);
                }
            }, UsernamePasswordAuthenticationFilter.class)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable()) // Tạm tắt CSRF để dễ dàng test API ở môi trường dev
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/index.html", "/logo.svg", "/login**", "/error", "/oauth2/**", "/login/oauth2/code/**",
                        "/css/**", "/js/**", "/ws/**", "/ws-sockjs/**", "/ws-test.html",
                        "/api/me", "/api/auth/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .defaultSuccessUrl(frontendUrl, true)
            );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
                "https://realtime-chat-six-sand.vercel.app",
                "https://chatapprealtime-production-ea7b4569.up.railway.app",
                "http://localhost:[*]", "http://127.0.0.1:[*]"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
