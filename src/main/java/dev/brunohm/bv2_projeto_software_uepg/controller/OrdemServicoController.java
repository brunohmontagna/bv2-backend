package dev.brunohm.bv2_projeto_software_uepg.controller;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import dev.brunohm.bv2_projeto_software_uepg.domain.enums.StatusOs;
import dev.brunohm.bv2_projeto_software_uepg.dto.PaginaResponse;
import dev.brunohm.bv2_projeto_software_uepg.dto.ordemservico.ItemOsAtualizacaoRequest;
import dev.brunohm.bv2_projeto_software_uepg.dto.ordemservico.ItemOsCriacaoRequest;
import dev.brunohm.bv2_projeto_software_uepg.dto.ordemservico.ItemOsResponse;
import dev.brunohm.bv2_projeto_software_uepg.dto.ordemservico.OrdemServicoAtualizacaoRequest;
import dev.brunohm.bv2_projeto_software_uepg.dto.ordemservico.OrdemServicoCriacaoRequest;
import dev.brunohm.bv2_projeto_software_uepg.dto.ordemservico.OrdemServicoResponse;
import dev.brunohm.bv2_projeto_software_uepg.dto.ordemservico.OrdemServicoValorTotalRequest;
import dev.brunohm.bv2_projeto_software_uepg.service.OrdemServicoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Os itens moram neste controller, e nao em um proprio: nao existem fora de uma
 * OS e so fazem sentido enderecados por ela.
 */
@RestController
@RequestMapping("/ordens-servico")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Ordens de Serviço", description = "Ordens de serviço dos clientes da M2 e seus itens. Qualquer usuário autenticado acessa todas")
public class OrdemServicoController {

    private final OrdemServicoService ordemServicoService;

    // ------------------------------------------------------------------
    // Ordem de servico
    // ------------------------------------------------------------------

    @PostMapping
    @Operation(summary = "Abre uma ordem de serviço para um cliente da M2. Pode nascer vazia, "
            + "já com uma lista de itens, e/ou com o valorTotal fixado à mão")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ordem de serviço criada. valorTotal = soma dos itens, "
                    + "ou o valor enviado à mão (que passa a valer valorTotalManual=true)"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Cliente, equipamento ou serviço (de um item) não encontrado"),
            @ApiResponse(responseCode = "409", description = "Item repetido (mesmo equipamento e serviço) na lista"),
            @ApiResponse(responseCode = "422", description = "Cliente inativo, equipamento de outro cliente, ou serviço inativo em um item")
    })
    public ResponseEntity<OrdemServicoResponse> criar(@Valid @RequestBody OrdemServicoCriacaoRequest request) {
        OrdemServicoResponse criada = ordemServicoService.criar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criada.id())
                .toUri();
        return ResponseEntity.created(location).body(criada);
    }

    @GetMapping
    @Operation(summary = "Lista ordens de serviço de forma paginada, com filtro por cliente, status e período de entrada")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de ordens de serviço"),
            @ApiResponse(responseCode = "400", description = "Parâmetro inválido (status fora do enum ou data mal formatada)"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido")
    })
    public ResponseEntity<PaginaResponse<OrdemServicoResponse>> listar(
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) StatusOs status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
            @PageableDefault(size = 20, sort = "dataEntrada", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ordemServicoService.listar(clienteId, status, dataInicio, dataFim, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma ordem de serviço pelo id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ordem de serviço encontrada"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Ordem de serviço não encontrada")
    })
    public ResponseEntity<OrdemServicoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ordemServicoService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza a observação da ordem de serviço (cliente, status e datas são imutáveis por aqui)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ordem de serviço atualizada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Ordem de serviço não encontrada"),
            @ApiResponse(responseCode = "422", description = "Ordem de serviço não está EM_ANDAMENTO")
    })
    public ResponseEntity<OrdemServicoResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody OrdemServicoAtualizacaoRequest request) {
        return ResponseEntity.ok(ordemServicoService.atualizar(id, request));
    }

    @PatchMapping("/{id}/concluir")
    @Operation(summary = "Marca a ordem de serviço como CONCLUIDA e registra a dataConcluida (idempotente)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ordem de serviço concluída"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Ordem de serviço não encontrada"),
            @ApiResponse(responseCode = "422", description = "Ordem de serviço já entregue ou cancelada")
    })
    public ResponseEntity<OrdemServicoResponse> concluir(@PathVariable Long id) {
        return ResponseEntity.ok(ordemServicoService.concluir(id));
    }

    @PatchMapping("/{id}/entregar")
    @Operation(summary = "Marca a ordem de serviço como ENTREGUE e registra a dataEntregue (exige CONCLUIDA antes; idempotente)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ordem de serviço entregue"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Ordem de serviço não encontrada"),
            @ApiResponse(responseCode = "422", description = "Ordem de serviço ainda EM_ANDAMENTO, ou cancelada")
    })
    public ResponseEntity<OrdemServicoResponse> entregar(@PathVariable Long id) {
        return ResponseEntity.ok(ordemServicoService.entregar(id));
    }

    @PatchMapping("/{id}/cancelar")
    @Operation(summary = "Cancela a ordem de serviço (idempotente)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ordem de serviço cancelada"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Ordem de serviço não encontrada"),
            @ApiResponse(responseCode = "422", description = "Ordem de serviço já entregue")
    })
    public ResponseEntity<OrdemServicoResponse> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(ordemServicoService.cancelar(id));
    }

    @PatchMapping("/{id}/valor-total")
    @Operation(summary = "Fixa o valorTotal à mão (congela, valorTotalManual=true) ou reseta para o "
            + "automático enviando valorTotal null (volta a somar os itens)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Valor total atualizado"),
            @ApiResponse(responseCode = "400", description = "Valor inválido (negativo ou fora da escala)"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Ordem de serviço não encontrada"),
            @ApiResponse(responseCode = "422", description = "Ordem de serviço não está EM_ANDAMENTO")
    })
    public ResponseEntity<OrdemServicoResponse> definirValorTotal(
            @PathVariable Long id,
            @Valid @RequestBody OrdemServicoValorTotalRequest request) {
        return ResponseEntity.ok(ordemServicoService.definirValorTotal(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove definitivamente a ordem de serviço (precisa estar sem itens)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Ordem de serviço removida"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Ordem de serviço não encontrada"),
            @ApiResponse(responseCode = "409", description = "Vínculos impedem a exclusão (notificações)"),
            @ApiResponse(responseCode = "422", description = "Ordem de serviço ainda possui itens")
    })
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        ordemServicoService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------
    // Itens da ordem de servico
    // ------------------------------------------------------------------

    @GetMapping("/{id}/itens")
    @Operation(summary = "Lista os itens que compõem a ordem de serviço (sem paginação)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Itens da ordem de serviço"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Ordem de serviço não encontrada")
    })
    public ResponseEntity<List<ItemOsResponse>> listarItens(@PathVariable Long id) {
        return ResponseEntity.ok(ordemServicoService.listarItens(id));
    }

    @PostMapping("/{id}/itens")
    @Operation(summary = "Adiciona um item (equipamento + serviço) e recalcula o valorTotal da ordem de serviço")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Item adicionado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Ordem de serviço, equipamento ou serviço não encontrado"),
            @ApiResponse(responseCode = "409", description = "Este serviço já foi lançado para este equipamento na OS"),
            @ApiResponse(responseCode = "422", description = "OS não está EM_ANDAMENTO, equipamento de outro cliente, ou serviço inativo")
    })
    public ResponseEntity<ItemOsResponse> adicionarItem(
            @PathVariable Long id,
            @Valid @RequestBody ItemOsCriacaoRequest request) {
        ItemOsResponse criado = ordemServicoService.adicionarItem(id, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{itemId}")
                .buildAndExpand(criado.id())
                .toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @PutMapping("/{id}/itens/{itemId}")
    @Operation(summary = "Atualiza a observação do item (equipamento e serviço são imutáveis: formam a chave do item)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item atualizado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Ordem de serviço ou item não encontrado"),
            @ApiResponse(responseCode = "422", description = "Ordem de serviço não está EM_ANDAMENTO")
    })
    public ResponseEntity<ItemOsResponse> atualizarItem(
            @PathVariable Long id,
            @PathVariable Long itemId,
            @Valid @RequestBody ItemOsAtualizacaoRequest request) {
        return ResponseEntity.ok(ordemServicoService.atualizarItem(id, itemId, request));
    }

    @DeleteMapping("/{id}/itens/{itemId}")
    @Operation(summary = "Remove o item e recalcula o valorTotal da ordem de serviço")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Item removido"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido"),
            @ApiResponse(responseCode = "404", description = "Ordem de serviço ou item não encontrado"),
            @ApiResponse(responseCode = "422", description = "Ordem de serviço não está EM_ANDAMENTO")
    })
    public ResponseEntity<Void> removerItem(@PathVariable Long id, @PathVariable Long itemId) {
        ordemServicoService.removerItem(id, itemId);
        return ResponseEntity.noContent().build();
    }
}
