package es.aelb.backendweb.infrastructure.security;

import es.aelb.backendweb.domain.user.User;
import es.aelb.backendweb.domain.user.UserRepository;
import es.aelb.backendweb.domain.user.valueobject.UserId;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

        if (token != null && jwtTokenProvider.validateToken(token)) {
            Claims claims = jwtTokenProvider.getClaims(token);
            String userId       = claims.getSubject();
            int    tokenVersion = jwtTokenProvider.getTokenVersion(claims);

            User user = userRepository.findById(UserId.of(userId)).orElse(null);

            if (user != null && !user.isBlocked() && user.getTokenVersion() == tokenVersion) {
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        chain.doFilter(request, response);
    }
}
