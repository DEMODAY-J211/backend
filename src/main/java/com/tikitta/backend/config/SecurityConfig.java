package com.tikitta.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final OAuth2UserCustomService oAuth2UserCustomService;
    private final CustomOAuth2SuccessHandler customOAuth2SuccessHandler;
    private final RedirectUriSaveFilter redirectUriSaveFilter;
    private final OriginSaveFilter originSaveFilter; // 새로 추가
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .addFilterBefore(new ManagerIdSaveFilter(), SecurityContextPersistenceFilter.class)
                .addFilterBefore(redirectUriSaveFilter, OAuth2AuthorizationRequestRedirectFilter.class)
                .addFilterAfter(originSaveFilter, RedirectUriSaveFilter.class) // RedirectUriSaveFilter 다음에 실행되도록 추가
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/",
                                "/user/*/main",
                                "/user/*/detail/**",
                                "/user/*/organization",
                                "/auth/**",
                                "/api/test/**"
                        ).permitAll()
                        .requestMatchers("/manager/**").hasRole("MANAGER")
                        .requestMatchers("/user/*/myshow", "/user/*/booking/**", "/user/*/ticket/**").hasRole("USER")
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth
                        .userInfoEndpoint(userInfo -> userInfo.userService(oAuth2UserCustomService))
                        .successHandler(customOAuth2SuccessHandler)
                        .failureUrl("/login?error=true")
                );

        return http.build();
    }
}