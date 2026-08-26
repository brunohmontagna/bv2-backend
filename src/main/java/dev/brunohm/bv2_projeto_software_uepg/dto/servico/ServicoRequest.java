package dev.brunohm.bv2_projeto_software_uepg.dto.servico;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Serve criacao e atualizacao. Nao expoe ativo nem contadorUso: a situacao muda
 * pelos PATCH /ativar e /desativar, e o contador e mantido pelas ordens de servico.
 */
public record ServicoRequest(

        @NotBlank(message = "O nome e obrigatorio")
        @Size(max = 50, message = "O nome deve ter no maximo 50 caracteres")
        String nome,

        @Size(max = 500, message = "A descricao deve ter no maximo 500 caracteres")
        String descricao,

        @NotNull(message = "O valor e obrigatorio")
        @DecimalMin(value = "0.00", message = "O valor nao pode ser negativo")
        @Digits(integer = 7, fraction = 2, message = "O valor deve ter no maximo 7 inteiros e 2 decimais")
        BigDecimal valor) {
}
