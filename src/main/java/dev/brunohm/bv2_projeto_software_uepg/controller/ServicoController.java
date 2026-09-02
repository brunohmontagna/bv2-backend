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
import dev.brunohm.bv2_projeto_software_uepg.dto.servico.ServicoRequest;
import dev.brunohm.bv2_projeto_software_uepg.dto.servico.ServicoResponse;
import dev.brunohm.bv2_projeto_software_uepg.service.ServicoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/servicos")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Servicos", description = "Catalogo de servicos prestados pela M2. Leitura e escrita para qualquer usuario autenticado")
public class ServicoController {

    private final ServicoService servicoService;

    @PostMapping
    @Operation(summary = "Cadastra um servico (nome + valor precisam ser unicos no catalogo)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Servico criado"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos"),
            @ApiResponse(responseCode = "409", description = "Ja existe um servico com esse nome e valor")
    })
    public ResponseEntity<ServicoResponse> criar(@Valid @RequestBody ServicoRequest request) {
        ServicoResponse criado = servicoService.criar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criado.id())
                .toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @GetMapping
    @Operation(summary = "Lista servicos de forma paginada, com filtro por nome e situacao")
    public ResponseEntity<PaginaResponse<ServicoResponse>> listar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) Boolean ativo,
            @PageableDefault(size = 20, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(servicoService.listar(nome, ativo, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um servico pelo id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Servico encontrado"),
            @ApiResponse(responseCode = "404", description = "Servico nao encontrado")
    })
    public ResponseEntity<ServicoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(servicoService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza nome, descricao e valor do servico")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Servico atualizado"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos"),
            @ApiResponse(responseCode = "404", description = "Servico nao encontrado"),
            @ApiResponse(responseCode = "409", description = "Ja existe outro servico com esse nome e valor")
    })
    public ResponseEntity<ServicoResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody ServicoRequest request) {
        return ResponseEntity.ok(servicoService.atualizar(id, request));
    }

    @PatchMapping("/{id}/ativar")
    @Operation(summary = "Reativa o servico (idempotente)")
    public ResponseEntity<ServicoResponse> ativar(@PathVariable Long id) {
        return ResponseEntity.ok(servicoService.alterarSituacao(id, true));
    }

    @PatchMapping("/{id}/desativar")
    @Operation(summary = "Inativa o servico sem apagar o historico (idempotente)")
    public ResponseEntity<ServicoResponse> desativar(@PathVariable Long id) {
        return ResponseEntity.ok(servicoService.alterarSituacao(id, false));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove definitivamente o servico")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Servico removido"),
            @ApiResponse(responseCode = "404", description = "Servico nao encontrado"),
            @ApiResponse(responseCode = "409", description = "Servico usado em ordens de servico")
    })
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        servicoService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
