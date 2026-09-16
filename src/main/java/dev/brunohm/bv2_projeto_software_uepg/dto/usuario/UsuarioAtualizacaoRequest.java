package dev.brunohm.bv2_projeto_software_uepg.dto.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * O papel e a situacao nao entram aqui: MASTER nao e atribuivel, e ativo muda
 * pelos PATCH /ativar e /desativar.
 *
 * <p>
 * <b>Senha e e-mail tambem nao.</b> Os dois ja estiveram neste request e foram
 * removidos pelo mesmo motivo: mudavam sem prova de posse, e os dois sao
 * credenciais — o e-mail e o login. Um token roubado bastava para tomar a conta,
 * e um typo no e-mail a trancava para sempre.
 *
 * <p>
 * Cada um tem agora seu fluxo verificado: PUT /usuarios/eu/senha exige a senha
 * atual, e PUT /usuarios/eu/email exige a senha atual e a confirmacao no endereco
 * novo. Isso vale inclusive para o MASTER, que nao troca senha nem e-mail de outro
 * usuario.
 */
public record UsuarioAtualizacaoRequest(

        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 50, message = "O nome deve ter no máximo 50 caracteres")
        String nome) {
}
