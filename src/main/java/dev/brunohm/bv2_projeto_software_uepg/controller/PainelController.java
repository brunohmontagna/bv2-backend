package dev.brunohm.bv2_projeto_software_uepg.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.brunohm.bv2_projeto_software_uepg.dto.painel.PainelResponse;
import dev.brunohm.bv2_projeto_software_uepg.service.PainelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/painel")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Painel", description = "Indicadores consolidados da operacao. Leitura para qualquer usuario autenticado")
public class PainelController {

    private final PainelService painelService;

    /*
     * Sem @PreAuthorize: MASTER e ADMIN enxergam o painel por igual. O unico 403 da
     * API continua sendo o ADMIN no cadastro de usuarios.
     */
    @GetMapping
    @Operation(summary = "Consolida resumo, faturamento, serie mensal e rankings do periodo "
            + "(padrao: ultimos 30 dias). O periodo e inclusivo nas duas pontas e cada metrica "
            + "usa a data do proprio evento: entrada e execucao por dataEntrada, faturamento "
            + "realizado e entrega por dataEntregue")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Indicadores do periodo; periodo sem movimento devolve tudo zerado"),
            @ApiResponse(responseCode = "400", description = "Data mal formatada (use AAAA-MM-DD)"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou invalido"),
            @ApiResponse(responseCode = "422", description = "Data inicial posterior a data final")
    })
    public ResponseEntity<PainelResponse> consultar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        return ResponseEntity.ok(painelService.consultar(dataInicio, dataFim));
    }
}
