package dev.brunohm.bv2_projeto_software_uepg.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dev.brunohm.bv2_projeto_software_uepg.domain.entity.TemplateNotificacao;
import dev.brunohm.bv2_projeto_software_uepg.domain.enums.StatusOs;

/**
 * A chave e o proprio StatusOs. O conjunto de linhas e fechado e semeado pela
 * migration V18, entao nao ha nada alem do herdado: a API le e edita, nunca cria
 * nem exclui.
 */
@Repository
public interface TemplateNotificacaoRepository extends JpaRepository<TemplateNotificacao, StatusOs> {
}
