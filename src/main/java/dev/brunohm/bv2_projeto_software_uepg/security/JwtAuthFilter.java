package dev.brunohm.bv2_projeto_software_uepg.security;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

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
import dev.brunohm.bv2_projeto_software_uepg.security.JwtService.TokenDecodificado;
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
            jwtService.decodificar(token).ifPresent(decodificado -> autenticar(decodificado, request));
        }

        filterChain.doFilter(request, response);
    }

    private void autenticar(TokenDecodificado decodificado, HttpServletRequest request) {
        String email = decodificado.email();
        try {
            UserDetails usuario = usuarioDetailsService.loadUserByUsername(email);

            // Desativar um usuario precisa valer na hora. Sem isto, o token dele
            // continuaria aceito ate expirar (jwt.expiracao-minutos, 120 por padrao).
            if (!usuario.isEnabled()) {
                logger.debug("Token de usuário desativado: " + email);
                return;
            }

            // Trocar a senha derruba as sessoes abertas: sem isto, quem roubou o
            // token continuaria dentro pelos 120 minutos de validade justamente no
            // cenario em que a vitima troca a senha as pressas.
            if (senhaTrocadaDepoisDe(usuario, decodificado.emitidoEm())) {
                logger.debug("Token anterior à última troca de senha: " + email);
                return;
            }

            var autenticacao = new UsernamePasswordAuthenticationToken(
                    usuario, null, usuario.getAuthorities());
            autenticacao.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(autenticacao);
        } catch (UsernameNotFoundException ex) {
            // Token assinado por nos, mas o usuario foi removido depois. Segue sem autenticar.
            logger.debug("Token válido para usuário inexistente: " + email);
        }
    }

    /**
     * Compara o iat do token com o senhaAlteradaEm do usuario, ambos em segundos.
     *
     * <p>
     * Nulo significa que a senha nunca foi trocada, e o token passa. A comparacao e
     * estritamente "antes": um token emitido no mesmo segundo da troca sobrevive —
     * janela irrelevante, ja que a troca de senha nao emite token novo.
     */
    private boolean senhaTrocadaDepoisDe(UserDetails usuario, Instant emitidoEm) {
        if (!(usuario instanceof UsuarioAutenticado autenticado)) {
            return false;
        }
        LocalDateTime senhaAlteradaEm = autenticado.getSenhaAlteradaEm();
        if (senhaAlteradaEm == null || emitidoEm == null) {
            return false;
        }
        return emitidoEm.isBefore(senhaAlteradaEm.atZone(ZoneId.systemDefault()).toInstant());
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
