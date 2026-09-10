package dev.brunohm.bv2_projeto_software_uepg.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.brunohm.bv2_projeto_software_uepg.domain.entity.TokenRecuperacaoSenha;
import dev.brunohm.bv2_projeto_software_uepg.domain.entity.Usuario;
import dev.brunohm.bv2_projeto_software_uepg.dto.auth.RecuperacaoSenhaRequest;
import dev.brunohm.bv2_projeto_software_uepg.dto.auth.RedefinicaoSenhaRequest;
import dev.brunohm.bv2_projeto_software_uepg.exception.RegraDeNegocioException;
import dev.brunohm.bv2_projeto_software_uepg.integration.EmailService;
import dev.brunohm.bv2_projeto_software_uepg.repository.TokenRecuperacaoSenhaRepository;
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

    /** 32 bytes = 256 bits de entropia. Forca bruta esta fora de questao. */
    private static final int TAMANHO_TOKEN_BYTES = 32;

    private final UsuarioRepository usuarioRepository;
    private final TokenRecuperacaoSenhaRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    private final String urlFrontend;
    private final long expiracaoMinutos;

    public RecuperacaoSenhaService(
            UsuarioRepository usuarioRepository,
            TokenRecuperacaoSenhaRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            @Value("${app.frontend.url}") String urlFrontend,
            @Value("${app.recuperacao-senha.expiracao-minutos:30}") long expiracaoMinutos) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.urlFrontend = urlFrontend;
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

        LocalDateTime agora = LocalDateTime.now();

        // Pedir um link novo derruba os anteriores: com dois e-mails na caixa de
        // entrada, so o ultimo funciona.
        tokenRepository.invalidarPendentesDoUsuario(usuario.getId(), agora);

        String token = gerarToken();
        tokenRepository.save(TokenRecuperacaoSenha.builder()
                .usuario(usuario)
                .tokenHash(hash(token))
                .expiraEm(agora.plusMinutes(expiracaoMinutos))
                .build());

        emailService.enviarRecuperacaoSenha(
                usuario.getEmail(), usuario.getNome(), montarLink(token), expiracaoMinutos);
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

        TokenRecuperacaoSenha token = tokenRepository.findByTokenHash(hash(request.token()))
                .orElseThrow(() -> new RegraDeNegocioException(TOKEN_INVALIDO));

        LocalDateTime agora = LocalDateTime.now();

        // Uso unico e prazo curto, com a mesma mensagem do token inexistente.
        if (!token.estaPendente(agora)) {
            throw new RegraDeNegocioException(TOKEN_INVALIDO);
        }

        Usuario usuario = token.getUsuario();

        if (Boolean.FALSE.equals(usuario.getAtivo())) {
            throw new RegraDeNegocioException(TOKEN_INVALIDO);
        }

        usuario.setSenha(passwordEncoder.encode(request.senhaNova()));
        usuario.setSenhaAlteradaEm(agora.truncatedTo(ChronoUnit.SECONDS));
        usuarioRepository.save(usuario);

        // Queima este token e qualquer outro pendente: depois da troca, nenhum link
        // antigo pode continuar valendo.
        token.setUsadoEm(agora);
        tokenRepository.save(token);
        tokenRepository.invalidarPendentesDoUsuario(usuario.getId(), agora);

        log.info("Senha redefinida por recuperacao para o usuario {}.", usuario.getId());
    }

    /* Base64 URL-safe para o token viajar na query string sem escapar nada. */
    private String gerarToken() {
        byte[] bytes = new byte[TAMANHO_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /*
     * SHA-256 e nao BCrypt: o token ja tem 256 bits de entropia, entao key stretching
     * nao acrescenta nada, e o hash precisa ser buscavel por igualdade — o sal do
     * BCrypt obrigaria a varrer a tabela inteira comparando linha a linha.
     */
    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 e obrigatorio em toda JVM; se faltar, nao ha o que fazer.
            throw new IllegalStateException("SHA-256 indisponivel nesta JVM", e);
        }
    }

    /* A barra final da URL configurada nao pode virar // no link. */
    private String montarLink(String token) {
        String base = urlFrontend.endsWith("/")
                ? urlFrontend.substring(0, urlFrontend.length() - 1)
                : urlFrontend;
        return base + "/redefinir-senha?token=" + token;
    }
}
