package dev.brunohm.bv2_projeto_software_uepg.repository;

import java.math.BigDecimal;

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
     * Pre-checagem da unicidade (conta, nome, valor) — indice uq_servicos_usuario_nome_valor
     * (V22) — para dar 409 com mensagem propria.
     */
    boolean existsByUsuarioIdAndNomeIgnoreCaseAndValor(Long usuarioId, String nome, BigDecimal valor);

    /* Usado na atualizacao, para o servico nao colidir com ele mesmo. */
    boolean existsByUsuarioIdAndNomeIgnoreCaseAndValorAndIdNot(Long usuarioId, String nome, BigDecimal valor, Long id);

    /* Resumo do painel: mede o catalogo oferecido hoje, nao o historico. */
    long countByUsuarioIdAndAtivoTrue(Long usuarioId);

    /* Exclusao de conta (UsuarioService.excluir). */
    @Modifying
    @Query("delete from Servico s where s.usuarioId = :usuarioId")
    int excluirDaConta(@Param("usuarioId") Long usuarioId);
}
