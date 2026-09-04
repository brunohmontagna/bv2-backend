package dev.brunohm.bv2_projeto_software_uepg.dto.notificacao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * O status nao vem no corpo: e a chave do recurso e viaja no path. Ativo e
 * conteudo vem juntos porque o modal do front edita os dois de uma vez.
 *
 * <p>
 * Conteudo continua obrigatorio mesmo com ativo=false — desligar a notificacao
 * nao e motivo para perder o texto ja escrito.
 */
public record TemplateNotificacaoRequest(

        @NotNull(message = "Informe se a notificacao automatica esta ativa.")
        Boolean ativo,

        @NotBlank(message = "O conteudo da mensagem e obrigatorio.")
        @Size(max = 500, message = "O conteudo deve ter no maximo 500 caracteres.")
        String conteudo) {
}
