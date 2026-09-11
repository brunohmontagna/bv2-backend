package dev.brunohm.bv2_projeto_software_uepg.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.brunohm.bv2_projeto_software_uepg.dto.auth.ConfirmacaoEmailRequest;
import dev.brunohm.bv2_projeto_software_uepg.dto.auth.LoginRequest;
import dev.brunohm.bv2_projeto_software_uepg.dto.auth.LoginResponse;
import dev.brunohm.bv2_projeto_software_uepg.dto.auth.RecuperacaoSenhaRequest;
import dev.brunohm.bv2_projeto_software_uepg.dto.auth.RedefinicaoSenhaRequest;
import dev.brunohm.bv2_projeto_software_uepg.service.AlteracaoEmailService;
import dev.brunohm.bv2_projeto_software_uepg.service.AuthService;
import dev.brunohm.bv2_projeto_software_uepg.service.RecuperacaoSenhaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticacao", description = "Emissao de tokens JWT, recuperacao de senha e confirmacao de e-mail. Rotas publicas")
public class AuthController {

    private final AuthService authService;
    private final RecuperacaoSenhaService recuperacaoSenhaService;
    private final AlteracaoEmailService alteracaoEmailService;

    @PostMapping("/login")
    @Operation(summary = "Autentica um usuario e devolve o token JWT")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticado"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos"),
            @ApiResponse(responseCode = "401", description = "Credenciais invalidas")
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.autenticar(request));
    }

    /**
     * Sempre 202, exista o e-mail ou nao. Um 404 para e-mail desconhecido viraria um
     * oraculo para descobrir quem tem conta no sistema.
     */
    @PostMapping("/senha/esqueci")
    @Operation(summary = "Envia por e-mail um link para redefinir a senha",
            description = "Responde 202 mesmo quando o e-mail nao esta cadastrado ou o usuario esta inativo: "
                    + "a resposta e identica nos tres casos, para nao revelar quem tem conta.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Pedido recebido. Se a conta existir, o e-mail sai"),
            @ApiResponse(responseCode = "400", description = "E-mail ausente ou mal formatado")
    })
    public ResponseEntity<Void> esqueciSenha(@Valid @RequestBody RecuperacaoSenhaRequest request) {
        recuperacaoSenhaService.solicitar(request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/senha/redefinir")
    @Operation(summary = "Redefine a senha usando o token recebido por e-mail",
            description = "O token vale uma unica vez e expira. Token inexistente, ja usado, expirado ou "
                    + "de usuario inativo devolvem a mesma mensagem, de proposito.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Senha redefinida. Os tokens emitidos antes deixam de valer"),
            @ApiResponse(responseCode = "400", description = "Dados invalidos"),
            @ApiResponse(responseCode = "422", description = "Link invalido ou expirado, ou confirmacao divergente")
    })
    public ResponseEntity<Void> redefinirSenha(@Valid @RequestBody RedefinicaoSenhaRequest request) {
        recuperacaoSenhaService.redefinir(request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Publica, como /auth/senha/redefinir: o token e a credencial, e o clique no link
     * costuma acontecer em outro navegador, onde nao ha sessao nenhuma.
     */
    @PostMapping("/email/confirmar")
    @Operation(summary = "Confirma a troca de e-mail usando o token enviado ao endereco novo",
            description = "Efetiva a troca e derruba as sessoes abertas: o subject do JWT e o e-mail, entao "
                    + "os tokens emitidos com o endereco antigo deixam de valer. Token inexistente, usado, "
                    + "expirado ou de outro fluxo devolvem a mesma mensagem, de proposito.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "E-mail alterado. As sessoes abertas deixam de valer"),
            @ApiResponse(responseCode = "400", description = "Token ausente"),
            @ApiResponse(responseCode = "409", description = "O endereco foi cadastrado por outro usuario desde o pedido"),
            @ApiResponse(responseCode = "422", description = "Link invalido ou expirado")
    })
    public ResponseEntity<Void> confirmarEmail(@Valid @RequestBody ConfirmacaoEmailRequest request) {
        alteracaoEmailService.confirmar(request);
        return ResponseEntity.noContent().build();
    }
}
