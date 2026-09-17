package dev.brunohm.bv2_projeto_software_uepg.domain.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import dev.brunohm.bv2_projeto_software_uepg.domain.enums.StatusOs;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Configuracao da notificacao automatica de uma conta: qual texto sai quando a OS
 * chega a um status, e se sai. Uma linha por (conta, status notificavel).
 *
 * <p>
 * O status e a chave natural junto com o dono, e nao um enum de "tipo de
 * notificacao": o que dispara o envio e a transicao da OS, entao um segundo enum
 * paralelo so criaria duas fontes de verdade. Nao existe criacao nem exclusao pela
 * API — as linhas nascem na migration (V18/V22) ou junto com o usuario
 * (TemplatesNotificacaoPadrao), e EM_ANDAMENTO fica de fora porque abertura de OS
 * nao notifica.
 */
@Entity
@Table(name = "templates_notificacao")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateNotificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_usuario", nullable = false, updatable = false)
    private Long usuarioId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, updatable = false, columnDefinition = "status_os")
    private StatusOs status;

    /** Texto com placeholders ({cliente}, {os}, ...), resolvidos no envio. */
    @Column(name = "conteudo", nullable = false, length = 500)
    private String conteudo;

    @Builder.Default
    @Column(name = "ativo", nullable = false)
    private Boolean ativo = false;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;
}
