package dev.brunohm.bv2_projeto_software_uepg.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import dev.brunohm.bv2_projeto_software_uepg.domain.entity.Usuario;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/** Geracao e verificacao dos tokens JWT (HMAC256, biblioteca java-jwt da Auth0). */
@Slf4j
@Service
public class JwtService {

    private static final String ISSUER = "bv2-api";

    private final String secret;
    private final long expiracaoMinutos;

    private Algorithm algoritmo;
    private JWTVerifier verificador;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiracao-minutos:120}") long expiracaoMinutos) {
        this.secret = secret;
        this.expiracaoMinutos = expiracaoMinutos;
    }

    @PostConstruct
    void inicializar() {
        this.algoritmo = Algorithm.HMAC256(secret);
        this.verificador = JWT.require(algoritmo).withIssuer(ISSUER).build();
    }

    public TokenGerado gerarToken(Usuario usuario) {
        Instant expiraEm = Instant.now().plus(expiracaoMinutos, ChronoUnit.MINUTES);
        String token = JWT.create()
                .withIssuer(ISSUER)
                .withSubject(usuario.getEmail())
                .withClaim("id", usuario.getId())
                .withClaim("role", usuario.getRole().name())
                .withIssuedAt(Instant.now())
                .withExpiresAt(expiraEm)
                .sign(algoritmo);
        return new TokenGerado(token, expiraEm);
    }

    /**
     * Devolve o e-mail e o instante de emissao do token, ou vazio se ele for
     * invalido, expirado ou adulterado. Nao lanca: quem decide o que fazer e o filtro.
     *
     * <p>
     * O <b>emitidoEm</b> vem junto porque o filtro compara com o senhaAlteradaEm do
     * usuario para derrubar sessoes antigas depois de uma troca de senha. Note que
     * isso <i>nao exigiu claim novo</i>: o iat ja e emitido pelo gerarToken desde
     * sempre — so nao estava sendo lido.
     */
    public Optional<TokenDecodificado> decodificar(String token) {
        try {
            DecodedJWT decodificado = verificador.verify(token);
            return Optional.of(new TokenDecodificado(
                    decodificado.getSubject(),
                    decodificado.getIssuedAtAsInstant()));
        } catch (JWTVerificationException ex) {
            log.debug("Token JWT rejeitado: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public record TokenGerado(String token, Instant expiraEm) {
    }

    /** O iat vem com precisao de segundos, como manda a RFC 7519. */
    public record TokenDecodificado(String email, Instant emitidoEm) {
    }
}
