package dev.brunohm.bv2_projeto_software_uepg.dto.ordemservico;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

public record OrdemServicoCriacaoRequest(

        @Schema(description = "Cliente da M2 dono da ordem de servico. Precisa estar ativo.")
        @NotNull(message = "O cliente e obrigatorio")
        Long clienteId,

        @Size(max = 500, message = "A observacao deve ter no maximo 500 caracteres")
        String observacao,

        @Schema(description = "Data de entrada do equipamento na assistencia. Se omitida, assume a data de hoje.")
        @PastOrPresent(message = "A data de entrada nao pode ser futura")
        LocalDate dataEntrada,

        @Schema(description = "Itens que ja compoem a OS na abertura (equipamento + servico). "
                + "Opcional: pode-se abrir a OS vazia e lancar os itens depois.")
        @Valid
        List<ItemOsCriacaoRequest> itens,

        @Schema(description = "Valor total definido a mao (desconto, preco fechado). Se enviado, "
                + "congela o valorTotal e ele deixa de ser recalculado pelos itens ate um reset. "
                + "Se omitido, o valorTotal e a soma dos servicos dos itens.")
        @DecimalMin(value = "0.00", message = "O valor total nao pode ser negativo")
        @Digits(integer = 7, fraction = 2, message = "O valor total deve ter no maximo 7 inteiros e 2 decimais")
        BigDecimal valorTotal) {
}
