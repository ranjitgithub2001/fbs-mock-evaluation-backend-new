package com.fbs.mock_evaluation_system.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;

import jakarta.servlet.DispatcherType;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
            UserDetailsServiceImpl userDetailsService) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.cors(cors -> cors.configurationSource(request -> {
            CorsConfiguration config = new CorsConfiguration();
            config.addAllowedOrigin("http://localhost:3000");
            config.addAllowedOrigin("https://shubhamwagh.co.in");
            config.addAllowedMethod("GET");
            config.addAllowedMethod("POST");
            config.addAllowedMethod("PUT");
            config.addAllowedMethod("PATCH");
            config.addAllowedMethod("DELETE");
            config.addAllowedMethod("OPTIONS");
            config.addAllowedHeader("*");
            config.setAllowCredentials(true);
            return config;
        }))
        .csrf(csrf -> csrf.disable())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .requestMatchers("/api/auth/**").permitAll()
            .requestMatchers(new RegexRequestMatcher("^/api/trainer-requests/?$", "POST")).permitAll()
            .requestMatchers(HttpMethod.POST, "/api/trainer-requests", "/api/trainer-requests/").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/trainer-requests", "/api/trainer-requests/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.POST, "/api/trainer-requests/*/approve", "/api/trainer-requests/*/reject").hasRole("ADMIN")
            .requestMatchers("/api/trainer-requests/**").hasRole("ADMIN")

            // ADMIN only
            .requestMatchers("/api/users/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.DELETE, "/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.POST, "/api/batches/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.POST, "/api/students/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.POST, "/api/modules/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.POST, "/api/batch-modules/**").hasRole("ADMIN")

            // ADMIN + TRAINER
            .requestMatchers(HttpMethod.POST, "/api/ai/**").hasAnyRole("ADMIN", "TRAINER")
            .requestMatchers(HttpMethod.POST, "/api/reports/**").hasAnyRole("ADMIN", "TRAINER")
            .requestMatchers(HttpMethod.GET, "/api/students/**").hasAnyRole("ADMIN", "TRAINER")
            .requestMatchers(HttpMethod.GET, "/api/batches/**").hasAnyRole("ADMIN", "TRAINER")
            .requestMatchers(HttpMethod.GET, "/api/modules/**").hasAnyRole("ADMIN", "TRAINER")
            .requestMatchers(HttpMethod.GET, "/api/batch-modules/**").hasAnyRole("ADMIN", "TRAINER")
            .requestMatchers(HttpMethod.POST, "/api/evaluations").hasAnyRole("ADMIN", "TRAINER")
            .requestMatchers(HttpMethod.PUT, "/api/evaluations/**").hasAnyRole("ADMIN", "TRAINER")
            .requestMatchers(HttpMethod.PUT, "/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.GET, "/api/evaluations/**").hasAnyRole("ADMIN", "TRAINER", "PLACEMENT")

            // ADMIN + TRAINER + PLACEMENT
            .requestMatchers(HttpMethod.GET, "/api/analytics/**").hasAnyRole("ADMIN", "TRAINER", "PLACEMENT")

            .anyRequest().authenticated()
        )
        .authenticationProvider(authenticationProvider())
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}