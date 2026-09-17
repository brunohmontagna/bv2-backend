package dev.brunohm.bv2_projeto_software_uepg.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import dev.brunohm.bv2_projeto_software_uepg.domain.entity.TemplateNotificacao;
import dev.brunohm.bv2_projeto_software_uepg.domain.enums.StatusOs;

/**
 * Uma linha por (conta, status notificavel). O conjunto de cada conta e fechado:
 * nasce na migration ou junto com o usuario, e a API so le e edita.
 */
@Repository
public interface TemplateNotificacaoRepository extends JpaRepository<TemplateNotificacao, Long> {

    Optional<TemplateNotificacao> findByUsuarioIdAndStatus(Long usuarioId, StatusOs status);

    List<TemplateNotificacao> findByUsuarioId(Long usuarioId);

    /* Exclusao de conta (UsuarioService.excluir). */
    @Modifying
    @Query("delete from TemplateNotificacao t where t.usuarioId = :usuarioId")
    int excluirDaConta(@Param("usuarioId") Long usuarioId);
}
