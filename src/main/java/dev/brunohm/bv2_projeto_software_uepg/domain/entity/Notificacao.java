package dev.brunohm.bv2_projeto_software_uepg.domain.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import dev.brunohm.bv2_projeto_software_uepg.domain.enums.StatusNotificacao;
import dev.brunohm.bv2_projeto_software_uepg.domain.enums.StatusOs;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
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
 * Log de execucao de um envio de notificacao ao cliente da M2. Nao e
 * configuracao: quem define o que sai e quando e o TemplateNotificacao. Aqui
 * fica o registro do que foi tentado, com o texto exato que foi enviado.
 *
 * <p>
 * Escrita apenas pelo sistema, no listener da transicao de status — a API so le.
 */
@Entity
@Table(name = "notificacoes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_ordem_servico", nullable = false)
    private OrdemServico ordemServico;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    /**
     * Status para o qual a OS mudou e que disparou o envio. E um retrato, nao uma
     * FK para templates_notificacao: o log precisa continuar legivel mesmo que o
     * template seja reescrito ou removido depois.
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status_os", nullable = false, columnDefinition = "status_os")
    private StatusOs statusOs;

    /**
     * O texto ja renderizado que foi ao cliente, e nao o template. Congelar aqui e
     * o que impede que editar o template reescreva o historico do que ja saiu.
     */
    @Column(name = "conteudo", nullable = false, length = 1000)
    private String conteudo;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "status_notificacao")
    private StatusNotificacao status = StatusNotificacao.PENDENTE;

    @Builder.Default
    @Column(name = "tentativas", nullable = false)
    private Integer tentativas = 0;

    @Column(name = "data_envio")
    private LocalDateTime dataEnvio;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;
}
