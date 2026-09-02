package dev.brunohm.bv2_projeto_software_uepg.dto.ordemservico;

import dev.brunohm.bv2_projeto_software_uepg.domain.entity.Equipamento;

/**
 * Recorte minimo do equipamento para aninhar no item da OS. Nao reaproveita o
 * EquipamentoResponse, que carrega cliente e marca e exigiria mais dois joins
 * por item so para repetir o cliente que ja aparece na propria OS.
 */
public record EquipamentoResumoResponse(
        Long id,
        String nome) {

    public static EquipamentoResumoResponse fromEntity(Equipamento equipamento) {
        return new EquipamentoResumoResponse(equipamento.getId(), equipamento.getNome());
    }
}
