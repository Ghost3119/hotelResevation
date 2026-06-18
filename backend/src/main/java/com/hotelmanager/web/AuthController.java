package com.hotelmanager.web;

import com.hotelmanager.service.AuthService;
import com.hotelmanager.web.dto.AuthResponse;
import com.hotelmanager.web.dto.LoginRequest;
import com.hotelmanager.web.dto.RefreshResponse;
import com.hotelmanager.web.dto.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@Tag(name = "Auth")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "hotel_refresh";

    private final AuthService authService;

    @Value("${app.refresh-token.cookie-path:/api/auth}")
    private String cookiePath;

    @Value("${app.refresh-token.secure:false}")
    private boolean cookieSecure;

    @Value("${app.refresh-token.expiration-seconds:604800}")
    private long refreshExpirationSeconds;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Login and obtain an access token (sets HttpOnly refresh token cookie)", security = {})
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        AuthService.LoginResult result = authService.login(req.getEmail(), req.getPassword());
        return ResponseEntity.ok()
                .header("Set-Cookie", refreshCookie(result.refreshToken()))
                .body(result.authResponse());
    }

    @Operation(summary = "Rotate refresh token from cookie and issue a new access token (public)", security = {})
    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String cookieToken,
            @RequestBody(required = false) RefreshTokenBody body) {
        String raw = cookieToken != null ? cookieToken : (body != null ? body.refreshToken() : null);
        AuthService.RefreshResult result = authService.refresh(raw);
        return ResponseEntity.ok()
                .header("Set-Cookie", refreshCookie(result.refreshToken()))
                .body(result.refreshResponse());
    }

    @Operation(summary = "Logout: revoke refresh token and clear cookie", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String cookieToken) {
        authService.logout(cookieToken);
        return ResponseEntity.noContent()
                .header("Set-Cookie", clearRefreshCookie())
                .build();
    }

    @Operation(summary = "Current authenticated user profile", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/me")
    public ResponseEntity<UserDto> me() {
        return ResponseEntity.ok(authService.me());
    }

    private String refreshCookie(String rawToken) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, rawToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path(cookiePath)
                .maxAge(Duration.ofSeconds(refreshExpirationSeconds))
                .build();
        return cookie.toString();
    }

    private String clearRefreshCookie() {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path(cookiePath)
                .maxAge(Duration.ZERO)
                .build();
        return cookie.toString();
    }

    public record RefreshTokenBody(String refreshToken) {}
}
