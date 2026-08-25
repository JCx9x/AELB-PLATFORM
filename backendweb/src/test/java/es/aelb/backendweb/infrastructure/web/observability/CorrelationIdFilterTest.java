package es.aelb.backendweb.infrastructure.web.observability;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {
    @AfterEach
    void clearMdc() { MDC.clear(); }

    @Test
    void generatesResponseIdAndCleansMdc() throws Exception {
        CorrelationIdFilter filter = new CorrelationIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        final String[] observed = new String[1];
        FilterChain chain = (req, res) -> observed[0] = MDC.get("correlationId");

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isNotBlank();
        assertThat(observed[0]).isEqualTo(response.getHeader(CorrelationIdFilter.HEADER));
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void acceptsSafeIncomingIdAndRejectsUnsafeValue() throws Exception {
        CorrelationIdFilter filter = new CorrelationIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");
        request.addHeader(CorrelationIdFilter.HEADER, "request-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> { });
        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo("request-123");

        MockHttpServletRequest unsafe = new MockHttpServletRequest("GET", "/api/health");
        unsafe.addHeader(CorrelationIdFilter.HEADER, "bad\nvalue");
        MockHttpServletResponse unsafeResponse = new MockHttpServletResponse();
        filter.doFilter(unsafe, unsafeResponse, (req, res) -> { });
        assertThat(unsafeResponse.getHeader(CorrelationIdFilter.HEADER)).doesNotContain("bad");
    }
}
