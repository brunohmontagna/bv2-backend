package dev.brunohm.bv2_projeto_software_uepg.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Pedido do link de recuperacao. A resposta e sempre 202, exista o e-mail ou nao —
 * ver o Javadoc do RecuperacaoSenhaService.
 */
public record RecuperacaoSenhaRequest(

        @Schema(description = "E-mail da conta. A resposta nao revela se ele existe.")
        @NotBlank(message = "O e-mail e obrigatorio")
        @Email(message = "E-mail em formato invalido")
        @Size(max = 50, message = "O e-mail deve ter no maximo 50 caracteres")
        String email) {
}
