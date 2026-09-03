package dev.brunohm.bv2_projeto_software_uepg.dto.painel;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Linha do ranking de clientes por faturamento. O faturamento soma o valorTotal
 * das OS entregues do cliente — nunca os valores dos itens, que divergem do total
 * sempre que houve valorTotalManual.
 */
@Schema(description = "Item de ranking por faturamento")
public record RankingFaturamentoResponse(Long id, String nome, BigDecimal faturamento, Long ordensEntregues) {
}
