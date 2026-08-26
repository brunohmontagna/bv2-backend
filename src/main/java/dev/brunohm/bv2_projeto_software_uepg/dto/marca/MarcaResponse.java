package dev.brunohm.bv2_projeto_software_uepg.dto.marca;

import java.time.LocalDateTime;

import dev.brunohm.bv2_projeto_software_uepg.domain.entity.Marca;

public record MarcaResponse(
        Long id,
        String nome,
        LocalDateTime criadoEm) {

    public static MarcaResponse fromEntity(Marca marca) {
        return new MarcaResponse(
                marca.getId(),
                marca.getNome(),
                marca.getCriadoEm());
    }
}
