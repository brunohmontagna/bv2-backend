package dev.brunohm.bv2_projeto_software_uepg.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.brunohm.bv2_projeto_software_uepg.domain.entity.TokenVerificacao;
import dev.brunohm.bv2_projeto_software_uepg.domain.entity.Usuario;
import dev.brunohm.bv2_projeto_software_uepg.domain.enums.FinalidadeToken;
import dev.brunohm.bv2_projeto_software_uepg.dto.auth.RecuperacaoSenhaRequest;
import dev.brunohm.bv2_projeto_software_uepg.dto.auth.RedefinicaoSenhaRequest;
import dev.brunohm.bv2_projeto_software_uepg.exception.RegraDeNegocioException;
import dev.brunohm.bv2_projeto_software_uepg.integration.EmailService;
import dev.brunohm.bv2_projeto_software_uepg.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;

/**
 * Fluxo "esqueci minha senha": pede um link por e-mail e o usa para definir a nova
 * senha, sem precisar saber a antiga.
 *
 * <p>
 * <b>Nada aqui revela se um e-mail existe.</b> O solicitar sempre termina em
 * silencio e o controller sempre responde 202 — e-mail cadastrado ou nao, usuario
 * ativo ou nao. Responder 404 para e-mail desconhecido transformaria o endpoint num
 * oraculo para descobrir quem tem conta no sistema. E uma inconsistencia deliberada
 * com POST /usuarios, que devolve 409 dizendo que o e-mail ja existe: la quem
 * pergunta e um MASTER autenticado, aqui e um anonimo.
 *
 * <p>
 * Pelo mesmo motivo, todos os erros de redefinicao usam a <b>mesma mensagem</b>.
 * Distinguir "token inexistente" de "token expirado" contaria ao atacante que
 * aquele token um dia existiu.
 */
@Slf4j
@Service
public class RecuperacaoSenhaService {

    /** Mensagem unica para todos os motivos de recusa. Ver o Javadoc da classe. */
    private static final String TOKEN_INVALIDO =
            "Link de redefinicao invalido ou expirado. Solicite um novo.";

    private final UsuarioRepository usuarioRepository;
    private final TokenVerificacaoService tokenVerificacaoService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final long expiracaoMinutos;

    public RecuperacaoSenhaService(
            UsuarioRepository usuarioRepository,
            TokenVerificacaoService tokenVerificacaoService,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            @Value("${app.recuperacao-senha.expiracao-minutos:30}") long expiracaoMinutos) {
        this.usuarioRepository = usuarioRepository;
        this.tokenVerificacaoService = tokenVerificacaoService;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.expiracaoMinutos = expiracaoMinutos;
    }

    /**
     * Gera o link e envia por e-mail. Retorna em silencio quando o e-mail nao existe
     * ou o usuario esta inativo — do lado de fora os tres casos sao indistinguiveis.
     */
    @Transactional
    public void solicitar(RecuperacaoSenhaRequest request) {
        Optional<Usuario> encontrado = usuarioRepository.findByEmail(request.email());

        if (encontrado.isEmpty()) {
            log.debug("Recuperacao pedida para e-mail nao cadastrado.");
            return;
        }

        Usuario usuario = encontrado.get();

        if (Boolean.FALSE.equals(usuario.getAtivo())) {
            log.debug("Recuperacao pedida para usuario desativado: {}", usuario.getId());
            return;
        }

        // Pedir um link novo derruba os anteriores desta finalidade: com dois
        // e-mails na caixa de entrada, so o ultimo funciona.
        String token = tokenVerificacaoService.emitir(
                usuario, FinalidadeToken.RECUPERACAO_SENHA, null, expiracaoMinutos);

        emailService.enviarRecuperacaoSenha(
                usuario.getEmail(),
                usuario.getNome(),
                tokenVerificacaoService.link("/redefinir-senha", token),
                expiracaoMinutos);
    }

    /**
     * Consome o token e troca a senha. Preenche senhaAlteradaEm, o que invalida todos
     * os JWTs emitidos antes — se a conta foi tomada, redefinir a senha expulsa quem
     * estava dentro.
     */
    @Transactional
    public void redefinir(RedefinicaoSenhaRequest request) {
        if (!request.senhaNova().equals(request.senhaNovaConfirmacao())) {
            throw new RegraDeNegocioException("A nova senha e a confirmacao nao conferem.");
        }

        // Inexistente, de outra finalidade, usado, expirado ou de usuario inativo:
        // todos com a mesma mensagem, para nao revelar que o token existiu.
        TokenVerificacao token = tokenVerificacaoService.validar(
                request.token(), FinalidadeToken.RECUPERACAO_SENHA, TOKEN_INVALIDO);

        Usuario usuario = token.getUsuario();

        usuario.setSenha(passwordEncoder.encode(request.senhaNova()));
        usuario.setSenhaAlteradaEm(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        usuarioRepository.save(usuario);

        // Queima este token e qualquer outro pendente da mesma finalidade: depois da
        // troca, nenhum link antigo de recuperacao pode continuar valendo.
        tokenVerificacaoService.consumir(token);

        log.info("Senha redefinida por recuperacao para o usuario {}.", usuario.getId());
    }
}
