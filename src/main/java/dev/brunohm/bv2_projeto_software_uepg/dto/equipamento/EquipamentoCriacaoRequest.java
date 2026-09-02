package dev.brunohm.bv2_projeto_software_uepg.dto.equipamento;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EquipamentoCriacaoRequest(

        @Schema(description = "Cliente da M2 dono do equipamento. Precisa estar ativo.")
        @NotNull(message = "O cliente e obrigatorio")
        Long clienteId,

        @NotNull(message = "A marca e obrigatoria")
        Long marcaId,

        @NotBlank(message = "O nome e obrigatorio")
        @Size(max = 50, message = "O nome deve ter no maximo 50 caracteres")
        String nome) {
}
