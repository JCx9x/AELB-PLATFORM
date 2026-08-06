package es.aelb.backendweb.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * Limita peticiones por IP en rutas sensibles a fuerza bruta, enumeración de
 * cuentas o abuso de Stripe (auditoría M-01). Es una protección de grano
 * grueso por ruta+IP; el login aplica además un límite más fino por
 * IP+cuenta en AuthController, ya que ahí sí se conoce el email solicitado.
 *
 * Esto complementa, no sustituye, una protección a nivel de red (WAF/CDN)
 * delante del backend — ver documentación de despliegue.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private record RouteLimit(String method, String pattern, int maxRequests, Duration window) {}

    private static final List<RouteLimit> ROUTE_LIMITS = List.of(
            // Login: límite amplio por IP (independiente del email) para frenar
            // enumeración de cuentas probando muchos emails desde la misma IP.
            new RouteLimit("POST", "/api/auth/login", 20, Duration.ofMinutes(1)),
            // Alta de cuenta: abuso para crear cuentas basura o tantear emails ya registrados.
            new RouteLimit("POST", "/api/users", 8, Duration.ofHours(1)),
            // Creación de checkout de Stripe: evita generar sesiones de pago sin límite.
            new RouteLimit("POST", "/api/registrations/*/checkout", 20, Duration.ofMinutes(1)),
            new RouteLimit("POST", "/api/championships/*/registration-payment/checkout", 20, Duration.ofMinutes(1)),
            new RouteLimit("POST", "/api/quotas/my/*/checkout", 20, Duration.ofMinutes(1)),
            new RouteLimit("POST", "/api/pricing/configs/*/checkout", 20, Duration.ofMinutes(1)),
            // Webhook de Stripe: sin JWT, así que solo la IP identifica al emisor.
            // El límite es generoso porque una IP legítima de Stripe no debe
            // verse nunca bloqueada; solo frena un flujo de peticiones masivo.
            new RouteLimit("POST", "/api/webhooks/stripe", 60, Duration.ofMinutes(1))
    );

    private final InMemoryRateLimiter rateLimiter;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public RateLimitFilter(InMemoryRateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        RouteLimit limit = findLimit(request.getMethod(), request.getRequestURI());

        if (limit != null) {
            String key = limit.method() + ":" + limit.pattern() + ":" + request.getRemoteAddr();
            if (!rateLimiter.tryConsume(key, limit.maxRequests(), limit.window())) {
                response.setStatus(429);
                response.setContentType("application/problem+json");
                response.getWriter().write(
                        "{\"status\":429,\"title\":\"Too Many Requests\",\"detail\":\"Demasiadas solicitudes. Inténtalo de nuevo en unos minutos.\"}"
                );
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private RouteLimit findLimit(String method, String uri) {
        for (RouteLimit routeLimit : ROUTE_LIMITS) {
            if (routeLimit.method().equals(method) && pathMatcher.match(routeLimit.pattern(), uri)) {
                return routeLimit;
            }
        }
        return null;
    }
}
