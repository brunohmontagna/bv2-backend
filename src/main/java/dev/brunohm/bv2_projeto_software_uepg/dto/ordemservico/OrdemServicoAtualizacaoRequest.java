package dev.brunohm.bv2_projeto_software_uepg.dto.ordemservico;

import jakarta.validation.constraints.Size;

/**
 * Cliente, status e datas nao entram aqui: o dono e imutavel e as datas sao
 * derivadas das transicoes de status (PATCH /concluir, /entregar, /cancelar).
 */
public record OrdemServicoAtualizacaoRequest(

        @Size(max = 500, message = "A observacao deve ter no maximo 500 caracteres")
        String observacao) {
}
