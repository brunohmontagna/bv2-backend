package dev.brunohm.bv2_projeto_software_uepg.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.brunohm.bv2_projeto_software_uepg.domain.entity.TokenVerificacao;
import dev.brunohm.bv2_projeto_software_uepg.domain.entity.Usuario;
import dev.brunohm.bv2_projeto_software_uepg.domain.enums.FinalidadeToken;
import dev.brunohm.bv2_projeto_software_uepg.dto.auth.ConfirmacaoSenhaRequest;
import dev.brunohm.bv2_projeto_software_uepg.dto.usuario.AlteracaoSenhaRequest;
import dev.brunohm.bv2_projeto_software_uepg.exception.RecursoNaoEncontradoException;
import dev.brunohm.bv2_projeto_software_uepg.exception.RegraDeNegocioException;
import dev.brunohm.bv2_projeto_software_uepg.integration.EmailService;
import dev.brunohm.bv2_projeto_software_uepg.repository.UsuarioRepository;
import dev.brunohm.bv2_projeto_software_uepg.security.AutenticacaoAtual;
import lombok.extern.slf4j.Slf4j;

/**
 * Troca de senha pelo proprio usuario, em duas etapas: o pedido informa a senha
 * atual e a nova, e a troca so acontece quando o link enviado <b>ao e-mail
 * atual</b> e clicado.
 *
 * <p>
 * <b>Por que a senha atual nao basta.</b> Ela prova que quem pede conhece a senha,
 * nao que e o dono da conta: uma sessao esquecida aberta mais uma senha vazada
 * bastariam para trancar o dono do lado de fora, e ele nem ficaria sabendo. Com a
 * confirmacao na caixa postal, o dono sempre recebe o aviso e a troca nao acontece
 * sem ele.
 *
 * <p>
 * <b>Por que a senha nova e escolhida no pedido, e nao no clique.</b> Quem clica
 * so prova que le o e-mail. Se a senha fosse definida ali, o link viraria um
 * "esqueci minha senha" sem a senha atual. Assim cada etapa prova uma coisa: o
 * pedido, que a pessoa sabe a senha; o clique, que ela controla o e-mail.
 */
@Slf4j
@Service
public class AlteracaoSenhaService {

    /** Mensagem unica para todos os motivos de recusa do token. */
    private static final String TOKEN_INVALIDO =
            "Link de confirmação inválido ou expirado. Solicite a troca de senha novamente.";

    private final UsuarioRepository usuarioRepository;
    private final TokenVerificacaoService tokenVerificacaoService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AutenticacaoAtual autenticacaoAtual;
    private final long expiracaoMinutos;

    public AlteracaoSenhaService(
            UsuarioRepository usuarioRepository,
            TokenVerificacaoService tokenVerificacaoService,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            AutenticacaoAtual autenticacaoAtual,
            @Value("${app.alteracao-senha.expiracao-minutos:30}") long expiracaoMinutos) {
        this.usuarioRepository = usuarioRepository;
        this.tokenVerificacaoService = tokenVerificacaoService;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.autenticacaoAtual = autenticacaoAtual;
        this.expiracaoMinutos = expiracaoMinutos;
    }

    /**
     * Registra o pedido e manda o link ao e-mail atual. <b>Nada muda na conta
     * aqui</b> — a senha atual continua valendo ate a confirmacao.
     *
     * <p>
     * Os tres 422 sao deliberados. Senha atual errada nao pode ser 401: o token e
     * valido e o usuario <i>esta</i> autenticado, e o front desloga em qualquer 401 —
     * um erro de digitacao expulsaria o usuario da sessao.
     */
    @Transactional
    public void solicitar(AlteracaoSenhaRequest request) {
        Long id = autenticacaoAtual.usuario().getId();
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Usuário", id));

        if (!passwordEncoder.matches(request.senhaAtual(), usuario.getSenha())) {
            throw new RegraDeNegocioException("A senha atual está incorreta.");
        }

        if (!request.senhaNova().equals(request.senhaNovaConfirmacao())) {
            throw new RegraDeNegocioException("A nova senha e a confirmação não conferem.");
        }

        if (passwordEncoder.matches(request.senhaNova(), usuario.getSenha())) {
            throw new RegraDeNegocioException("A nova senha deve ser diferente da atual.");
        }

        // Pedir de novo derruba o link anterior: com dois e-mails na caixa, so o
        // ultimo vale — e e a senha do ultimo pedido que entra.
        String token = tokenVerificacaoService.emitir(
                usuario, FinalidadeToken.ALTERACAO_SENHA, null,
                passwordEncoder.encode(request.senhaNova()), expiracaoMinutos);

        emailService.enviarConfirmacaoSenha(
                usuario.getEmail(),
                usuario.getNome(),
                tokenVerificacaoService.link("/confirmar-senha", token),
                expiracaoMinutos);
    }

    /**
     * Consome o token e efetiva a troca. Preenche senhaAlteradaEm, o que invalida
     * todos os JWTs emitidos antes — inclusive o da sessao que fez o pedido.
     */
    @Transactional
    public void confirmar(ConfirmacaoSenhaRequest request) {
        TokenVerificacao token = tokenVerificacaoService.validar(
                request.token(), FinalidadeToken.ALTERACAO_SENHA, TOKEN_INVALIDO);

        Usuario usuario = token.getUsuario();

        usuario.setSenha(token.getSenhaNova());
        // Truncado a segundos para casar com o iat do JWT. Ver Usuario.senhaAlteradaEm.
        usuario.setSenhaAlteradaEm(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        usuarioRepository.save(usuario);

        tokenVerificacaoService.consumir(token);

        log.info("Senha alterada com confirmação por e-mail para o usuário {}.", usuario.getId());
    }
}
