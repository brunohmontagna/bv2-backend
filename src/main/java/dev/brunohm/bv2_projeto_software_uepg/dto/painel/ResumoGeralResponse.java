package dev.brunohm.bv2_projeto_software_uepg.dto.painel;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import dev.brunohm.bv2_projeto_software_uepg.domain.enums.StatusOs;
import dev.brunohm.bv2_projeto_software_uepg.dto.painel.projecao.ContagemStatusProjecao;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Linha de topo do painel: a fotografia de agora.
 *
 * <p>
 * "Geral" e nao apenas "Resumo" porque o sufixo ...ResumoResponse ja significa
 * outra coisa no projeto (recorte enxuto de uma entidade, como ClienteResumoResponse).
 *
 * <p>
 * Nada aqui e recortado por periodo: quantas OS estao em cada status e quantos
 * cadastros existem sao estado atual, nao movimento da janela analisada.
 */
@Schema(description = "Contagens de estado atual, sem recorte de periodo")
public record ResumoGeralResponse(
        @Schema(description = "Quantidade de OS em cada status, com os quatro status sempre presentes")
        Map<StatusOs, Long> ordensPorStatus,
        long clientesAtivos,
        long equipamentos,
        long servicosAtivos) {

    /**
     * O group by so devolve status que tem OS. Semear os quatro em zero antes de
     * sobrescrever garante que o front possa indexar por StatusOs sem checar
     * ausencia — um status sem nenhuma OS vale 0, nao "nao existe".
     *
     * <p>
     * EnumMap preserva a ordem de declaracao do enum, que e a ordem do fluxo da
     * oficina (EM_ANDAMENTO, CONCLUIDA, ENTREGUE, CANCELADA).
     */
    public static ResumoGeralResponse consolidar(List<ContagemStatusProjecao> contagens,
            long clientesAtivos, long equipamentos, long servicosAtivos) {

        Map<StatusOs, Long> porStatus = new EnumMap<>(StatusOs.class);
        for (StatusOs status : StatusOs.values()) {
            porStatus.put(status, 0L);
        }
        for (ContagemStatusProjecao contagem : contagens) {
            porStatus.put(contagem.status(), contagem.quantidade());
        }

        return new ResumoGeralResponse(porStatus, clientesAtivos, equipamentos, servicosAtivos);
    }
}
