package dev.brunohm.bv2_projeto_software_uepg.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import dev.brunohm.bv2_projeto_software_uepg.domain.entity.Servico;

@Repository
public interface ServicoRepository extends JpaRepository<Servico, Long>, JpaSpecificationExecutor<Servico> {

    /*
     * Pre-checagem da unicidade (conta, nome) — indice uq_servicos_usuario_nome (V14) —
     * para dar 409 com mensagem propria.
     */
    boolean existsByUsuarioIdAndNomeIgnoreCase(Long usuarioId, String nome);

    /* Usado na atualizacao, para o servico nao colidir com ele mesmo. */
    boolean existsByUsuarioIdAndNomeIgnoreCaseAndIdNot(Long usuarioId, String nome, Long id);

    /* Resumo do painel: mede o catalogo oferecido hoje, nao o historico. */
    long countByUsuarioIdAndAtivoTrue(Long usuarioId);

    /* Exclusao de conta (UsuarioService.excluir). */
    @Modifying
    @Query("delete from Servico s where s.usuarioId = :usuarioId")
    int excluirDaConta(@Param("usuarioId") Long usuarioId);
}
