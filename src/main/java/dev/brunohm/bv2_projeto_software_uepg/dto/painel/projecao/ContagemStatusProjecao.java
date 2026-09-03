package dev.brunohm.bv2_projeto_software_uepg.dto.painel.projecao;

import dev.brunohm.bv2_projeto_software_uepg.domain.enums.StatusOs;

/**
 * Linha crua do "group by status". Vive fora de dto/painel porque nao e contrato
 * HTTP: o JSON entrega um mapa com os quatro status, e o group by so devolve os
 * que tem ocorrencia.
 */
public record ContagemStatusProjecao(StatusOs status, Long quantidade) {
}
