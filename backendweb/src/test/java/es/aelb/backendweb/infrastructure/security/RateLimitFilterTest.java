package es.aelb.backendweb.infrastructure.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Verifica que el límite se aplica por {@code request.getRemoteAddr()} — es decir, por la IP
 * que el filtro realmente recibe en ese momento (real, una vez corregido H-06 con
 * {@code server.forward-headers-strategy=framework}; interna de Caddy si no lo estuviera).
 * No repite aquí las pruebas de {@link org.springframework.web.filter.ForwardedHeaderFilter},
 * que es código de Spring ya probado por el framework.
 */
class RateLimitFilterTest {

    private static final String LOGIN_PATH = "/api/auth/login";

    private RateLimitFilter newFilter() {
        return new RateLimitFilter(new InMemoryRateLimiter());
    }

    private MockHttpServletRequest loginRequest(String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", LOGIN_PATH);
        request.setRemoteAddr(remoteAddr);
        return request;
    }

    @Test
    void dosClientesConIpDistintaNoComparteLimite() throws Exception {
        RateLimitFilter filter = newFilter();
        FilterChain chain = mock(FilterChain.class);

        // Agota el límite de /api/auth/login (20/min) para el cliente A.
        for (int i = 0; i < 20; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilterInternal(loginRequest("203.0.113.10"), response, chain);
            assertEquals(200, response.getStatus());
        }
        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilterInternal(loginRequest("203.0.113.10"), blocked, chain);
        assertEquals(429, blocked.getStatus());

        // El cliente B, con una IP distinta, no se ve afectado por el bloqueo de A.
        MockHttpServletResponse otherClient = new MockHttpServletResponse();
        filter.doFilterInternal(loginRequest("203.0.113.20"), otherClient, chain);
        assertEquals(200, otherClient.getStatus());

        verify(chain, times(21)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void elMismoClienteMantieneSuLimiteEntrePeticiones() throws Exception {
        RateLimitFilter filter = newFilter();
        FilterChain chain = mock(FilterChain.class);
        String ip = "198.51.100.5";

        for (int i = 0; i < 20; i++) {
            filter.doFilterInternal(loginRequest(ip), new MockHttpServletResponse(), chain);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(loginRequest(ip), response, chain);

        assertEquals(429, response.getStatus());
        verify(chain, times(20)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rutaSinLimiteConfiguradoNuncaBloquea() throws Exception {
        RateLimitFilter filter = newFilter();
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/championships");
        request.setRemoteAddr("203.0.113.10");

        for (int i = 0; i < 50; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilterInternal(request, response, chain);
            assertEquals(200, response.getStatus());
        }
        verify(chain, times(50)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
