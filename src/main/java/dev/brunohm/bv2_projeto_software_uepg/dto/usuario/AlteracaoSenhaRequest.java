package dev.brunohm.bv2_projeto_software_uepg.dto.usuario;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Troca de senha pelo proprio usuario, provando que sabe a senha atual.
 *
 * <p>
 * A confirmacao e conferida no servidor mesmo sendo um campo de interface: a regra
 * nao pode depender de o front conferir.
 */
public record AlteracaoSenhaRequest(

        /*
         * Sem @Size de proposito, como no LoginRequest: quem so esta conferindo a
         * senha antiga nao precisa saber a politica de tamanho da nova.
         */
        @Schema(description = "A senha atual, para provar a posse da conta.")
        @NotBlank(message = "A senha atual é obrigatória")
        String senhaAtual,

        @Schema(description = "A nova senha. O limite de 72 é o do BCrypt, que trunca acima disso.")
        @NotBlank(message = "A nova senha é obrigatória")
        @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")
        String senhaNova,

        @Schema(description = "Repetição da nova senha. Precisa ser idêntica à senhaNova.")
        @NotBlank(message = "A confirmação da nova senha é obrigatória")
        String senhaNovaConfirmacao) {
}
