package dev.brunohm.bv2_projeto_software_uepg.dto.painel;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resposta unica do dashboard. Nao tem fromEntity porque nao deriva de nenhuma
 * entidade: e a consolidacao de doze agregacoes, montada pelo PainelService.
 */
@Schema(description = "Indicadores consolidados da operação")
public record PainelResponse(
        PeriodoResponse periodo,
        ResumoGeralResponse resumo,
        FaturamentoResponse faturamento,

        @Schema(description = "Um ponto por mês do período, inclusive os meses sem movimento")
        List<SerieMensalResponse> serieMensal,

        RankingsResponse rankings) {
}
