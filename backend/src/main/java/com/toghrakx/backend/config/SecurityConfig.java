package com.toghrakx.backend.config;

import com.toghrakx.backend.exception.ProblemDetailWriter;
import com.toghrakx.backend.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import java.util.Arrays;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import java.util.List;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final boolean swaggerPublic;
    private final List<String> allowedOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
            @org.springframework.beans.factory.annotation.Value("${app.security.swagger-public:false}") boolean swaggerPublic,
            @org.springframework.beans.factory.annotation.Value("${app.cors.allowed-origins:http://localhost:4200}") String allowedOriginsRaw) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.swaggerPublic = swaggerPublic;
        // Support comma-separated list from env var (e.g. "https://a.com,https://b.com")
        this.allowedOrigins = Arrays.stream(allowedOriginsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    private static final String[] SWAGGER_ENDPOINTS = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/v3/api-docs/swagger-config"
    };

    // 401/403 decided by the URL rules use the same ProblemDetail body as controller errors.
    @Bean
    AuthenticationEntryPoint restAuthenticationEntryPoint(ProblemDetailWriter problemDetailWriter) {
        return (request, response, authException) -> problemDetailWriter.write(request, response,
                HttpStatus.UNAUTHORIZED, "Authentication is required to access this resource.");
    }

    @Bean
    AccessDeniedHandler restAccessDeniedHandler(ProblemDetailWriter problemDetailWriter) {
        return (request, response, accessDeniedException) -> problemDetailWriter.write(request, response,
                HttpStatus.FORBIDDEN, "You do not have permission to access this resource.");
    }

    // JwtAuthenticationFilter is a @Component, so Spring Boot would also register it as a plain
    // servlet filter and run it a second time outside Spring Security. It belongs only in the
    // security chain (addFilterBefore below), so switch the automatic registration off.
    @Bean
    FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterServletRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationEntryPoint authenticationEntryPoint,
            AccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Disable CSRF for JWT usage
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(registry -> registry

                        // Static resources ((common locations) css, js, images, webjars, favicon, etc.)
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                        .requestMatchers("/error").permitAll()

                        // Swagger / OpenAPI
                        .requestMatchers(SWAGGER_ENDPOINTS).access((authentication, ctx) -> new AuthorizationDecision(
                                swaggerPublic || authentication.get().getAuthorities().stream()
                                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))))

                        // Access rules have two layers, each with one job:
                        // - here (URL rules): only "public" vs "must be logged in";
                        // - @PreAuthorize on each controller method: which roles may call it.
                        // A new endpoint is private by default; add a role check on the method itself.

                        // Authentication endpoints
                        .requestMatchers("/api/auth/**").permitAll()

                        // Public read-only content
                        .requestMatchers(HttpMethod.GET,
                                "/api/categories/**",
                                "/api/posts/**",
                                "/api/pages/**",
                                "/api/social-links/**",
                                "/api/site-settings",
                                "/api/search",
                                "/images/**").permitAll()

                        // Anonymous readers record a post view
                        .requestMatchers(HttpMethod.POST, "/api/posts/*/view").permitAll()

                        // Everything else requires a logged-in user
                        .anyRequest().authenticated())

                // Let CORS preflight through (optional but helpful)
                .cors(Customizer.withDefaults())
                // JWT filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var config = new CorsConfiguration();
        // Origins are driven by the app.cors.allowed-origins property (see application.properties).
        // Set CORS_ALLOWED_ORIGINS env var in production instead of changing code here.
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setAllowCredentials(true);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
