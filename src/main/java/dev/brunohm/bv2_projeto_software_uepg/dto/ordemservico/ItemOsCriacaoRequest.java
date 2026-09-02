package dev.brunohm.bv2_projeto_software_uepg.dto.ordemservico;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ItemOsCriacaoRequest(

        @Schema(description = "Equipamento a ser atendido. Precisa pertencer ao mesmo cliente da ordem de servico.")
        @NotNull(message = "O equipamento e obrigatorio")
        Long equipamentoId,

        @Schema(description = "Servico a ser executado. Precisa estar ativo no catalogo.")
        @NotNull(message = "O servico e obrigatorio")
        Long servicoId,

        @Size(max = 500, message = "A observacao deve ter no maximo 500 caracteres")
        String observacao) {
}
