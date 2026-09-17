package dev.brunohm.bv2_projeto_software_uepg.domain.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Cliente de uma conta: quem leva o equipamento para consertar. E um cadastro —
 * <b>nao faz login</b>. Pertence ao usuario dono da conta (usuarioId), e tudo que
 * pende dele (equipamentos, OS, notificacoes) herda esse dono.
 */
@Entity
@Table(name = "clientes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Id simples, sem @ManyToOne: o dono so e usado para filtrar e comparar, e uma
     * associacao LAZY nova obrigaria EntityGraph em toda consulta que a tocasse.
     * Imutavel: mudar o dono moveria o historico inteiro do cliente de conta.
     */
    @Column(name = "id_usuario", nullable = false, updatable = false)
    private Long usuarioId;

    @Column(name = "nome", nullable = false, length = 50)
    private String nome;

    @Column(name = "telefone", nullable = false, length = 13)
    private String telefone;

    @Builder.Default
    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;
}
