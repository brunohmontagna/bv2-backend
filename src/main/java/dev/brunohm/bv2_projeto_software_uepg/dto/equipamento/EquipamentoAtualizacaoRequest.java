package dev.brunohm.bv2_projeto_software_uepg.dto.equipamento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * O cliente dono nao esta aqui de proposito: e imutavel. Transferir um
 * equipamento que ja tem historico de ordens de servico nao e uma edicao de
 * cadastro.
 */
public record EquipamentoAtualizacaoRequest(

        @NotNull(message = "A marca e obrigatoria")
        Long marcaId,

        @NotBlank(message = "O nome e obrigatorio")
        @Size(max = 50, message = "O nome deve ter no maximo 50 caracteres")
        String nome) {
}
