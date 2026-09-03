package dev.brunohm.bv2_projeto_software_uepg.dto.painel;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Um ponto do grafico: quantas OS entraram e quanto foi faturado no mes. As duas
 * metricas usam datas diferentes de proposito — entrada por dataEntrada, faturamento
 * por dataEntregue —, entao o mesmo mes mostra o que entrou e o que saiu.
 */
@Schema(description = "Um mes da serie temporal")
public record SerieMensalResponse(
        @Schema(description = "Mes no formato AAAA-MM", example = "2026-07") String mes,
        long ordensAbertas,
        BigDecimal faturamento) {

    private static final DateTimeFormatter FORMATO_MES = DateTimeFormatter.ofPattern("uuuu-MM");

    /**
     * O mes vai como String ja formatada em vez de YearMonth: o Jackson serializa
     * YearMonth como objeto ou array conforme o modulo de tempo registrado, e o
     * formato do grafico nao deveria depender dessa configuracao.
     */
    public static SerieMensalResponse de(YearMonth mes, long ordensAbertas, BigDecimal faturamento) {
        return new SerieMensalResponse(
                mes.format(FORMATO_MES),
                ordensAbertas,
                faturamento != null ? faturamento : BigDecimal.ZERO);
    }
}
