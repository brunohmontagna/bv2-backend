package dev.brunohm.bv2_projeto_software_uepg.dto.marca;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Serve criacao e atualizacao: o recurso tem um unico campo editavel. */
public record MarcaRequest(

        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 50, message = "O nome deve ter no máximo 50 caracteres")
        String nome) {
}
