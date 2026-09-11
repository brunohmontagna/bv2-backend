package dev.brunohm.bv2_projeto_software_uepg.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Confirmacao da troca de e-mail. So o token: o endereco novo foi escolhido no
 * pedido e viaja junto do token no banco, entao a tela de confirmacao nao tem
 * formulario nenhum — so precisa clicar.
 */
public record ConfirmacaoEmailRequest(

        @Schema(description = "O token que veio no link enviado ao novo endereco.")
        @NotBlank(message = "O token e obrigatorio")
        String token) {
}
