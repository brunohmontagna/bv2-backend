package dev.brunohm.bv2_projeto_software_uepg.controller;

import java.net.URI;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import dev.brunohm.bv2_projeto_software_uepg.dto.PaginaResponse;
import dev.brunohm.bv2_projeto_software_uepg.dto.equipamento.EquipamentoAtualizacaoRequest;
import dev.brunohm.bv2_projeto_software_uepg.dto.equipamento.EquipamentoCriacaoRequest;
import dev.brunohm.bv2_projeto_software_uepg.dto.equipamento.EquipamentoResponse;
import dev.brunohm.bv2_projeto_software_uepg.service.EquipamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/equipamentos")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Equipamentos", description = "Equipamentos dos clientes da M2. Qualquer usuario autenticado acessa todos")
public class EquipamentoController {

    private final EquipamentoService equipamentoService;

    @PostMapping
    @Operation(summary = "Cadastra um equipamento para um cliente da M2")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Equipamento criado"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos"),
            @ApiResponse(responseCode = "404", description = "Cliente ou marca nao encontrada"),
            @ApiResponse(responseCode = "422", description = "Cliente inativo")
    })
    public ResponseEntity<EquipamentoResponse> criar(@Valid @RequestBody EquipamentoCriacaoRequest request) {
        EquipamentoResponse criado = equipamentoService.criar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criado.id())
                .toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @GetMapping
    @Operation(summary = "Lista equipamentos de forma paginada, com filtro por cliente, marca e nome")
    public ResponseEntity<PaginaResponse<EquipamentoResponse>> listar(
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Long marcaId,
            @RequestParam(required = false) String nome,
            @PageableDefault(size = 20, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(equipamentoService.listar(clienteId, marcaId, nome, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um equipamento pelo id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Equipamento encontrado"),
            @ApiResponse(responseCode = "404", description = "Equipamento nao encontrado")
    })
    public ResponseEntity<EquipamentoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(equipamentoService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza nome e marca do equipamento (o cliente dono e imutavel)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Equipamento atualizado"),
            @ApiResponse(responseCode = "404", description = "Equipamento ou marca nao encontrada")
    })
    public ResponseEntity<EquipamentoResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody EquipamentoAtualizacaoRequest request) {
        return ResponseEntity.ok(equipamentoService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove definitivamente o equipamento")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Equipamento removido"),
            @ApiResponse(responseCode = "404", description = "Equipamento nao encontrado"),
            @ApiResponse(responseCode = "409", description = "Equipamento usado em ordens de servico")
    })
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        equipamentoService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
