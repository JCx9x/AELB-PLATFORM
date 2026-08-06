package es.aelb.backendweb.infrastructure.web.controller;

import es.aelb.backendweb.application.auth.IssueRefreshTokenCommand;
import es.aelb.backendweb.application.auth.IssueRefreshTokenUseCase;
import es.aelb.backendweb.application.auth.IssuedRefreshToken;
import es.aelb.backendweb.application.auth.LogoutCommand;
import es.aelb.backendweb.application.auth.LogoutUseCase;
import es.aelb.backendweb.application.auth.RefreshSessionCommand;
import es.aelb.backendweb.application.auth.RefreshSessionResult;
import es.aelb.backendweb.application.auth.RefreshSessionUseCase;
import es.aelb.backendweb.application.user.LoginCommand;
import es.aelb.backendweb.application.user.LoginResult;
import es.aelb.backendweb.application.user.LoginUseCase;
import es.aelb.backendweb.infrastructure.security.JwtTokenProvider;
import es.aelb.backendweb.infrastructure.security.AuthCookieProperties;
import es.aelb.backendweb.infrastructure.security.InMemoryRateLimiter;
import es.aelb.backendweb.infrastructure.web.dto.request.LoginRequest;
import es.aelb.backendweb.infrastructure.web.dto.response.AuthResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

/**
 * Controlador de autenticación.
 * El access token (corta duración, claim "ver" comprobado contra BD en cada
 * petición) y el refresh token (opaco y rotatorio, revocado en cada uso)
 * viajan únicamente en cookies HttpOnly — nunca en el cuerpo de la
 * respuesta ni accesibles desde JavaScript del navegador.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final LoginUseCase             loginUseCase;
    private final IssueRefreshTokenUseCase issueRefreshTokenUseCase;
    private final RefreshSessionUseCase    refreshSessionUseCase;
    private final LogoutUseCase            logoutUseCase;
    private final JwtTokenProvider         jwtTokenProvider;
    private final AuthCookieProperties     authCookieProperties;
    private final InMemoryRateLimiter      rateLimiter;

    private static final int    LOGIN_ATTEMPTS_PER_ACCOUNT = 5;
    private static final Duration LOGIN_WINDOW             = Duration.ofMinutes(1);

    public AuthController(
            LoginUseCase loginUseCase,
            IssueRefreshTokenUseCase issueRefreshTokenUseCase,
            RefreshSessionUseCase refreshSessionUseCase,
            LogoutUseCase logoutUseCase,
            JwtTokenProvider jwtTokenProvider,
            AuthCookieProperties authCookieProperties,
            InMemoryRateLimiter rateLimiter
    ) {
        this.loginUseCase             = loginUseCase;
        this.issueRefreshTokenUseCase = issueRefreshTokenUseCase;
        this.refreshSessionUseCase    = refreshSessionUseCase;
        this.logoutUseCase            = logoutUseCase;
        this.jwtTokenProvider         = jwtTokenProvider;
        this.authCookieProperties     = authCookieProperties;
        this.rateLimiter              = rateLimiter;
    }

    /** Initializes the double-submit CSRF token before an unsafe browser request. */
    @GetMapping("/csrf")
    public ResponseEntity<Map<String, String>> csrf(CsrfToken csrfToken) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(Map.of("token", csrfToken.getToken()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String normalizedEmail = request.email() == null ? "" : request.email().trim().toLowerCase(Locale.ROOT);
        String rateLimitKey = "login:" + httpRequest.getRemoteAddr() + ":" + normalizedEmail;
        if (!rateLimiter.tryConsume(rateLimitKey, LOGIN_ATTEMPTS_PER_ACCOUNT, LOGIN_WINDOW)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .build();
        }

        LoginResult result = loginUseCase.execute(
                new LoginCommand(request.email(), request.password())
        );

        String token = jwtTokenProvider.generateToken(
                result.userId(),
                result.email(),
                result.role(),
                result.tokenVersion()
        );
        IssuedRefreshToken refreshToken = issueRefreshTokenUseCase.execute(
                new IssueRefreshTokenCommand(result.userId())
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authenticationCookie(token).toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie(refreshToken.rawToken(), refreshToken.expiresAt()).toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(new AuthResponse(
                result.userId(),
                result.email(),
                result.role(),
                result.firstName(),
                result.lastName()
        ));
    }

    /** Rota la sesión de refresco y acuña un access token fresco. La llama el frontend cuando el access token caduca. */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request) {
        String rawRefreshToken = readCookie(request, authCookieProperties.getRefreshName());
        if (rawRefreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .build();
        }

        RefreshSessionResult result = refreshSessionUseCase.execute(new RefreshSessionCommand(rawRefreshToken));

        String accessToken = jwtTokenProvider.generateToken(
                result.userId(), result.email(), result.role(), result.tokenVersion()
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authenticationCookie(accessToken).toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie(result.newRawRefreshToken(), result.newRefreshExpiresAt()).toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(new AuthResponse(
                        result.userId(), result.email(), result.role(),
                        result.firstName(), result.lastName()
                ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String rawRefreshToken = readCookie(request, authCookieProperties.getRefreshName());
        logoutUseCase.execute(new LogoutCommand(rawRefreshToken));

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expiredAuthenticationCookie().toString())
                .header(HttpHeaders.SET_COOKIE, expiredRefreshCookie().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .build();
    }

    private static String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        return Arrays.stream(cookies)
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private ResponseCookie authenticationCookie(String token) {
        return ResponseCookie.from(authCookieProperties.getName(), token)
                .httpOnly(true)
                .secure(authCookieProperties.isSecure())
                .sameSite(authCookieProperties.getSameSite())
                .path("/api")
                .maxAge(Duration.ofMillis(jwtTokenProvider.getExpirationMs()))
                .build();
    }

    private ResponseCookie refreshCookie(String rawToken, LocalDateTime expiresAt) {
        Duration maxAge = Duration.between(LocalDateTime.now(), expiresAt);
        return ResponseCookie.from(authCookieProperties.getRefreshName(), rawToken)
                .httpOnly(true)
                .secure(authCookieProperties.isSecure())
                .sameSite(authCookieProperties.getSameSite())
                .path("/api/auth")
                .maxAge(maxAge.isNegative() ? Duration.ZERO : maxAge)
                .build();
    }

    private ResponseCookie expiredAuthenticationCookie() {
        return ResponseCookie.from(authCookieProperties.getName(), "")
                .httpOnly(true)
                .secure(authCookieProperties.isSecure())
                .sameSite(authCookieProperties.getSameSite())
                .path("/api")
                .maxAge(Duration.ZERO)
                .build();
    }

    private ResponseCookie expiredRefreshCookie() {
        return ResponseCookie.from(authCookieProperties.getRefreshName(), "")
                .httpOnly(true)
                .secure(authCookieProperties.isSecure())
                .sameSite(authCookieProperties.getSameSite())
                .path("/api/auth")
                .maxAge(Duration.ZERO)
                .build();
    }
}
