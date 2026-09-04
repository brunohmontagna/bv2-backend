package dev.brunohm.bv2_projeto_software_uepg.controller;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.brunohm.bv2_projeto_software_uepg.domain.enums.StatusNotificacao;
import dev.brunohm.bv2_projeto_software_uepg.domain.enums.StatusOs;
import dev.brunohm.bv2_projeto_software_uepg.dto.PaginaResponse;
import dev.brunohm.bv2_projeto_software_uepg.dto.notificacao.NotificacaoResponse;
import dev.brunohm.bv2_projeto_software_uepg.dto.notificacao.PlaceholderResponse;
import dev.brunohm.bv2_projeto_software_uepg.dto.notificacao.TemplateNotificacaoRequest;
import dev.brunohm.bv2_projeto_software_uepg.dto.notificacao.TemplateNotificacaoResponse;
import dev.brunohm.bv2_projeto_software_uepg.service.NotificacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/notificacoes")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Notificacoes", description = "Avisos automaticos por WhatsApp ao trocar o status da OS. "
        + "As notificacoes sao um log escrito pelo sistema — a API so le. O que se configura sao os "
        + "templates: um por status, com o texto e a chave liga/desliga")
public class NotificacaoController {

    private final NotificacaoService notificacaoService;

    // ------------------------------------------------------------------
    // Log de envios
    // ------------------------------------------------------------------

    @GetMapping
    @Operation(summary = "Lista os envios de forma paginada, com filtro por OS, cliente, status da OS e resultado do envio")
    public ResponseEntity<PaginaResponse<NotificacaoResponse>> listar(
            @RequestParam(required = false) Long ordemServicoId,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) StatusOs statusOs,
            @RequestParam(required = false) StatusNotificacao status,
            @PageableDefault(size = 20, sort = "criadoEm", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(
                notificacaoService.listar(ordemServicoId, clienteId, statusOs, status, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um envio pelo id, com o texto exato que foi enviado ao cliente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notificacao encontrada"),
            @ApiResponse(responseCode = "404", description = "Notificacao nao encontrada")
    })
    public ResponseEntity<NotificacaoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(notificacaoService.buscarPorId(id));
    }

    // ------------------------------------------------------------------
    // Templates (configuracao)
    // ------------------------------------------------------------------

    @GetMapping("/templates")
    @Operation(summary = "Lista os templates configuraveis, um por status de OS que notifica")
    public ResponseEntity<List<TemplateNotificacaoResponse>> listarTemplates() {
        return ResponseEntity.ok(notificacaoService.listarTemplates());
    }

    @GetMapping("/placeholders")
    @Operation(summary = "Lista os placeholders aceitos no conteudo do template, com descricao e exemplo")
    public ResponseEntity<List<PlaceholderResponse>> listarPlaceholders() {
        return ResponseEntity.ok(notificacaoService.listarPlaceholders());
    }

    @PutMapping("/templates/{statusOs}")
    @Operation(summary = "Define o texto e liga ou desliga a notificacao automatica de um status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Template atualizado"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos ou status inexistente"),
            @ApiResponse(responseCode = "404", description = "Esse status nao tem notificacao configuravel"),
            @ApiResponse(responseCode = "422", description = "O conteudo usa um placeholder desconhecido")
    })
    public ResponseEntity<TemplateNotificacaoResponse> atualizarTemplate(
            @PathVariable StatusOs statusOs,
            @Valid @RequestBody TemplateNotificacaoRequest request) {
        return ResponseEntity.ok(notificacaoService.atualizarTemplate(statusOs, request));
    }
}
