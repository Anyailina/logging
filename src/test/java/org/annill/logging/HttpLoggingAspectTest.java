package org.annill.logging;

import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import org.annill.logging.aspect.HttpLoggingAspect;
import org.annill.logging.util.LoggingUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.web.context.request.*;

import java.util.Collections;
import java.util.Enumeration;

class HttpLoggingAspectTest {

    @InjectMocks
    private HttpLoggingAspect aspect;

    @Mock
    private LoggingUtils loggingUtils;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        ServletRequestAttributes attrs = mock(ServletRequestAttributes.class);
        when(attrs.getRequest()).thenReturn(request);
        RequestContextHolder.setRequestAttributes(attrs);

        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/test");
        when(request.getHeaderNames()).thenReturn(Collections.enumeration(Collections.emptyList()));
        when(request.getQueryString()).thenReturn(null);
    }

    @Test
    void testLogIncomingRequest() {
        aspect.logIncomingRequest();
        verify(loggingUtils).logHttpEvent(any());
    }

    @Test
    void testLogIncomingResponse() {
        aspect.logIncomingResponse("responseBody");
        verify(loggingUtils).logHttpEvent(any());
    }

    @Test
    void testLogIncomingError() {
        aspect.logIncomingError(new RuntimeException("fail"));
        verify(loggingUtils).logHttpEvent(any());
    }
    @Test
    void testLogIncomingRequestWithHeaders() {
        when(request.getHeaderNames()).thenReturn(Collections.enumeration(List.of("header1")));
        when(request.getHeader("header1")).thenReturn("value1");

        aspect.logIncomingRequest();

        verify(loggingUtils).logHttpEvent(
            argThat(event -> event.getHeaders() != null && event.getHeaders().containsKey("header1"))
        );
    }

    @Test
    void testLogIncomingRequestWithQueryParams() {
        when(request.getQueryString()).thenReturn("param1=value1&param2=value2");

        aspect.logIncomingRequest();

        verify(loggingUtils).logHttpEvent(
            argThat(event -> event.getQueryParams() != null && event.getQueryParams().contains("param1=value1"))
        );
    }

    @Test
    void testLogIncomingResponseWithNullBody() {
        aspect.logIncomingResponse(null);

        verify(loggingUtils).logHttpEvent(
            argThat(event -> event.getResponseBody() == null)
        );
    }

}
