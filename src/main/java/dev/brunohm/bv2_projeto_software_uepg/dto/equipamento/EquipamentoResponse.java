package dev.brunohm.bv2_projeto_software_uepg.dto.equipamento;

import java.time.LocalDateTime;

import dev.brunohm.bv2_projeto_software_uepg.domain.entity.Equipamento;
import dev.brunohm.bv2_projeto_software_uepg.dto.marca.MarcaResponse;

public record EquipamentoResponse(
        Long id,
        String nome,
        LocalDateTime criadoEm,
        ClienteResumoResponse cliente,
        MarcaResponse marca) {

    public static EquipamentoResponse fromEntity(Equipamento equipamento) {
        return new EquipamentoResponse(
                equipamento.getId(),
                equipamento.getNome(),
                equipamento.getCriadoEm(),
                ClienteResumoResponse.fromEntity(equipamento.getCliente()),
                MarcaResponse.fromEntity(equipamento.getMarca()));
    }
}
