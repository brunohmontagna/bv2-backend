package dev.brunohm.bv2_projeto_software_uepg.dto.painel;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Os tres rankings do periodo, todos limitados ao top 5. Listas vem vazias, nunca
 * nulas: um periodo sem movimento e uma resposta legitima do painel.
 */
@Schema(description = "Rankings do período (top 5 de cada)")
public record RankingsResponse(
        @Schema(description = "Serviços com mais execuções lançadas")
        List<RankingContagemResponse> servicosMaisExecutados,

        @Schema(description = "Clientes que mais faturaram, por OS entregue")
        List<RankingFaturamentoResponse> clientesPorFaturamento,

        @Schema(description = "Marcas presentes em mais ordens de serviço")
        List<RankingContagemResponse> marcasMaisAtendidas) {
}
