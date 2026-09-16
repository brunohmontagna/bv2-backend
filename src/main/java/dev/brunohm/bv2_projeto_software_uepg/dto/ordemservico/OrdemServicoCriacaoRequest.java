package dev.brunohm.bv2_projeto_software_uepg.dto.ordemservico;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

public record OrdemServicoCriacaoRequest(

        @Schema(description = "Cliente da M2 dono da ordem de serviço. Precisa estar ativo.")
        @NotNull(message = "O cliente é obrigatório")
        Long clienteId,

        @Size(max = 500, message = "A observação deve ter no máximo 500 caracteres")
        String observacao,

        @Schema(description = "Data de entrada do equipamento na assistência. Se omitida, assume a data de hoje.")
        @PastOrPresent(message = "A data de entrada não pode ser futura")
        LocalDate dataEntrada,

        @Schema(description = "Itens que compõem a OS (equipamento + serviço). Obrigatório: "
                + "uma OS não existe sem ao menos um serviço lançado.")
        @NotEmpty(message = "A ordem de serviço precisa ter ao menos um item")
        @Valid
        List<ItemOsCriacaoRequest> itens,

        @Schema(description = "Valor total definido à mão (desconto, preço fechado). Se enviado, "
                + "congela o valorTotal e ele deixa de ser recalculado pelos itens até um reset. "
                + "Se omitido, o valorTotal é a soma dos serviços dos itens.")
        @DecimalMin(value = "0.00", message = "O valor total não pode ser negativo")
        @Digits(integer = 7, fraction = 2, message = "O valor total deve ter no máximo 7 inteiros e 2 decimais")
        BigDecimal valorTotal) {
}
