package dev.brunohm.bv2_projeto_software_uepg.dto.usuario;

import java.time.LocalDateTime;

import dev.brunohm.bv2_projeto_software_uepg.domain.entity.Usuario;
import dev.brunohm.bv2_projeto_software_uepg.domain.enums.RoleUsuario;

/** Projecao publica do usuario. A senha nunca aparece aqui. */
public record UsuarioResponse(
        Long id,
        String nome,
        String email,
        RoleUsuario role,
        Boolean ativo,
        LocalDateTime criadoEm) {

    public static UsuarioResponse fromEntity(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getRole(),
                usuario.getAtivo(),
                usuario.getCriadoEm());
    }
}
