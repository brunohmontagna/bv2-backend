package dev.brunohm.bv2_projeto_software_uepg.dto.painel;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Janela efetivamente analisada. Vai na resposta mesmo quando o cliente nao
 * informou nada: o padrao e resolvido no servidor, e sem o eco o front nao teria
 * como rotular o que esta exibindo.
 */
@Schema(description = "Periodo analisado, inclusivo nas duas pontas")
public record PeriodoResponse(LocalDate dataInicio, LocalDate dataFim) {
}
