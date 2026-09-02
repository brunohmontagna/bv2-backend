package dev.brunohm.bv2_projeto_software_uepg.dto.ordemservico;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

/**
 * Define ou reseta o valorTotal a mao. Enviar um valor congela o total (para de
 * recalcular pelos itens); enviar null volta ao automatico (soma dos servicos).
 */
public record OrdemServicoValorTotalRequest(

        @Schema(description = "Novo valor total. Null reseta para o modo automatico (soma dos itens).")
        @DecimalMin(value = "0.00", message = "O valor total nao pode ser negativo")
        @Digits(integer = 7, fraction = 2, message = "O valor total deve ter no maximo 7 inteiros e 2 decimais")
        BigDecimal valorTotal) {
}
