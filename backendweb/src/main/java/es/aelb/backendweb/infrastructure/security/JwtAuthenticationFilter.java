package es.aelb.backendweb.infrastructure.security;

import es.aelb.backendweb.domain.user.User;
import es.aelb.backendweb.domain.user.UserRepository;
import es.aelb.backendweb.domain.user.valueobject.UserId;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Filtro que extrae el JWT exclusivamente de la cookie HttpOnly,
 * lo valida y establece la autenticación en el SecurityContext.
 * Se ejecuta una vez por petición (OncePerRequestFilter).
 *
 * La firma y expiración del JWT solo prueban que el token lo emitió este
 * servidor y no ha caducado — no bastan para autorizar. En cada petición se
 * contrasta contra BD: si el usuario no existe, está bloqueado o la versión
 * de sesión (claim "ver") no coincide con la actual — que se incrementa al
 * bloquear/desbloquear, cambiar el rol o cambiar la contraseña — el token se
 * trata como revocado aunque su firma sea válida. La autoridad concedida usa
 * siempre el rol vivo de BD, nunca el claim "role" del JWT.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final AuthCookieProperties authCookieProperties;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, AuthCookieProperties authCookieProperties, UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.authCookieProperties = authCookieProperties;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String token = Arrays.stream(request.getCookies() == null ? new jakarta.servlet.http.Cookie[0] : request.getCookies())
                .filter(cookie -> authCookieProperties.getName().equals(cookie.getName()))
                .map(jakarta.servlet.http.Cookie::getValue)
                .findFirst()
                .orElse(null);

        // Sin cookie no se registra nada: es el caso normal de una petición
        // anónima y, con diferencia, el más frecuente — no tiene valor como
        // señal de seguridad y generaría ruido en cada petición pública.
        if (token != null) {
            try {
                Claims claims = jwtTokenProvider.getClaims(token);
                String userId       = claims.getSubject();
                int    tokenVersion = jwtTokenProvider.getTokenVersion(claims);

                User user = userRepository.findById(UserId.of(userId)).orElse(null);

                // Un token con firma/expiración válidas puede seguir sin autorizar: la
                // cuenta puede haber sido borrada, bloqueada, o la versión de sesión
                // haber cambiado (rol/contraseña). Cada caso se registra con su motivo
                // porque, a diferencia de "sin cookie", son anómalos y poco frecuentes.
                if (user == null) {
                    log.warn("event=AUTH_TOKEN_REJECTED reason=USER_NOT_FOUND userId={}", userId);
                } else if (user.isBlocked()) {
                    log.warn("event=AUTH_TOKEN_REJECTED reason=USER_BLOCKED userId={}", userId);
                } else if (user.getTokenVersion() != tokenVersion) {
                    log.warn("event=AUTH_TOKEN_REJECTED reason=SESSION_REVOKED userId={}", userId);
                } else {
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (ExpiredJwtException e) {
                // Subtipo de JwtException: debe comprobarse antes que el catch genérico.
                log.warn("event=AUTH_TOKEN_REJECTED reason=TOKEN_EXPIRED");
            } catch (JwtException | IllegalArgumentException e) {
                // Firma inválida o token manipulado — mismas excepciones que antes
                // descartaba JwtTokenProvider.validateToken(token), ahora distinguidas aquí.
                log.warn("event=AUTH_TOKEN_REJECTED reason=TOKEN_INVALID");
            }
        }

        chain.doFilter(request, response);
    }
}
