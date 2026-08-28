package dev.brunohm.bv2_projeto_software_uepg.dto.ordemservico;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

public record OrdemServicoCriacaoRequest(

        @Schema(description = "Dono da ordem de servico. Se omitido, assume o cliente do usuario autenticado. "
                + "Obrigatorio para o ADMIN, que nao possui cadastro de cliente.")
        Long clienteId,

        @Size(max = 500, message = "A observacao deve ter no maximo 500 caracteres")
        String observacao,

        @Schema(description = "Data de entrada do equipamento na assistencia. Se omitida, assume a data de hoje.")
        @PastOrPresent(message = "A data de entrada nao pode ser futura")
        LocalDate dataEntrada) {
}
