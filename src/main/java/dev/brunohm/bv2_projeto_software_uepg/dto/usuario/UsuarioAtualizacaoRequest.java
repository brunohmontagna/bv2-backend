package dev.brunohm.bv2_projeto_software_uepg.dto.usuario;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * O papel e a situacao nao entram aqui: MASTER nao e atribuivel, e ativo muda
 * pelos PATCH /ativar e /desativar.
 */
public record UsuarioAtualizacaoRequest(

        @NotBlank(message = "O nome e obrigatorio")
        @Size(max = 50, message = "O nome deve ter no maximo 50 caracteres")
        String nome,

        @NotBlank(message = "O e-mail e obrigatorio")
        @Email(message = "E-mail em formato invalido")
        @Size(max = 50, message = "O e-mail deve ter no maximo 50 caracteres")
        String email,

        @Schema(description = "Nova senha. Se omitida ou vazia, a senha atual e mantida.")
        @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")
        String senha) {
}
