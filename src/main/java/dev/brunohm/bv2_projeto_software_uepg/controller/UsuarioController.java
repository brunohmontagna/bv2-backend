package dev.brunohm.bv2_projeto_software_uepg.controller;

import java.net.URI;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import dev.brunohm.bv2_projeto_software_uepg.dto.usuario.UsuarioAtualizacaoRequest;
import dev.brunohm.bv2_projeto_software_uepg.dto.usuario.UsuarioCriacaoRequest;
import dev.brunohm.bv2_projeto_software_uepg.dto.usuario.UsuarioResponse;
import dev.brunohm.bv2_projeto_software_uepg.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Unico recurso restrito por papel. As demais rotas da API sao abertas a qualquer
 * usuario autenticado, porque MASTER e ADMIN operam o sistema por igual.
 */
@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Usuarios", description = "Cadastro dos usuarios do sistema. Restrito ao MASTER, "
        + "exceto /usuarios/eu, que qualquer autenticado usa sobre o proprio registro")
public class UsuarioController {

    private final UsuarioService usuarioService;

    // ------------------------------------------------------------------
    // Proprio usuario: aberto a qualquer autenticado
    // ------------------------------------------------------------------

    @GetMapping("/eu")
    @Operation(summary = "Devolve o usuario autenticado (o id vem do token, nao do path)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario autenticado"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou invalido")
    })
    public ResponseEntity<UsuarioResponse> buscarAutenticado() {
        return ResponseEntity.ok(usuarioService.buscarAutenticado());
    }

    @PutMapping("/eu")
    @Operation(summary = "Atualiza nome, e-mail e senha do proprio usuario (papel e situacao sao imutaveis)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario atualizado"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou invalido"),
            @ApiResponse(responseCode = "409", description = "E-mail ja cadastrado em outro usuario")
    })
    public ResponseEntity<UsuarioResponse> atualizarAutenticado(
            @Valid @RequestBody UsuarioAtualizacaoRequest request) {
        return ResponseEntity.ok(usuarioService.atualizarAutenticado(request));
    }

    // ------------------------------------------------------------------
    // Cadastro de usuarios: apenas MASTER
    // ------------------------------------------------------------------

    @PostMapping
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Cadastra um usuario, sempre com papel ADMIN (MASTER nao e atribuivel pela API)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Usuario criado"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou invalido"),
            @ApiResponse(responseCode = "403", description = "Apenas o MASTER gerencia usuarios"),
            @ApiResponse(responseCode = "409", description = "E-mail ja cadastrado")
    })
    public ResponseEntity<UsuarioResponse> criar(@Valid @RequestBody UsuarioCriacaoRequest request) {
        UsuarioResponse criado = usuarioService.criar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criado.id())
                .toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @GetMapping
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Lista os usuarios do sistema, com filtro por nome e situacao")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina de usuarios"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou invalido"),
            @ApiResponse(responseCode = "403", description = "Apenas o MASTER enxerga o cadastro de usuarios")
    })
    public ResponseEntity<PaginaResponse<UsuarioResponse>> listar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) Boolean ativo,
            @PageableDefault(size = 20, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(usuarioService.listar(nome, ativo, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Busca um usuario pelo id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario encontrado"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou invalido"),
            @ApiResponse(responseCode = "403", description = "Apenas o MASTER enxerga o cadastro de usuarios"),
            @ApiResponse(responseCode = "404", description = "Usuario nao encontrado")
    })
    public ResponseEntity<UsuarioResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Atualiza nome, e-mail e senha de um usuario (papel e situacao sao imutaveis)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario atualizado"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos"),
            @ApiResponse(responseCode = "401", description = "Token ausente ou invalido"),
            @ApiResponse(responseCode = "403", description = "Apenas o MASTER gerencia usuarios"),
            @ApiResponse(responseCode = "404", description = "Usuario nao encontrado"),
            @ApiResponse(responseCode = "409", description = "E-mail ja cadastrado em outro usuario")
    })
    public ResponseEntity<UsuarioResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioAtualizacaoRequest request) {
        return ResponseEntity.ok(usuarioService.atualizar(id, request));
    }

    @PatchMapping("/{id}/ativar")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Reativa o usuario, devolvendo-lhe o acesso (idempotente)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario reativado"),
            @ApiResponse(responseCode = "403", description = "Apenas o MASTER gerencia usuarios"),
            @ApiResponse(responseCode = "404", description = "Usuario nao encontrado")
    })
    public ResponseEntity<UsuarioResponse> ativar(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.alterarSituacao(id, true));
    }

    @PatchMapping("/{id}/desativar")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Revoga o acesso do usuario na hora, inclusive tokens ja emitidos (idempotente). "
            + "Nao ha exclusao definitiva: o registro de quem operou o sistema e preservado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario desativado"),
            @ApiResponse(responseCode = "403", description = "Apenas o MASTER gerencia usuarios"),
            @ApiResponse(responseCode = "404", description = "Usuario nao encontrado"),
            @ApiResponse(responseCode = "422", description = "O MASTER e unico e nao pode ser desativado")
    })
    public ResponseEntity<UsuarioResponse> desativar(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.alterarSituacao(id, false));
    }
}
