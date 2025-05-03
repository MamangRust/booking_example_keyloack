package com.sanedge.booking_keyclock.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/test",
            "/static/**",
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/reset",
            "/api/auth/forgot",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/api-docs/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrfCustomizer -> csrfCustomizer.disable())
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated());

        http.sessionManagement(
                sessionManagementCustomizer -> sessionManagementCustomizer.sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS));

        http.oauth2ResourceServer(
                oauth2ResourceServerCustomizer -> oauth2ResourceServerCustomizer
                        .bearerTokenResolver(
                                httpServletRequest -> {
                                    var header = httpServletRequest.getHeader(
                                            HttpHeaders.AUTHORIZATION);

                                    if (header == null || header.isBlank()) {
                                        return header;
                                    }

                                    var token = header.split("Bearer ")[1].trim();

                                    return token;
                                })
                        .jwt(Customizer.withDefaults()));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfiguration() {
        var configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

}
