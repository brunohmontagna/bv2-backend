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
import dev.brunohm.bv2_projeto_software_uepg.dto.marca.MarcaRequest;
import dev.brunohm.bv2_projeto_software_uepg.dto.marca.MarcaResponse;
import dev.brunohm.bv2_projeto_software_uepg.service.MarcaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/marcas")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Marcas", description = "Catalogo de marcas dos equipamentos. Leitura e escrita para qualquer usuario autenticado")
public class MarcaController {

    private final MarcaService marcaService;

    @PostMapping
    @Operation(summary = "Cadastra uma marca")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Marca criada"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos"),
            @ApiResponse(responseCode = "409", description = "Nome ja cadastrado")
    })
    public ResponseEntity<MarcaResponse> criar(@Valid @RequestBody MarcaRequest request) {
        MarcaResponse criada = marcaService.criar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criada.id())
                .toUri();
        return ResponseEntity.created(location).body(criada);
    }

    @GetMapping
    @Operation(summary = "Lista marcas de forma paginada, com filtro por nome")
    public ResponseEntity<PaginaResponse<MarcaResponse>> listar(
            @RequestParam(required = false) String nome,
            @PageableDefault(size = 20, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(marcaService.listar(nome, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma marca pelo id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Marca encontrada"),
            @ApiResponse(responseCode = "404", description = "Marca nao encontrada")
    })
    public ResponseEntity<MarcaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(marcaService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza o nome da marca")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Marca atualizada"),
            @ApiResponse(responseCode = "404", description = "Marca nao encontrada"),
            @ApiResponse(responseCode = "409", description = "Nome ja cadastrado em outra marca")
    })
    public ResponseEntity<MarcaResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody MarcaRequest request) {
        return ResponseEntity.ok(marcaService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove a marca")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Marca removida"),
            @ApiResponse(responseCode = "404", description = "Marca nao encontrada"),
            @ApiResponse(responseCode = "409", description = "Marca possui equipamentos vinculados")
    })
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        marcaService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
