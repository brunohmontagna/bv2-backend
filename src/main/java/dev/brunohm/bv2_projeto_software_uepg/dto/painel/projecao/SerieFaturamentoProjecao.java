package dev.brunohm.bv2_projeto_software_uepg.dto.painel.projecao;

import java.math.BigDecimal;

/** Um mes da serie de faturamento, agrupado por dataEntregue. */
public record SerieFaturamentoProjecao(Integer ano, Integer mes, BigDecimal valorTotal) {
}
