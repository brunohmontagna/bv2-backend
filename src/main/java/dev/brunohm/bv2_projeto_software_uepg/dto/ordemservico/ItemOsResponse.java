package dev.brunohm.bv2_projeto_software_uepg.dto.ordemservico;

import java.time.LocalDateTime;

import dev.brunohm.bv2_projeto_software_uepg.domain.entity.ItemOs;

public record ItemOsResponse(
        Long id,
        String observacao,
        LocalDateTime criadoEm,
        EquipamentoResumoResponse equipamento,
        ServicoResumoResponse servico) {

    public static ItemOsResponse fromEntity(ItemOs item) {
        return new ItemOsResponse(
                item.getId(),
                item.getObservacao(),
                item.getCriadoEm(),
                EquipamentoResumoResponse.fromEntity(item.getEquipamento()),
                ServicoResumoResponse.fromEntity(item.getServico()));
    }
}
