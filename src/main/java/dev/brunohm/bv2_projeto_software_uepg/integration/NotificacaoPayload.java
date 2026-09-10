package dev.brunohm.bv2_projeto_software_uepg.integration;

import java.math.BigDecimal;
import java.time.LocalDate;

import dev.brunohm.bv2_projeto_software_uepg.domain.entity.OrdemServico;
import dev.brunohm.bv2_projeto_software_uepg.domain.enums.StatusOs;

/**
 * Corpo do POST que o BV2 manda ao webhook do n8n.
 *
 * <p>
 * Leva a <b>mensagem ja renderizada</b> — e o caminho normal, o n8n so encaminha
 * e nao tem campo de texto nem regra de decisao. Os campos estruturados vao junto
 * de proposito: se a Meta exigir template aprovado (fora da janela de 24h a Cloud
 * API recusa texto livre), o n8n mapeia cliente.nome e ordemServico.id nas
 * variaveis do template sem que o backend mude uma linha.
 */
public record NotificacaoPayload(
        StatusOs evento,
        Long notificacaoId,
        String mensagem,
        OrdemServicoPayload ordemServico,
        ClientePayload cliente) {

    public record OrdemServicoPayload(
            Long id,
            StatusOs statusAnterior,
            StatusOs status,
            LocalDate dataEntrada,
            LocalDate dataConcluida,
            LocalDate dataEntregue,
            BigDecimal valorTotal) {
    }

    public record ClientePayload(
            Long id,
            String nome,
            String telefone) {
    }

    public static NotificacaoPayload de(Long notificacaoId, String mensagem,
            OrdemServico os, StatusOs statusAnterior, StatusOs statusNovo) {
        return new NotificacaoPayload(
                statusNovo,
                notificacaoId,
                mensagem,
                new OrdemServicoPayload(
                        os.getId(),
                        statusAnterior,
                        statusNovo,
                        os.getDataEntrada(),
                        os.getDataConcluida(),
                        os.getDataEntregue(),
                        os.getValorTotal()),
                new ClientePayload(
                        os.getCliente().getId(),
                        os.getCliente().getNome(),
                        os.getCliente().getTelefone()));
    }
}
