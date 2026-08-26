package dev.brunohm.bv2_projeto_software_uepg.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Acesso ao usuario da requisicao em curso. Concentra a leitura do
 * SecurityContextHolder para que os services nao precisem repeti-la.
 */
@Component
public class AutenticacaoAtual {

    public UsuarioAutenticado usuario() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || !(autenticacao.getPrincipal() instanceof UsuarioAutenticado usuario)) {
            throw new AccessDeniedException("Requisicao sem usuario autenticado.");
        }
        return usuario;
    }

    public boolean isAdmin() {
        return usuario().isAdmin();
    }
}
