package dev.brunohm.bv2_projeto_software_uepg.dto.painel.projecao;

import java.math.BigDecimal;

/**
 * Soma e contagem das OS entregues no periodo, colhidas numa varredura so porque
 * a contagem e o divisor do ticket medio.
 *
 * <p>
 * Agregada sem group by sempre devolve uma linha, entao o objeto nunca vem nulo —
 * mas {@code valorTotal} vem, quando nenhuma OS caiu no periodo.
 */
public record TotaisEntreguesProjecao(BigDecimal valorTotal, Long quantidade) {
}
