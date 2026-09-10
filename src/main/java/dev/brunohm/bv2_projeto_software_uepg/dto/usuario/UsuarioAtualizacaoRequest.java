package dev.brunohm.bv2_projeto_software_uepg.dto.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * O papel e a situacao nao entram aqui: MASTER nao e atribuivel, e ativo muda
 * pelos PATCH /ativar e /desativar.
 *
 * <p>
 * <b>A senha tambem nao.</b> Ela ja esteve neste request e foi removida: trocava a
 * senha sem exigir a atual, entao qualquer token roubado bastava para tomar a conta.
 * Agora existem exatamente dois caminhos, os dois com prova de posse —
 * PUT /usuarios/eu/senha (sabe a senha atual) e o fluxo de recuperacao por e-mail
 * (tem acesso a caixa postal). Isso vale inclusive para o MASTER: ele nao redefine
 * a senha de outro usuario, quem esqueceu usa a recuperacao.
 */
public record UsuarioAtualizacaoRequest(

        @NotBlank(message = "O nome e obrigatorio")
        @Size(max = 50, message = "O nome deve ter no maximo 50 caracteres")
        String nome,

        @NotBlank(message = "O e-mail e obrigatorio")
        @Email(message = "E-mail em formato invalido")
        @Size(max = 50, message = "O e-mail deve ter no maximo 50 caracteres")
        String email) {
}
