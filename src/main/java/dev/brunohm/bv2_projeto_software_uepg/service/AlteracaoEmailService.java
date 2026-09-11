package dev.brunohm.bv2_projeto_software_uepg.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.brunohm.bv2_projeto_software_uepg.domain.entity.TokenVerificacao;
import dev.brunohm.bv2_projeto_software_uepg.domain.entity.Usuario;
import dev.brunohm.bv2_projeto_software_uepg.domain.enums.FinalidadeToken;
import dev.brunohm.bv2_projeto_software_uepg.dto.auth.ConfirmacaoEmailRequest;
import dev.brunohm.bv2_projeto_software_uepg.dto.usuario.SolicitacaoAlteracaoEmailRequest;
import dev.brunohm.bv2_projeto_software_uepg.exception.RecursoDuplicadoException;
import dev.brunohm.bv2_projeto_software_uepg.exception.RecursoNaoEncontradoException;
import dev.brunohm.bv2_projeto_software_uepg.exception.RegraDeNegocioException;
import dev.brunohm.bv2_projeto_software_uepg.integration.EmailService;
import dev.brunohm.bv2_projeto_software_uepg.repository.UsuarioRepository;
import dev.brunohm.bv2_projeto_software_uepg.security.AutenticacaoAtual;
import lombok.extern.slf4j.Slf4j;

/**
 * Troca de e-mail em duas etapas: o usuario pede informando a senha atual e o
 * endereco novo, e a troca so acontece quando o link enviado <b>ao endereco
 * novo</b> e clicado.
 *
 * <p>
 * <b>Por que a verificacao vai para o endereco novo.</b> Confirmar no endereco
 * atual provaria identidade, mas nao provaria que o novo existe — e como o e-mail
 * e o login, um typo trancaria a conta para sempre (nao ha DELETE em /usuarios, e
 * se fosse o MASTER, o cadastro de usuarios ficaria inacessivel). Verificando no
 * destino, endereco errado e apenas um link que nunca chega: a conta continua
 * exatamente como estava.
 *
 * <p>
 * <b>Por que a senha atual e exigida.</b> Como quem clica no link e quem controla
 * o destino, o clique deixa de provar identidade. Sem a senha, uma sessao roubada
 * viraria tomada de conta completa: o atacante aponta a conta para a caixa dele,
 * confirma sozinho e depois usa o "esqueci minha senha".
 */
@Slf4j
@Service
public class AlteracaoEmailService {

    /** Mensagem unica para todos os motivos de recusa do token. */
    private static final String TOKEN_INVALIDO =
            "Link de confirmacao invalido ou expirado. Solicite a alteracao novamente.";

    private final UsuarioRepository usuarioRepository;
    private final TokenVerificacaoService tokenVerificacaoService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AutenticacaoAtual autenticacaoAtual;
    private final long expiracaoMinutos;

    public AlteracaoEmailService(
            UsuarioRepository usuarioRepository,
            TokenVerificacaoService tokenVerificacaoService,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            AutenticacaoAtual autenticacaoAtual,
            @Value("${app.alteracao-email.expiracao-minutos:30}") long expiracaoMinutos) {
        this.usuarioRepository = usuarioRepository;
        this.tokenVerificacaoService = tokenVerificacaoService;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.autenticacaoAtual = autenticacaoAtual;
        this.expiracaoMinutos = expiracaoMinutos;
    }

    /**
     * Registra o pedido e manda o link ao endereco novo. <b>Nada muda na conta
     * aqui</b> — o e-mail atual continua sendo o login ate a confirmacao.
     */
    @Transactional
    public void solicitar(SolicitacaoAlteracaoEmailRequest request) {
        Long id = autenticacaoAtual.usuario().getId();
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Usuario", id));

        /*
         * 422 e nao 401: o token e valido e o usuario esta autenticado, o que falhou
         * foi uma regra. Um 401 faria o interceptador do front deslogar por erro de
         * digitacao — mesma decisao do PUT /usuarios/eu/senha.
         */
        if (!passwordEncoder.matches(request.senhaAtual(), usuario.getSenha())) {
            throw new RegraDeNegocioException("A senha atual esta incorreta.");
        }

        if (!request.novoEmail().equals(request.novoEmailConfirmacao())) {
            throw new RegraDeNegocioException("O novo e-mail e a confirmacao nao conferem.");
        }

        if (request.novoEmail().equalsIgnoreCase(usuario.getEmail())) {
            throw new RegraDeNegocioException("O novo e-mail e igual ao atual.");
        }

        garantirEmailLivre(request.novoEmail(), usuario.getId());

        String token = tokenVerificacaoService.emitir(
                usuario, FinalidadeToken.ALTERACAO_EMAIL, request.novoEmail(), expiracaoMinutos);

        emailService.enviarConfirmacaoEmail(
                request.novoEmail(),
                usuario.getNome(),
                tokenVerificacaoService.link("/confirmar-email", token),
                expiracaoMinutos);
    }

    /**
     * Consome o token e efetiva a troca.
     *
     * <p>
     * Depois disto o usuario e deslogado sem que nada aqui trate disso: o subject
     * do JWT e o e-mail, entao o JwtAuthFilter deixa de encontrar o usuario pelo
     * endereco antigo e recusa o token. E o comportamento desejado.
     */
    @Transactional
    public void confirmar(ConfirmacaoEmailRequest request) {
        TokenVerificacao token = tokenVerificacaoService.validar(
                request.token(), FinalidadeToken.ALTERACAO_EMAIL, TOKEN_INVALIDO);

        Usuario usuario = token.getUsuario();

        /*
         * Checagem de unicidade de novo, e nao por desconfianca da primeira: entre o
         * pedido e o clique, outra pessoa pode ter cadastrado esse endereco. Sem isto
         * a corrida estoura no commit como violacao de constraint, virando um 409
         * generico do banco em vez de mensagem legivel.
         */
        garantirEmailLivre(token.getEmailNovo(), usuario.getId());

        usuario.setEmail(token.getEmailNovo());
        usuarioRepository.save(usuario);

        tokenVerificacaoService.consumir(token);

        log.info("E-mail alterado para o usuario {}.", usuario.getId());
    }

    /* 409 com mensagem legivel, como POST /usuarios e o PUT ja fazem. */
    private void garantirEmailLivre(String email, Long usuarioId) {
        if (usuarioRepository.existsByEmailAndIdNot(email, usuarioId)) {
            throw new RecursoDuplicadoException(
                    "Ja existe um usuario cadastrado com o e-mail " + email);
        }
    }
}
