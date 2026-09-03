package dev.brunohm.bv2_projeto_software_uepg.dto.painel;

import java.math.BigDecimal;
import java.math.RoundingMode;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Dinheiro do periodo, separado entre o que ja entrou e o que ainda esta na
 * oficina. OS cancelada nao entra em nenhum dos dois.
 */
@Schema(description = "Faturamento do periodo")
public record FaturamentoResponse(
        @Schema(description = "Soma do valorTotal das OS ENTREGUE, pela dataEntregue")
        BigDecimal faturamentoRealizado,

        @Schema(description = "Soma do valorTotal das OS EM_ANDAMENTO e CONCLUIDA, pela dataEntrada")
        BigDecimal faturamentoEmAberto,

        @Schema(description = "Faturamento realizado dividido pelas OS entregues; zero quando nao houve entrega")
        BigDecimal ticketMedio,

        long ordensEntregues,

        @Schema(description = "OS do periodo cujo valorTotal foi fixado a mao (desconto, preco fechado)")
        long ordensComValorManual) {

    private static final int CASAS_DECIMAIS = 2;

    /**
     * Concentra os dois tratamentos que a agregacao exige.
     *
     * <p>
     * O primeiro: sum() sem nenhuma linha devolve null, nao zero — acontece sempre
     * que o periodo nao teve movimento, ou seja, no primeiro uso do painel. Um
     * coalesce no JPQL resolveria no banco, mas o literal 0 entraria como Integer e
     * a coercao para BigDecimal na projecao por construtor e uma surpresa gratuita.
     *
     * <p>
     * O segundo: divide sem escala explicita estoura ArithmeticException em dizima
     * (100,00 dividido por 3), e o divisor pode ser zero.
     */
    public static FaturamentoResponse calcular(BigDecimal realizado, BigDecimal emAberto,
            long ordensEntregues, long ordensComValorManual) {

        BigDecimal faturamentoRealizado = zeroSeNulo(realizado);
        BigDecimal ticketMedio = ordensEntregues > 0
                ? faturamentoRealizado.divide(BigDecimal.valueOf(ordensEntregues), CASAS_DECIMAIS,
                        RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new FaturamentoResponse(faturamentoRealizado, zeroSeNulo(emAberto), ticketMedio,
                ordensEntregues, ordensComValorManual);
    }

    private static BigDecimal zeroSeNulo(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }
}
