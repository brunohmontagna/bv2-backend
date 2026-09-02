package dev.brunohm.bv2_projeto_software_uepg.dto.ordemservico;

import java.math.BigDecimal;

import dev.brunohm.bv2_projeto_software_uepg.domain.entity.Servico;

/**
 * Recorte minimo do servico para aninhar no item da OS. O valor entra porque e
 * o que compoe o valorTotal; contadorUso e ativo sao estado do catalogo e nao
 * dizem respeito ao item.
 */
public record ServicoResumoResponse(
        Long id,
        String nome,
        BigDecimal valor) {

    public static ServicoResumoResponse fromEntity(Servico servico) {
        return new ServicoResumoResponse(servico.getId(), servico.getNome(), servico.getValor());
    }
}
