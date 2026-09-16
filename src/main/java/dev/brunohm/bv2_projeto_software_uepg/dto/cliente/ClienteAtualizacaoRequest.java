package dev.brunohm.bv2_projeto_software_uepg.dto.cliente;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** A situacao (ativo) nao entra aqui: muda pelos PATCH /ativar e /desativar. */
public record ClienteAtualizacaoRequest(

        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 50, message = "O nome deve ter no máximo 50 caracteres")
        String nome,

        @NotBlank(message = "O telefone é obrigatório")
        @Size(max = 13, message = "O telefone deve ter no máximo 13 caracteres")
        @Pattern(regexp = "\\d{10,13}", message = "O telefone deve conter apenas dígitos (10 a 13)")
        String telefone) {
}
