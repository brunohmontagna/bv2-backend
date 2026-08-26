package dev.brunohm.bv2_projeto_software_uepg.dto.cliente;

import dev.brunohm.bv2_projeto_software_uepg.domain.entity.Usuario;
import dev.brunohm.bv2_projeto_software_uepg.domain.enums.RoleUsuario;

/** Projecao publica do usuario. A senha nunca aparece aqui. */
public record UsuarioResponse(Long id, String nome, String email, RoleUsuario role) {

    public static UsuarioResponse fromEntity(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getRole());
    }
}
