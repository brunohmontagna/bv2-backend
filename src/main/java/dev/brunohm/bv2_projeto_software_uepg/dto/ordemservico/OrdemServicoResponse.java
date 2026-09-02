package dev.brunohm.bv2_projeto_software_uepg.dto.ordemservico;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import dev.brunohm.bv2_projeto_software_uepg.domain.entity.OrdemServico;
import dev.brunohm.bv2_projeto_software_uepg.domain.enums.StatusOs;
import dev.brunohm.bv2_projeto_software_uepg.dto.equipamento.ClienteResumoResponse;

public record OrdemServicoResponse(
        Long id,
        String observacao,
        StatusOs status,
        LocalDate dataEntrada,
        LocalDate dataConcluida,
        LocalDate dataEntregue,
        BigDecimal valorTotal,
        Boolean valorTotalManual,
        LocalDateTime criadoEm,
        ClienteResumoResponse cliente) {

    public static OrdemServicoResponse fromEntity(OrdemServico ordemServico) {
        return new OrdemServicoResponse(
                ordemServico.getId(),
                ordemServico.getObservacao(),
                ordemServico.getStatus(),
                ordemServico.getDataEntrada(),
                ordemServico.getDataConcluida(),
                ordemServico.getDataEntregue(),
                ordemServico.getValorTotal(),
                ordemServico.getValorTotalManual(),
                ordemServico.getCriadoEm(),
                ClienteResumoResponse.fromEntity(ordemServico.getCliente()));
    }
}
