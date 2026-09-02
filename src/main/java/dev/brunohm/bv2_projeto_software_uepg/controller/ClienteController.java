package dev.brunohm.bv2_projeto_software_uepg.controller;

import java.net.URI;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

import dev.brunohm.bv2_projeto_software_uepg.dto.PaginaResponse;
import dev.brunohm.bv2_projeto_software_uepg.dto.cliente.ClienteAtualizacaoRequest;
import dev.brunohm.bv2_projeto_software_uepg.dto.cliente.ClienteCriacaoRequest;
import dev.brunohm.bv2_projeto_software_uepg.dto.cliente.ClienteResponse;
import dev.brunohm.bv2_projeto_software_uepg.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/clientes")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Clientes", description = "Cadastro dos clientes da M2: quem leva o equipamento para consertar. Nao fazem login")
public class ClienteController {

    private final ClienteService clienteService;

    @PostMapping
    @Operation(summary = "Cadastra um cliente da M2")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cliente criado"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos"),
                })
    public ResponseEntity<ClienteResponse> criar(@Valid @RequestBody ClienteCriacaoRequest request) {
        ClienteResponse criado = clienteService.criar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criado.id())
                .toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @GetMapping
    @Operation(summary = "Lista clientes de forma paginada, com filtro por nome e situacao")
    public ResponseEntity<PaginaResponse<ClienteResponse>> listar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) Boolean ativo,
            @PageableDefault(size = 20, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(clienteService.listar(nome, ativo, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um cliente pelo id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
            @ApiResponse(responseCode = "404", description = "Cliente nao encontrado")
    })
    public ResponseEntity<ClienteResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza nome e telefone do cliente")
    public ResponseEntity<ClienteResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody ClienteAtualizacaoRequest request) {
        return ResponseEntity.ok(clienteService.atualizar(id, request));
    }

    @PatchMapping("/{id}/ativar")
    @Operation(summary = "Reativa o cliente (idempotente)")
    public ResponseEntity<ClienteResponse> ativar(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.alterarSituacao(id, true));
    }

    @PatchMapping("/{id}/desativar")
    @Operation(summary = "Inativa o cliente sem apagar o historico (idempotente)")
    public ResponseEntity<ClienteResponse> desativar(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.alterarSituacao(id, false));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove definitivamente o cliente. Para o caso comum, prefira desativar")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Cliente removido"),
            @ApiResponse(responseCode = "404", description = "Cliente nao encontrado"),
            @ApiResponse(responseCode = "409", description = "Cliente possui vinculos e nao pode ser removido")
    })
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        clienteService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
