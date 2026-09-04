package dev.brunohm.bv2_projeto_software_uepg.dto.notificacao;

import java.time.LocalDateTime;

import dev.brunohm.bv2_projeto_software_uepg.domain.entity.TemplateNotificacao;
import dev.brunohm.bv2_projeto_software_uepg.domain.enums.StatusOs;

public record TemplateNotificacaoResponse(
        StatusOs status,
        String conteudo,
        Boolean ativo,
        LocalDateTime atualizadoEm) {

    public static TemplateNotificacaoResponse fromEntity(TemplateNotificacao template) {
        return new TemplateNotificacaoResponse(
                template.getStatus(),
                template.getConteudo(),
                template.getAtivo(),
                template.getAtualizadoEm());
    }
}
