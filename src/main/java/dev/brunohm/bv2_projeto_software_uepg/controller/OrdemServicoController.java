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
 * OS e compartilham com ela a mesma checagem de posse.
 */
@RestController
@RequestMapping("/ordens-servico")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Ordens de Servico", description = "Ordens de servico e seus itens. O ADMIN acessa todas; o cliente, apenas as suas")
public class OrdemServicoController {

    private final OrdemServicoService ordemServicoService;

    // ------------------------------------------------------------------
    // Ordem de servico
    // ------------------------------------------------------------------

    @PostMapping
    @Operation(summary = "Abre uma ordem de servico (o cliente pode omitir o clienteId para abrir no proprio nome)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ordem de servico criada, com valorTotal zerado"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou invalido"),
            @ApiResponse(responseCode = "403", description = "Sem permissao sobre este cliente"),
            @ApiResponse(responseCode = "404", description = "Cliente nao encontrado"),
            @ApiResponse(responseCode = "422", description = "Cliente inativo, ou usuario sem cadastro de cliente")
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
    @Operation(summary = "Lista ordens de servico de forma paginada, com filtro por cliente, status e periodo de entrada")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina de ordens de servico. O cliente so recebe as suas, mesmo informando outro clienteId"),
            @ApiResponse(responseCode = "400", description = "Parametro invalido (status fora do enum ou data mal formatada)"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou invalido")
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
    @Operation(summary = "Busca uma ordem de servico pelo id (ADMIN, ou o cliente dono)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ordem de servico encontrada"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou invalido"),
            @ApiResponse(responseCode = "403", description = "Sem permissao sobre esta ordem de servico"),
            @ApiResponse(responseCode = "404", description = "Ordem de servico nao encontrada")
    })
    public ResponseEntity<OrdemServicoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ordemServicoService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza a observacao da ordem de servico (cliente, status e datas sao imutaveis por aqui)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ordem de servico atualizada"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou invalido"),
            @ApiResponse(responseCode = "403", description = "Sem permissao sobre esta ordem de servico"),
            @ApiResponse(responseCode = "404", description = "Ordem de servico nao encontrada"),
            @ApiResponse(responseCode = "422", description = "Ordem de servico nao esta EM_ANDAMENTO")
    })
    public ResponseEntity<OrdemServicoResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody OrdemServicoAtualizacaoRequest request) {
        return ResponseEntity.ok(ordemServicoService.atualizar(id, request));
    }

    @PatchMapping("/{id}/concluir")
    @Operation(summary = "Marca a ordem de servico como CONCLUIDA e registra a dataConcluida (idempotente)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ordem de servico concluida"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou invalido"),
            @ApiResponse(responseCode = "403", description = "Sem permissao sobre esta ordem de servico"),
            @ApiResponse(responseCode = "404", description = "Ordem de servico nao encontrada"),
            @ApiResponse(responseCode = "422", description = "Ordem de servico ja entregue ou cancelada")
    })
    public ResponseEntity<OrdemServicoResponse> concluir(@PathVariable Long id) {
        return ResponseEntity.ok(ordemServicoService.concluir(id));
    }

    @PatchMapping("/{id}/entregar")
    @Operation(summary = "Marca a ordem de servico como ENTREGUE e registra a dataEntregue (exige CONCLUIDA antes; idempotente)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ordem de servico entregue"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou invalido"),
            @ApiResponse(responseCode = "403", description = "Sem permissao sobre esta ordem de servico"),
            @ApiResponse(responseCode = "404", description = "Ordem de servico nao encontrada"),
            @ApiResponse(responseCode = "422", description = "Ordem de servico ainda EM_ANDAMENTO, ou cancelada")
    })
    public ResponseEntity<OrdemServicoResponse> entregar(@PathVariable Long id) {
        return ResponseEntity.ok(ordemServicoService.entregar(id));
    }

    @PatchMapping("/{id}/cancelar")
    @Operation(summary = "Cancela a ordem de servico (idempotente)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ordem de servico cancelada"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou invalido"),
            @ApiResponse(responseCode = "403", description = "Sem permissao sobre esta ordem de servico"),
            @ApiResponse(responseCode = "404", description = "Ordem de servico nao encontrada"),
            @ApiResponse(responseCode = "422", description = "Ordem de servico ja entregue")
    })
    public ResponseEntity<OrdemServicoResponse> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(ordemServicoService.cancelar(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove definitivamente a ordem de servico (precisa estar sem itens)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Ordem de servico removida"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou invalido"),
            @ApiResponse(responseCode = "403", description = "Sem permissao sobre esta ordem de servico"),
            @ApiResponse(responseCode = "404", description = "Ordem de servico nao encontrada"),
            @ApiResponse(responseCode = "409", description = "Vinculos impedem a exclusao (notificacoes)"),
            @ApiResponse(responseCode = "422", description = "Ordem de servico ainda possui itens")
    })
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        ordemServicoService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------
    // Itens da ordem de servico
    // ------------------------------------------------------------------

    @GetMapping("/{id}/itens")
    @Operation(summary = "Lista os itens que compoem a ordem de servico (sem paginacao)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Itens da ordem de servico"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou invalido"),
            @ApiResponse(responseCode = "403", description = "Sem permissao sobre esta ordem de servico"),
            @ApiResponse(responseCode = "404", description = "Ordem de servico nao encontrada")
    })
    public ResponseEntity<List<ItemOsResponse>> listarItens(@PathVariable Long id) {
        return ResponseEntity.ok(ordemServicoService.listarItens(id));
    }

    @PostMapping("/{id}/itens")
    @Operation(summary = "Adiciona um item (equipamento + servico) e recalcula o valorTotal da ordem de servico")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Item adicionado"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou invalido"),
            @ApiResponse(responseCode = "403", description = "Sem permissao sobre esta ordem de servico"),
            @ApiResponse(responseCode = "404", description = "Ordem de servico, equipamento ou servico nao encontrado"),
            @ApiResponse(responseCode = "409", description = "Este servico ja foi lancado para este equipamento na OS"),
            @ApiResponse(responseCode = "422", description = "OS nao esta EM_ANDAMENTO, equipamento de outro cliente, ou servico inativo")
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
    @Operation(summary = "Atualiza a observacao do item (equipamento e servico sao imutaveis: formam a chave do item)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item atualizado"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou invalido"),
            @ApiResponse(responseCode = "403", description = "Sem permissao sobre esta ordem de servico"),
            @ApiResponse(responseCode = "404", description = "Ordem de servico ou item nao encontrado"),
            @ApiResponse(responseCode = "422", description = "Ordem de servico nao esta EM_ANDAMENTO")
    })
    public ResponseEntity<ItemOsResponse> atualizarItem(
            @PathVariable Long id,
            @PathVariable Long itemId,
            @Valid @RequestBody ItemOsAtualizacaoRequest request) {
        return ResponseEntity.ok(ordemServicoService.atualizarItem(id, itemId, request));
    }

    @DeleteMapping("/{id}/itens/{itemId}")
    @Operation(summary = "Remove o item e recalcula o valorTotal da ordem de servico")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Item removido"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou invalido"),
            @ApiResponse(responseCode = "403", description = "Sem permissao sobre esta ordem de servico"),
            @ApiResponse(responseCode = "404", description = "Ordem de servico ou item nao encontrado"),
            @ApiResponse(responseCode = "422", description = "Ordem de servico nao esta EM_ANDAMENTO")
    })
    public ResponseEntity<Void> removerItem(@PathVariable Long id, @PathVariable Long itemId) {
        ordemServicoService.removerItem(id, itemId);
        return ResponseEntity.noContent().build();
    }
}
