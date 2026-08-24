package dev.brunohm.bv2_projeto_software_uepg.controller;

import java.net.URI;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@Tag(name = "Clientes", description = "Cadastro de clientes da assistencia tecnica")
public class ClienteController {

    private final ClienteService clienteService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cadastra um cliente junto com seu usuario de acesso")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cliente criado"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos"),
            @ApiResponse(responseCode = "409", description = "E-mail ja cadastrado")
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
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lista clientes de forma paginada, com filtro por nome e situacao")
    public ResponseEntity<PaginaResponse<ClienteResponse>> listar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) Boolean ativo,
            @PageableDefault(size = 20, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(clienteService.listar(nome, ativo, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um cliente pelo id (ADMIN, ou o proprio cliente)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
            @ApiResponse(responseCode = "403", description = "Sem permissao sobre este cadastro"),
            @ApiResponse(responseCode = "404", description = "Cliente nao encontrado")
    })
    public ResponseEntity<ClienteResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza nome e telefone (ADMIN, ou o proprio cliente)")
    public ResponseEntity<ClienteResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody ClienteAtualizacaoRequest request) {
        return ResponseEntity.ok(clienteService.atualizar(id, request));
    }

    @PatchMapping("/{id}/ativar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reativa o cliente (idempotente)")
    public ResponseEntity<ClienteResponse> ativar(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.alterarSituacao(id, true));
    }

    @PatchMapping("/{id}/desativar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Inativa o cliente sem apagar o historico (idempotente)")
    public ResponseEntity<ClienteResponse> desativar(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.alterarSituacao(id, false));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove definitivamente o cliente e seu usuario")
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
