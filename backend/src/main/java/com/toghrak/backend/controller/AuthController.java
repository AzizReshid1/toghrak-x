package com.toghrak.backend.controller;

import com.toghrak.backend.dto.LoginRequest;
import com.toghrak.backend.dto.LoginResponse;
import com.toghrak.backend.model.User;
import com.toghrak.backend.security.JwtTokenService;
import com.toghrak.backend.security.UserPrincipal;
import com.toghrak.backend.service.RefreshTokenService;
import com.toghrak.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

        private final AuthenticationManager authenticationManager;
        private final JwtTokenService jwtTokenService;
        private final RefreshTokenService refreshTokenService;
        private final UserService userService;
        private final long refreshExpiryMs;
        private final boolean secureCookie;
        private final String cookieDomain;

        public AuthController(AuthenticationManager authenticationManager,
                        JwtTokenService jwtTokenService,
                        RefreshTokenService refreshTokenService,
                        UserService userService,
                        @Value("${app.refresh.expiration:43200000}") long refreshExpiryMs,
                        @Value("${app.refresh.cookie.secure:false}") boolean secureCookie,
                        @Value("${app.refresh.cookie.domain:}") String cookieDomain) {
                this.authenticationManager = authenticationManager;
                this.jwtTokenService = jwtTokenService;
                this.refreshTokenService = refreshTokenService;
                this.userService = userService;
                this.refreshExpiryMs = refreshExpiryMs;
                this.secureCookie = secureCookie;
                this.cookieDomain = cookieDomain;
        }

        @PostMapping("/login")
        public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
                // Create authentication token
                Authentication authentication = authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(
                                                loginRequest.getUsername(),
                                                loginRequest.getPassword()));
                // If authentication was successful, generate a JWT token
                String token = jwtTokenService.generateToken(authentication);

                // Issue refresh token cookie
                Long userId = ((UserPrincipal) authentication.getPrincipal()).getId();
                refreshTokenService.revokeAllForUser(userId);
                var refresh = refreshTokenService.create(userId);
                ResponseCookie cookie = buildRefreshCookie(refresh.getPlainToken(), refreshExpiryMs);

                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                                .body(new LoginResponse(token));
        }

        @PostMapping("/refresh")
        public ResponseEntity<LoginResponse> refresh(
                        @CookieValue(name = "refreshToken", required = false) String refreshToken) {
                if (refreshToken == null || refreshToken.isBlank()) {
                        return ResponseEntity.status(401).build();
                }
                var rotated = refreshTokenService.rotate(refreshToken);
                // Load the full user with role — needed to embed the role claim in the new access token.
                // This is one DB call per refresh (every ~30 min), not per every API request.
                User fullUser = userService.getUser(rotated.getUser().getId());
                String newAccess = jwtTokenService.generateTokenForUser(fullUser.getId(), fullUser.getRole().getName());
                ResponseCookie cookie = buildRefreshCookie(rotated.getPlainToken(), refreshExpiryMs);
                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                                .body(new LoginResponse(newAccess));
        }

        @PostMapping("/logout")
        public ResponseEntity<Void> logout(
                        @CookieValue(name = "refreshToken", required = false) String refreshToken) {
                if (refreshToken != null && !refreshToken.isBlank()) {
                        refreshTokenService.revoke(refreshToken);
                }
                ResponseCookie cleared = ResponseCookie.from("refreshToken", "")
                                .httpOnly(true)
                                .secure(secureCookie)
                                .sameSite("Lax")
                                .path("/api/auth")
                                .maxAge(0)
                                .domain(cookieDomain.isBlank() ? null : cookieDomain)
                                .build();
                return ResponseEntity.noContent()
                                .header(HttpHeaders.SET_COOKIE, cleared.toString())
                                .build();
        }

        private ResponseCookie buildRefreshCookie(String value, long ttlMs) {
                return ResponseCookie.from("refreshToken", value)
                                .httpOnly(true)
                                .secure(secureCookie)
                                .sameSite("Lax")
                                .path("/api/auth")
                                .maxAge(ttlMs / 1000)
                                .domain(cookieDomain.isBlank() ? null : cookieDomain)
                                .build();
        }
}
