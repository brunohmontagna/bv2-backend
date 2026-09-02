package dev.brunohm.bv2_projeto_software_uepg.dto.usuario;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * O papel nao entra aqui: todo usuario criado pela API nasce ADMIN. MASTER e a
 * equipe desenvolvedora, semeada na migration, e nao e atribuivel por requisicao.
 */
@Schema(description = "Dados de acesso de um novo usuario. Criado sempre com papel ADMIN.")
public record UsuarioCriacaoRequest(

        @NotBlank(message = "O nome e obrigatorio")
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
