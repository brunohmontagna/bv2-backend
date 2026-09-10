package dev.brunohm.bv2_projeto_software_uepg.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Redefinicao com o token recebido por e-mail. Nao pede a senha atual: quem chega
 * aqui e justamente quem nao a sabe — o token e a prova de posse da conta.
 */
public record RedefinicaoSenhaRequest(

        @Schema(description = "O token que veio no link do e-mail.")
        @NotBlank(message = "O token e obrigatorio")
        String token,

        @Schema(description = "A nova senha. O limite de 72 e o do BCrypt, que trunca acima disso.")
        @NotBlank(message = "A nova senha e obrigatoria")
        @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")
        String senhaNova,

        @Schema(description = "Repeticao da nova senha. Precisa ser identica a senhaNova.")
        @NotBlank(message = "A confirmacao da nova senha e obrigatoria")
        String senhaNovaConfirmacao) {
}
