package dev.brunohm.bv2_projeto_software_uepg.dto.servico;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import dev.brunohm.bv2_projeto_software_uepg.domain.entity.Servico;

public record ServicoResponse(
        Long id,
        String nome,
        String descricao,
        BigDecimal valor,
        Integer contadorUso,
        Boolean ativo,
        LocalDateTime criadoEm) {

    public static ServicoResponse fromEntity(Servico servico) {
        return new ServicoResponse(
                servico.getId(),
                servico.getNome(),
                servico.getDescricao(),
                servico.getValor(),
                servico.getContadorUso(),
                servico.getAtivo(),
                servico.getCriadoEm());
    }
}
