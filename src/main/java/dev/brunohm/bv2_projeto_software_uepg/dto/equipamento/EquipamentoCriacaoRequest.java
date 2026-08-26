package dev.brunohm.bv2_projeto_software_uepg.dto.equipamento;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EquipamentoCriacaoRequest(

        @Schema(description = "Dono do equipamento. Se omitido, assume o cliente do usuario autenticado. "
                + "Obrigatorio para o ADMIN, que nao possui cadastro de cliente.")
        Long clienteId,

        @NotNull(message = "A marca e obrigatoria")
        Long marcaId,

        @NotBlank(message = "O nome e obrigatorio")
        @Size(max = 50, message = "O nome deve ter no maximo 50 caracteres")
        String nome) {
}
