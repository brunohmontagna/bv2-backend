package dev.brunohm.bv2_projeto_software_uepg.dto.ordemservico;

import jakarta.validation.constraints.Size;

/**
 * Equipamento e servico nao entram aqui: junto com a OS eles formam a chave
 * unica do item (uq_item_os_os_equip_servico). Trocar um deles e remover o item
 * e adicionar outro.
 */
public record ItemOsAtualizacaoRequest(

        @Size(max = 500, message = "A observacao deve ter no maximo 500 caracteres")
        String observacao) {
}
