package dev.brunohm.bv2_projeto_software_uepg.dto.notificacao;

import java.time.LocalDateTime;

import dev.brunohm.bv2_projeto_software_uepg.domain.entity.Notificacao;
import dev.brunohm.bv2_projeto_software_uepg.domain.enums.StatusNotificacao;
import dev.brunohm.bv2_projeto_software_uepg.domain.enums.StatusOs;
import dev.brunohm.bv2_projeto_software_uepg.dto.equipamento.ClienteResumoResponse;

/**
 * Os dois "status" convivem e significam coisas diferentes: statusOs e a
 * transicao da OS que disparou o envio; status e o resultado do envio em si.
 *
 * <p>
 * A OS entra so pelo id: expor o OrdemServicoResponse inteiro forcaria um join
 * a mais em toda listagem, e o cliente — que e o dado util aqui — ja vem
 * aninhado. Ler o id de um proxy LAZY nao dispara query.
 */
public record NotificacaoResponse(
        Long id,
        Long ordemServicoId,
        ClienteResumoResponse cliente,
        StatusOs statusOs,
        String conteudo,
        StatusNotificacao status,
        Integer tentativas,
        LocalDateTime dataEnvio,
        LocalDateTime criadoEm) {

    public static NotificacaoResponse fromEntity(Notificacao notificacao) {
        return new NotificacaoResponse(
                notificacao.getId(),
                notificacao.getOrdemServico().getId(),
                ClienteResumoResponse.fromEntity(notificacao.getCliente()),
                notificacao.getStatusOs(),
                notificacao.getConteudo(),
                notificacao.getStatus(),
                notificacao.getTentativas(),
                notificacao.getDataEnvio(),
                notificacao.getCriadoEm());
    }
}
