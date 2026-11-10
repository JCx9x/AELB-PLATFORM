package es.aelb.backendweb.infrastructure.security;

import es.aelb.backendweb.domain.user.User;
import es.aelb.backendweb.domain.user.UserRepository;
import es.aelb.backendweb.domain.user.UserRole;
import es.aelb.backendweb.domain.user.valueobject.UserId;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * H-05: {@link JwtAuthenticationFilter} debe diferenciar el motivo por el que un token
 * no autoriza (expirado / inválido / revocado / bloqueado / usuario inexistente) y, en
 * cualquier caso, nunca debe lanzar — solo decide si establece el contexto de seguridad.
 */
class JwtAuthenticationFilterTest {

    private static final String COOKIE_NAME = "aelb_access";
    private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final JwtAuthenticationFilter filter =
            new JwtAuthenticationFilter(jwtTokenProvider, new AuthCookieProperties(), userRepository);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest requestWithCookie(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (token != null) {
            request.setCookies(new jakarta.servlet.http.Cookie(COOKIE_NAME, token));
        }
        return request;
    }

    private void runFilter(MockHttpServletRequest request) throws Exception {
        FilterChain chain = mock(FilterChain.class);
        filter.doFilterInternal(request, new MockHttpServletResponse(), chain);
        verify(chain).doFilter(any(), any());
    }

    @Test
    void sinCookieNoEstableceAutenticacionYContinua() throws Exception {
        runFilter(requestWithCookie(null));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void tokenValidoDeUsuarioActivoEstableceLaAutenticacion() throws Exception {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("user-1");
        when(jwtTokenProvider.getClaims("good-token")).thenReturn(claims);
        when(jwtTokenProvider.getTokenVersion(claims)).thenReturn(3);

        User user = mock(User.class);
        when(user.isBlocked()).thenReturn(false);
        when(user.getTokenVersion()).thenReturn(3);
        when(user.getRole()).thenReturn(UserRole.GESTOR);
        when(userRepository.findById(UserId.of("user-1"))).thenReturn(Optional.of(user));

        runFilter(requestWithCookie("good-token"));

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertTrue(auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_GESTOR")));
    }

    @Test
    void tokenExpiradoNoAutenticaNiLanza() throws Exception {
        when(jwtTokenProvider.getClaims("expired-token"))
                .thenThrow(new ExpiredJwtException(null, mock(Claims.class), "expired"));

        runFilter(requestWithCookie("expired-token"));

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void tokenConFirmaInvalidaNoAutenticaNiLanza() throws Exception {
        when(jwtTokenProvider.getClaims("tampered-token")).thenThrow(new JwtException("bad signature"));

        runFilter(requestWithCookie("tampered-token"));

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void usuarioInexistenteNoAutentica() throws Exception {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("ghost");
        when(jwtTokenProvider.getClaims("token")).thenReturn(claims);
        when(jwtTokenProvider.getTokenVersion(claims)).thenReturn(1);
        when(userRepository.findById(UserId.of("ghost"))).thenReturn(Optional.empty());

        runFilter(requestWithCookie("token"));

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void usuarioBloqueadoNoAutenticaAunqueElTokenSeaValido() throws Exception {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("user-2");
        when(jwtTokenProvider.getClaims("token")).thenReturn(claims);
        when(jwtTokenProvider.getTokenVersion(claims)).thenReturn(1);

        User user = mock(User.class);
        when(user.isBlocked()).thenReturn(true);
        when(userRepository.findById(UserId.of("user-2"))).thenReturn(Optional.of(user));

        runFilter(requestWithCookie("token"));

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void versionDeSesionDistintaRevocaElToken() throws Exception {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("user-3");
        when(jwtTokenProvider.getClaims("token")).thenReturn(claims);
        when(jwtTokenProvider.getTokenVersion(claims)).thenReturn(1); // versión del JWT

        User user = mock(User.class);
        when(user.isBlocked()).thenReturn(false);
        when(user.getTokenVersion()).thenReturn(2); // versión actual en BD, ya incrementada
        when(userRepository.findById(UserId.of("user-3"))).thenReturn(Optional.of(user));

        runFilter(requestWithCookie("token"));

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
