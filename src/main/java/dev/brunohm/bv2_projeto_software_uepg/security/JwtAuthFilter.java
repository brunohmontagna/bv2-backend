package dev.brunohm.bv2_projeto_software_uepg.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Le o header Authorization: Bearer <token> e popula o SecurityContext.
 * Token ausente ou invalido nao gera erro aqui: a requisicao segue sem
 * autenticacao e quem responde 401 e o AuthenticationEntryPoint.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIXO = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioDetailsService usuarioDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String token = extrairToken(request);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            jwtService.extrairEmail(token).ifPresent(email -> autenticar(email, request));
        }

        filterChain.doFilter(request, response);
    }

    private void autenticar(String email, HttpServletRequest request) {
        try {
            UserDetails usuario = usuarioDetailsService.loadUserByUsername(email);

            // Desativar um usuario precisa valer na hora. Sem isto, o token dele
            // continuaria aceito ate expirar (jwt.expiracao-minutos, 120 por padrao).
            if (!usuario.isEnabled()) {
                logger.debug("Token de usuario desativado: " + email);
                return;
            }

            var autenticacao = new UsernamePasswordAuthenticationToken(
                    usuario, null, usuario.getAuthorities());
            autenticacao.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(autenticacao);
        } catch (UsernameNotFoundException ex) {
            // Token assinado por nos, mas o usuario foi removido depois. Segue sem autenticar.
            logger.debug("Token valido para usuario inexistente: " + email);
        }
    }

    private String extrairToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        if (header == null || !header.startsWith(PREFIXO)) {
            return null;
        }
        String token = header.substring(PREFIXO.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
