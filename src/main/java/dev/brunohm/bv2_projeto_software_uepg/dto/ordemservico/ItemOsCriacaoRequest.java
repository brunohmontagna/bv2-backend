package dev.brunohm.bv2_projeto_software_uepg.dto.ordemservico;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ItemOsCriacaoRequest(

        @Schema(description = "Equipamento a ser atendido. Precisa pertencer ao mesmo cliente da ordem de serviço.")
        @NotNull(message = "O equipamento é obrigatório")
        Long equipamentoId,

        @Schema(description = "Serviço a ser executado. Precisa estar ativo no catálogo.")
        @NotNull(message = "O serviço é obrigatório")
        Long servicoId,

        @Size(max = 500, message = "A observação deve ter no máximo 500 caracteres")
        String observacao) {
}
