package dev.brunohm.bv2_projeto_software_uepg.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import dev.brunohm.bv2_projeto_software_uepg.domain.entity.TokenVerificacao;
import dev.brunohm.bv2_projeto_software_uepg.domain.entity.Usuario;
import dev.brunohm.bv2_projeto_software_uepg.domain.enums.FinalidadeToken;
import dev.brunohm.bv2_projeto_software_uepg.exception.RegraDeNegocioException;
import dev.brunohm.bv2_projeto_software_uepg.repository.TokenVerificacaoRepository;

/**
 * A mecanica de token enviada por e-mail, num lugar so: geracao, hash, validacao
 * e consumo. Os fluxos (recuperacao de senha, alteracao de e-mail) decidem o que
 * fazer com o token; como ele e gerado e verificado mora aqui.
 *
 * <p>
 * Existe para que os dois fluxos nao tenham cada um sua copia de codigo de
 * seguranca: duas copias divergiriam na primeira correcao aplicada so de um lado.
 *
 * <p>
 * Nao e transacional por conta propria. Roda dentro da transacao de quem chama,
 * para que emitir o token e mudar a conta sejam atomicos juntos.
 */
@Service
public class TokenVerificacaoService {

    /** 32 bytes = 256 bits de entropia. Forca bruta esta fora de questao. */
    private static final int TAMANHO_TOKEN_BYTES = 32;

    private final TokenVerificacaoRepository tokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final String urlFrontend;

    public TokenVerificacaoService(
            TokenVerificacaoRepository tokenRepository,
            @Value("${app.frontend.url}") String urlFrontend) {
        this.tokenRepository = tokenRepository;
        this.urlFrontend = urlFrontend;
    }

    /**
     * Gera e grava um token novo, derrubando antes os pendentes da mesma finalidade
     * — com dois e-mails na caixa de entrada, so o ultimo vale. Os pendentes de
     * <i>outra</i> finalidade ficam intactos.
     *
     * @return o token em claro. E a unica vez que ele existe fora do e-mail: o
     *         banco so guarda o hash.
     */
    public String emitir(Usuario usuario, FinalidadeToken finalidade, String emailNovo,
            long expiracaoMinutos) {
        LocalDateTime agora = LocalDateTime.now();
        tokenRepository.invalidarPendentesDoUsuario(usuario.getId(), finalidade, agora);

        String token = gerar();
        tokenRepository.save(TokenVerificacao.builder()
                .usuario(usuario)
                .tokenHash(hash(token))
                .finalidade(finalidade)
                .emailNovo(emailNovo)
                .expiraEm(agora.plusMinutes(expiracaoMinutos))
                .build());
        return token;
    }

    /**
     * Devolve o token se ele existir, for da finalidade pedida, estiver pendente e o
     * usuario estiver ativo. Qualquer outro caso lanca 422 com
     * <b>a mesma mensagem</b>: distinguir "nao existe" de "expirou" ou de "e de
     * outro fluxo" contaria ao atacante que aquele token um dia existiu.
     */
    public TokenVerificacao validar(String tokenClaro, FinalidadeToken finalidade,
            String mensagemInvalido) {
        TokenVerificacao token = tokenRepository.findByTokenHash(hash(tokenClaro))
                .orElseThrow(() -> new RegraDeNegocioException(mensagemInvalido));

        if (token.getFinalidade() != finalidade
                || !token.estaPendente(LocalDateTime.now())
                || Boolean.FALSE.equals(token.getUsuario().getAtivo())) {
            throw new RegraDeNegocioException(mensagemInvalido);
        }
        return token;
    }

    /**
     * Queima o token usado e todos os demais pendentes da mesma finalidade: depois
     * de consumido, nenhum link antigo daquele fluxo pode continuar valendo.
     */
    public void consumir(TokenVerificacao token) {
        LocalDateTime agora = LocalDateTime.now();
        token.setUsadoEm(agora);
        tokenRepository.save(token);
        tokenRepository.invalidarPendentesDoUsuario(
                token.getUsuario().getId(), token.getFinalidade(), agora);
    }

    /** Monta o link para uma tela do front. A barra final da URL nao vira //. */
    public String link(String caminho, String token) {
        String base = urlFrontend.endsWith("/")
                ? urlFrontend.substring(0, urlFrontend.length() - 1)
                : urlFrontend;
        return base + caminho + "?token=" + token;
    }

    /* Base64 URL-safe para o token viajar na query string sem escapar nada. */
    private String gerar() {
        byte[] bytes = new byte[TAMANHO_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /*
     * SHA-256 e nao BCrypt: o token ja tem 256 bits de entropia, entao key stretching
     * nao acrescenta nada, e o hash precisa ser buscavel por igualdade — o sal do
     * BCrypt obrigaria a varrer a tabela inteira.
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
}
