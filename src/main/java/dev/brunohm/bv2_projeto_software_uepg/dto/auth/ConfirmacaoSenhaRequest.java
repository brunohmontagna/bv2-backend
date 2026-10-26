package dev.brunohm.bv2_projeto_software_uepg.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Confirmacao da troca de senha. So o token: a senha nova foi escolhida no pedido
 * e viaja junto do token no banco (em hash), entao a tela de confirmacao nao tem
 * formulario nenhum — so precisa clicar.
 */
public record ConfirmacaoSenhaRequest(

        @Schema(description = "O token que veio no link enviado ao e-mail do usuário.")
        @NotBlank(message = "O token é obrigatório")
        String token) {
}
