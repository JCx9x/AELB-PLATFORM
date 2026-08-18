package es.aelb.backendweb.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Arrays;
import java.util.List;

/**
 * Configuración de Spring Security — Modelo de acceso:
 *
 * PÚBLICO (sin login)
 *   - Home, campeonatos visibles, noticias publicadas, resultados publicados
 *   - Cálculo de precio (para ver el precio antes de loguearse)
 *   - Registro de nuevo usuario y login
 *   - Webhook de pago (llamado por el proveedor externo, sin JWT)
 *
 * USUARIO AUTENTICADO
 *   - Inscribirse en un campeonato
 *   - Cancelar propia inscripción
 *   - Ver propias inscripciones
 *   - Editar perfil y cambiar contraseña
 *   - Crear checkout de pago
 *
 * GESTOR / ADMIN
 *   - Gestión de campeonatos, categorías, noticias, resultados
 *   - Ver todas las inscripciones de un campeonato
 *   - Marcar pagos
 *   - Endpoints /all (contenido no publicado/oculto)
 *   - Configuración de precios
 *
 * IMPORTANTE: las reglas más específicas deben ir ANTES que los patrones generales (/**)
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(AuthCookieProperties.class)
public class SecurityConfig {

    @Value("${aelb.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthenticationFilter jwtFilter,
                                           RateLimitFilter rateLimitFilter,
                                           CookieCsrfTokenRepository csrfTokenRepository) throws Exception {
        return http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        // El handler XOR por defecto (protección BREACH) enmascara el token de
                        // forma distinta en cada acceso a CsrfToken#getToken(); pensado para
                        // tokens incrustados en HTML renderizado en servidor. Aquí el cliente es
                        // un SPA que lee la cookie (no HttpOnly a propósito) y la reenvía tal
                        // cual como cabecera — con el handler XOR ese valor nunca coincide con
                        // el que se compara en el servidor y el login falla con 403 aunque las
                        // credenciales sean correctas. Con el handler plano el valor de la
                        // cookie y el que se debe enviar como cabecera son el mismo.
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        // Stripe authenticates requests with its own signed webhook scheme.
                        .ignoringRequestMatchers("/api/webhooks/stripe"))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // ── Auth y registro ──────────────────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        // El access token ya puede estar caducado al llamar a /refresh — la
                        // validez real la comprueba RefreshSessionUseCase contra el refresh token.
                        .requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll()

                        // ── Storage (presigned URL generation) ───────────────────────
                        .requestMatchers(HttpMethod.POST, "/api/storage/presign/upload").hasAnyRole("GESTOR", "ADMIN")

                        // ── Campeonatos ──────────────────────────────────────────────
                        // /all devuelve campeonatos ocultos → solo gestores
                        .requestMatchers(HttpMethod.GET,    "/api/championships/all").hasAnyRole("GESTOR", "ADMIN")
                        // Ver inscripciones de un campeonato → solo gestores
                        .requestMatchers(HttpMethod.GET,    "/api/championships/*/registrations").hasAnyRole("GESTOR", "ADMIN")
                        // El PDF contiene datos personales (incluido el DNI) → nunca público
                        .requestMatchers(HttpMethod.GET,    "/api/championships/*/registrations/pdf").hasAnyRole("GESTOR", "ADMIN")
                        // Listado y detalle de campeonatos visibles → público
                        .requestMatchers(HttpMethod.GET,    "/api/championships", "/api/championships/**").permitAll()
                        // Inscribirse en un campeonato → requiere login
                        .requestMatchers(HttpMethod.POST,   "/api/championships/*/registrations").authenticated()
                        // Gestión → solo gestores
                        .requestMatchers(HttpMethod.POST,   "/api/championships").hasAnyRole("GESTOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/api/championships/**").hasAnyRole("GESTOR", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/championships/**").hasAnyRole("GESTOR", "ADMIN")

                        // ── Noticias ─────────────────────────────────────────────────
                        // /all devuelve noticias no publicadas → solo gestores
                        .requestMatchers(HttpMethod.GET,    "/api/news/all").hasAnyRole("GESTOR", "ADMIN")
                        // Listado y detalle de noticias publicadas → público
                        .requestMatchers(HttpMethod.GET,    "/api/news", "/api/news/**").permitAll()
                        // Gestión → solo gestores
                        .requestMatchers(HttpMethod.POST,   "/api/news").hasAnyRole("GESTOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/api/news/**").hasAnyRole("GESTOR", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/news/**").hasAnyRole("GESTOR", "ADMIN")

                        // ── Resultados ───────────────────────────────────────────────
                        // /all devuelve resultados no publicados → solo gestores
                        .requestMatchers(HttpMethod.GET,    "/api/results/all").hasAnyRole("GESTOR", "ADMIN")
                        // Listado, detalle y descarga de PDF → público
                        .requestMatchers(HttpMethod.GET,    "/api/results", "/api/results/**").permitAll()
                        // Gestión → solo gestores
                        .requestMatchers(HttpMethod.POST,   "/api/results").hasAnyRole("GESTOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/api/results/**").hasAnyRole("GESTOR", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/results/**").hasAnyRole("GESTOR", "ADMIN")

                        // ── Categorías ───────────────────────────────────────────────
                        .requestMatchers(HttpMethod.GET,    "/api/categories", "/api/categories/**").permitAll()
                        .requestMatchers(HttpMethod.POST,   "/api/categories").hasAnyRole("GESTOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/api/categories/**").hasAnyRole("GESTOR", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasAnyRole("GESTOR", "ADMIN")

                        // ── Inscripciones ────────────────────────────────────────────
                        .requestMatchers(HttpMethod.DELETE, "/api/registrations/**").authenticated()
                        .requestMatchers(HttpMethod.PUT,    "/api/registrations/*/payment").hasAnyRole("GESTOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/api/registrations/*/category").hasAnyRole("GESTOR", "ADMIN")

                        // ── Cuotas anuales ──────────────────────────────────────────
                        .requestMatchers(HttpMethod.GET,    "/api/quotas/my").authenticated()
                        .requestMatchers(HttpMethod.POST,   "/api/quotas/my/**").authenticated()
                        .requestMatchers(HttpMethod.GET,    "/api/quotas/prices/**").hasAnyRole("GESTOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/api/quotas/prices/**").hasAnyRole("GESTOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/api/quotas/users/**").hasAnyRole("GESTOR", "ADMIN")
                        .requestMatchers(HttpMethod.GET,    "/api/quotas/users/**").hasAnyRole("GESTOR", "ADMIN")

                        // ── Usuarios ─────────────────────────────────────────────────
                        // Rutas admin más específicas ANTES que las generales
                        .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole("ADMIN")
                        // Cambios de rol/bloqueo: solo ADMIN. GESTOR usa /management,
                        // que no acepta campos privilegiados.
                        .requestMatchers(HttpMethod.PUT,    "/api/users/*/admin").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/api/users/*/management").hasAnyRole("GESTOR", "ADMIN")
                        .requestMatchers(HttpMethod.GET,    "/api/users/*/registrations").authenticated()
                        .requestMatchers(HttpMethod.GET,    "/api/users").hasAnyRole("GESTOR", "ADMIN")
                        // El controlador aplica ownership para /{id}; gestores y admins pueden ver cualquiera.
                        .requestMatchers(HttpMethod.GET,    "/api/users/*").authenticated()
                        .requestMatchers(HttpMethod.GET,    "/api/users/**").authenticated()
                        .requestMatchers(HttpMethod.PUT,    "/api/users/*/password").authenticated()
                        .requestMatchers(HttpMethod.PUT,    "/api/users/**").authenticated()

                        // ── Precios ──────────────────────────────────────────────────
                        // Calcular precio → público (ver precio antes de loguearse)
                        .requestMatchers(HttpMethod.POST,   "/api/pricing/configs/*/calculate").permitAll()
                        // Preparar checkout → requiere login (se necesita saber quién paga)
                        .requestMatchers(HttpMethod.POST,   "/api/pricing/configs/*/checkout").authenticated()
                        // Gestión de configuración de precios → solo gestores
                        .requestMatchers(HttpMethod.GET,    "/api/pricing/**").hasAnyRole("GESTOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/api/pricing/**").hasAnyRole("GESTOR", "ADMIN")
                        .requestMatchers(HttpMethod.POST,   "/api/pricing/**").hasAnyRole("GESTOR", "ADMIN")

                        // ── Equipos ──────────────────────────────────────────────────
                        // Datos completos de gestión → solo gestores y administradores.
                        .requestMatchers(HttpMethod.GET,    "/api/teams/admin", "/api/teams/admin/**").hasAnyRole("GESTOR", "ADMIN")
                        // Listado y detalle público → DTO sin datos del responsable.
                        .requestMatchers(HttpMethod.GET,    "/api/teams", "/api/teams/**").permitAll()
                        // Gestión → solo gestores
                        .requestMatchers(HttpMethod.POST,   "/api/teams").hasAnyRole("GESTOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/api/teams/**").hasAnyRole("GESTOR", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/teams/**").hasAnyRole("GESTOR", "ADMIN")

                        // ── Documentación ────────────────────────────────────────────
                        // /all devuelve documentos no publicados → solo gestores
                        .requestMatchers(HttpMethod.GET,    "/api/documents/all").hasAnyRole("GESTOR", "ADMIN")
                        // Listado, detalle y descarga → público
                        .requestMatchers(HttpMethod.GET,    "/api/documents", "/api/documents/**").permitAll()
                        // Gestión → solo gestores
                        .requestMatchers(HttpMethod.POST,   "/api/documents").hasAnyRole("GESTOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/api/documents/**").hasAnyRole("GESTOR", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/documents/**").hasAnyRole("GESTOR", "ADMIN")

                        // ── Webhooks de pago ─────────────────────────────────────────
                        // El proveedor de pago llama sin JWT → público
                        .requestMatchers(HttpMethod.POST,   "/api/webhooks/stripe").permitAll()

                        // ── Por defecto ──────────────────────────────────────────────
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(rateLimitFilter, JwtAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> {
                            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            res.setContentType("application/problem+json");
                            res.getWriter().write(
                                    "{\"status\":401,\"title\":\"Unauthorized\",\"detail\":\"Token ausente o inválido\"}"
                            );
                        })
                        .accessDeniedHandler((req, res, e) -> {
                            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            res.setContentType("application/problem+json");
                            res.getWriter().write(
                                    "{\"status\":403,\"title\":\"Forbidden\",\"detail\":\"No tienes permiso para realizar esta acción\"}"
                            );
                        })
                )
                .build();
    }

    @Bean
    public CookieCsrfTokenRepository csrfTokenRepository(AuthCookieProperties authCookieProperties) {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer(cookie -> cookie
                .path("/")
                .secure(authCookieProperties.isSecure())
                .sameSite(authCookieProperties.getSameSite()));
        return repository;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
