/**
 * Copyright 2017-2024 the original author or authors from the JHipster project.
 *
 * This file is part of the JHipster Online project, see https://github.com/jhipster/jhipster-online
 * for more information.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.jhipster.online.config;

import io.github.jhipster.online.security.AuthoritiesConstants;
import io.github.jhipster.online.security.jwt.JWTFilter;
import io.github.jhipster.online.security.jwt.TokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.filter.CorsFilter;
import org.zalando.problem.spring.web.advice.security.SecurityProblemSupport;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
@Import(SecurityProblemSupport.class)
public class SecurityConfiguration {

    private final TokenProvider tokenProvider;

    private final CorsFilter corsFilter;
    private final SecurityProblemSupport problemSupport;

    public SecurityConfiguration(TokenProvider tokenProvider, CorsFilter corsFilter, SecurityProblemSupport problemSupport) {
        this.tokenProvider = tokenProvider;
        this.corsFilter = corsFilter;
        this.problemSupport = problemSupport;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // @formatter:off
        http
            .csrf(csrf -> csrf.disable())
            .addFilterBefore(corsFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(new JWTFilter(tokenProvider), UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(problemSupport)
                .accessDeniedHandler(problemSupport)
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; frame-src 'self' data:; script-src 'self' 'unsafe-inline' 'unsafe-eval' https://www.googletagmanager.com  https://static.cloudflareinsights.com; connect-src 'self' https://www.googletagmanager.com  https://*.analytics.google.com https://analytics.google.com https://stats.g.doubleclick.net; style-src 'self' 'unsafe-inline'; img-src 'self' https://www.googletagmanager.com https://www.google.fr https://www.google.com data:; font-src 'self' https://cdn.linearicons.com data:; manifest-src 'self' https://static.cloudflareinsights.com data:;"
                ))
                .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .permissionsPolicy(permissions -> permissions.policy(
                    "geolocation=(none), midi=(none), sync-xhr=(none), microphone=(none), camera=(none), magnetometer=(none), gyroscope=(none), speaker=(none), fullscreen=(none), speaker=(none), payment=(none)"
                ))
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/").permitAll()
                .requestMatchers("/*.js").permitAll()
                .requestMatchers("/*.css").permitAll()
                .requestMatchers("/*.ico").permitAll()
                .requestMatchers("/*.png").permitAll()
                .requestMatchers("/*.svg").permitAll()
                .requestMatchers("/*.woff2").permitAll()
                .requestMatchers("/app/**").permitAll()
                .requestMatchers("/i18n/**").permitAll()
                .requestMatchers("/content/**").permitAll()
                .requestMatchers("/swagger-ui/index.html").permitAll()
                .requestMatchers("/test/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/api/authenticate").permitAll()
                .requestMatchers("/api/register").permitAll()
                .requestMatchers("/api/activate").permitAll()
                .requestMatchers("/api/account/reset-password/init").permitAll()
                .requestMatchers("/api/account/reset-password/finish").permitAll()
                .requestMatchers("/api/account/reset-password/link").hasAnyAuthority(AuthoritiesConstants.ADMIN)
                .requestMatchers("/api/crash-reports/*").permitAll()
                .requestMatchers("/api/download-application").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/kubernetes-snippets/**").permitAll()
                .requestMatchers("/api/s/link/*").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/s/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/s/**").permitAll()
                .requestMatchers("/api/git/config").permitAll()
                .requestMatchers("/api/github/callback").permitAll()
                .requestMatchers("/api/gitlab/callback").permitAll()
                .requestMatchers("/api/gitea/callback").permitAll()
                .requestMatchers("/jdl-studio/**").permitAll()
                .requestMatchers("/api/**").authenticated()
                .requestMatchers("/management/health").permitAll()
                .requestMatchers("/management/info").permitAll()
                .requestMatchers("/management/prometheus").permitAll()
                .requestMatchers("/management/**").hasAuthority(AuthoritiesConstants.ADMIN)
                .requestMatchers("/v3/api-docs/**").permitAll()
                .requestMatchers("/swagger-resources/configuration/ui").permitAll()
                .requestMatchers("/swagger-ui/index.html").hasAuthority(AuthoritiesConstants.ADMIN)
                .anyRequest().authenticated()
            )
            .httpBasic(basic -> {});
        // @formatter:on
        return http.build();
    }
}
