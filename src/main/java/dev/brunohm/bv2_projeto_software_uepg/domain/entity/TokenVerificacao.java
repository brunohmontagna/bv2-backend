package dev.brunohm.bv2_projeto_software_uepg.domain.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import dev.brunohm.bv2_projeto_software_uepg.domain.enums.FinalidadeToken;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Token de uso unico enviado por e-mail. Serve aos dois fluxos que precisam
 * provar posse de uma caixa postal: recuperar a senha e confirmar um endereco
 * novo. Qual dos dois e o que diz a {@link FinalidadeToken}.
 *
 * <p>
 * <b>O token em si nunca e persistido</b> — so o SHA-256 dele. O valor em claro
 * existe apenas na memoria durante a requisicao que o gera e no link do e-mail:
 * assim um vazamento do banco nao permite redefinir senha nem sequestrar conta
 * de ninguem.
 */
@Entity
@Table(name = "tokens_verificacao")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenVerificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    /** SHA-256 do token em hexadecimal: 64 caracteres, com UNIQUE no banco. */
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "finalidade", nullable = false, columnDefinition = "finalidade_token")
    private FinalidadeToken finalidade;

    /**
     * O endereco que o usuario quer passar a usar. Preenchido so em
     * ALTERACAO_EMAIL — no fluxo de senha nao ha e-mail novo nenhum.
     *
     * <p>
     * Fica aqui, e nao na URL nem no corpo da confirmacao, porque o endereco e
     * escolhido no pedido: a tela de confirmacao so precisa clicar.
     */
    @Column(name = "email_novo", length = 50)
    private String emailNovo;

    @Column(name = "expira_em", nullable = false)
    private LocalDateTime expiraEm;

    /** Nulo enquanto pendente. Preenchido ao redimir ou ao ser invalidado. */
    @Column(name = "usado_em")
    private LocalDateTime usadoEm;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    public boolean estaPendente(LocalDateTime agora) {
        return usadoEm == null && expiraEm.isAfter(agora);
    }
}
