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
@Tag(name = "Autenticação", description = "Emissão de tokens JWT, recuperação de senha e confirmação de e-mail. Rotas públicas")
public class AuthController {

    private final AuthService authService;
    private final RecuperacaoSenhaService recuperacaoSenhaService;
    private final AlteracaoEmailService alteracaoEmailService;

    @PostMapping("/login")
    @Operation(summary = "Autentica um usuário e devolve o token JWT")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas")
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
            description = "Responde 202 mesmo quando o e-mail não está cadastrado ou o usuário está inativo: "
                    + "a resposta é idêntica nos três casos, para não revelar quem tem conta.")
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
            description = "O token vale uma única vez e expira. Token inexistente, já usado, expirado ou "
                    + "de usuário inativo devolvem a mesma mensagem, de propósito.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Senha redefinida. Os tokens emitidos antes deixam de valer"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "422", description = "Link inválido ou expirado, confirmação divergente ou nova senha igual à atual")
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
    @Operation(summary = "Confirma a troca de e-mail usando o token enviado ao endereço novo",
            description = "Efetiva a troca e derruba as sessões abertas: o subject do JWT é o e-mail, então "
                    + "os tokens emitidos com o endereço antigo deixam de valer. Token inexistente, usado, "
                    + "expirado ou de outro fluxo devolvem a mesma mensagem, de propósito.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "E-mail alterado. As sessões abertas deixam de valer"),
            @ApiResponse(responseCode = "400", description = "Token ausente"),
            @ApiResponse(responseCode = "409", description = "O endereço foi cadastrado por outro usuário desde o pedido"),
            @ApiResponse(responseCode = "422", description = "Link inválido ou expirado")
    })
    public ResponseEntity<Void> confirmarEmail(@Valid @RequestBody ConfirmacaoEmailRequest request) {
        alteracaoEmailService.confirmar(request);
        return ResponseEntity.noContent().build();
    }
}
