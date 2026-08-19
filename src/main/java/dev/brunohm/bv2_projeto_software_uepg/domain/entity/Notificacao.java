package dev.brunohm.bv2_projeto_software_uepg.domain.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import dev.brunohm.bv2_projeto_software_uepg.domain.enums.StatusNotificacao;
import dev.brunohm.bv2_projeto_software_uepg.domain.enums.TipoNotificacao;
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

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "tipo", nullable = false, columnDefinition = "tipo_notificacao")
    private TipoNotificacao tipo;

    @Column(name = "conteudo", nullable = false, length = 500)
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
