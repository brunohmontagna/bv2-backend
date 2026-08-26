package dev.brunohm.bv2_projeto_software_uepg.dto.cliente;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Dados de acesso criados junto com o cliente (Cliente exige um Usuario). */
public record UsuarioCriacaoRequest(

        @NotBlank(message = "O nome do usuario e obrigatorio")
        @Size(max = 50, message = "O nome deve ter no maximo 50 caracteres")
        String nome,

        @NotBlank(message = "O e-mail e obrigatorio")
        @Email(message = "E-mail em formato invalido")
        @Size(max = 50, message = "O e-mail deve ter no maximo 50 caracteres")
        String email,

        // Limite de 72 porque o BCrypt trunca silenciosamente acima disso.
        @NotBlank(message = "A senha e obrigatoria")
        @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres")
        String senha) {
}
