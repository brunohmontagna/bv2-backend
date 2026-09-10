package dev.brunohm.bv2_projeto_software_uepg.domain.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Token de uso unico do fluxo "esqueci minha senha".
 *
 * <p>
 * <b>O token em si nunca e persistido</b> — so o SHA-256 dele. O valor em claro
 * existe apenas na memoria durante a requisicao que o gera e no link do e-mail:
 * assim um vazamento do banco nao permite redefinir senha de ninguem.
 */
@Entity
@Table(name = "tokens_recuperacao_senha")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenRecuperacaoSenha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    /** SHA-256 do token em hexadecimal: 64 caracteres, com UNIQUE no banco. */
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

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
