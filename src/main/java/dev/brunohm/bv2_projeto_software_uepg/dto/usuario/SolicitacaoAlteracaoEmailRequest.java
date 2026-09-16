package dev.brunohm.bv2_projeto_software_uepg.dto.usuario;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Pedido de troca de e-mail. Nada muda ainda: o endereco so passa a valer depois
 * que o link enviado a ele for clicado.
 */
public record SolicitacaoAlteracaoEmailRequest(

        /*
         * Sem @Size de proposito, como no LoginRequest: quem esta so provando quem e
         * nao precisa saber a politica de tamanho da senha.
         *
         * A senha e o que impede que uma sessao roubada vire tomada de conta. Como a
         * confirmacao vai para o endereco NOVO, o clique no link nao prova identidade
         * — quem clica e quem controla o destino, que no ataque e o atacante.
         */
        @Schema(description = "A senha atual, para provar que o pedido partiu do dono da conta.")
        @NotBlank(message = "A senha atual é obrigatória")
        String senhaAtual,

        @Schema(description = "O novo endereço. É para ele que vai o link de confirmação.")
        @NotBlank(message = "O novo e-mail é obrigatório")
        @Email(message = "E-mail em formato inválido")
        @Size(max = 50, message = "O e-mail deve ter no máximo 50 caracteres")
        String novoEmail,

        @Schema(description = "Repetição do novo endereço. Precisa ser idêntica ao novoEmail.")
        @NotBlank(message = "A confirmação do novo e-mail é obrigatória")
        String novoEmailConfirmacao) {
}
