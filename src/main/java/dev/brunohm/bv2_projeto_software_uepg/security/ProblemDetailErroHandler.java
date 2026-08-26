package dev.brunohm.bv2_projeto_software_uepg.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Falhas de 401/403 acontecem na cadeia de filtros, antes do DispatcherServlet,
 * e por padrao o container responderia HTML. Aqui elas sao devolvidas ao
 * HandlerExceptionResolver para cairem no GlobalExceptionHandler, de modo que
 * toda a API tenha um unico formato de erro (RFC 7807).
 */
@Component
public class ProblemDetailErroHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final HandlerExceptionResolver handlerExceptionResolver;

    public ProblemDetailErroHandler(
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver) {
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException ex) {
        handlerExceptionResolver.resolveException(request, response, null, ex);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException ex) {
        handlerExceptionResolver.resolveException(request, response, null, ex);
    }
}
