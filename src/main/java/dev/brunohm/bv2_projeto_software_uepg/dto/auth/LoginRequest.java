package dev.brunohm.bv2_projeto_software_uepg.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank(message = "O e-mail e obrigatorio")
        @Email(message = "E-mail em formato invalido")
        @Size(max = 50, message = "O e-mail deve ter no maximo 50 caracteres")
        String email,

        @NotBlank(message = "A senha e obrigatoria")
        String senha) {
}
