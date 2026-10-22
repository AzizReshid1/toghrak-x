package com.toghrakx.backend.config;

import com.toghrakx.backend.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;

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

    @Bean
    AuthenticationEntryPoint restAuthenticationEntryPoint() {
        return (request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                "Unauthorized");
    }

    @Bean
    AccessDeniedHandler restAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> response.sendError(HttpServletResponse.SC_FORBIDDEN,
                "Forbidden");
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Disable CSRF for JWT usage
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(restAuthenticationEntryPoint())
                        .accessDeniedHandler(restAccessDeniedHandler()))
                .authorizeHttpRequests(registry -> registry

                        // Static resources ((common locations) css, js, images, webjars, favicon, etc.)
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                        .requestMatchers("/error").permitAll()

                        // Swagger / OpenAPI
                        .requestMatchers(SWAGGER_ENDPOINTS).access((authentication, ctx) -> new AuthorizationDecision(
                                swaggerPublic || authentication.get().getAuthorities().stream()
                                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))))

                        // Authentication endpoints
                        .requestMatchers("/api/auth/**").permitAll()

                        // Categories — GET is public; write operations require authentication.
                        // Role enforcement (ADMIN only) is handled by @PreAuthorize in CategoryController.
                        .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/categories/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/categories/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/categories/**").authenticated()

                        // Posts
                        .requestMatchers(HttpMethod.GET, "/api/posts/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/posts/**").hasAnyRole("EDITOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/posts/**").hasAnyRole("EDITOR", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/posts/**").hasAnyRole("EDITOR", "ADMIN")

                        // Pages
                        .requestMatchers(HttpMethod.GET, "/api/pages/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/pages/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/pages/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/pages/**").hasRole("ADMIN")

                        // Social links
                        .requestMatchers(HttpMethod.GET, "/api/social-links/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/social-links/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/social-links/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/social-links/**").hasRole("ADMIN")

                        // Site settings (logo/slogan)
                        .requestMatchers(HttpMethod.GET, "/api/site-settings").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/site-settings").hasRole("ADMIN")

                        // Search
                        .requestMatchers(HttpMethod.GET, "/api/search").permitAll()

                        // User profile
                        .requestMatchers("/api/users/me").authenticated()

                        // Public images
                        .requestMatchers("/images/**").permitAll()

                        // Image upload
                        .requestMatchers(HttpMethod.POST, "/api/images/**").hasAnyRole("EDITOR", "ADMIN")

                        // User management — role enforcement (ADMIN only) is handled
                        // by @PreAuthorize in UserController; SecurityConfig ensures auth is required.
                        .requestMatchers("/api/users").authenticated()
                        // Admin dashboard — role enforcement is handled by @PreAuthorize
                        // in AdminDashboardController; SecurityConfig ensures auth is required.
                        .requestMatchers("/api/admin/**").authenticated()

                        // Anything else requires auth
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
