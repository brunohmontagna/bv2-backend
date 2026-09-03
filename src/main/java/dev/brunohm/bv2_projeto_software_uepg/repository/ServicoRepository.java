package dev.brunohm.bv2_projeto_software_uepg.repository;

import java.math.BigDecimal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import dev.brunohm.bv2_projeto_software_uepg.domain.entity.Servico;

@Repository
public interface ServicoRepository extends JpaRepository<Servico, Long>, JpaSpecificationExecutor<Servico> {

    /* Pre-checagem da unicidade (nome, valor) para dar 409 com mensagem propria. */
    boolean existsByNomeIgnoreCaseAndValor(String nome, BigDecimal valor);

    /* Usado na atualizacao, para o servico nao colidir com ele mesmo. */
    boolean existsByNomeIgnoreCaseAndValorAndIdNot(String nome, BigDecimal valor, Long id);

    /* Resumo do painel: mede o catalogo oferecido hoje, nao o historico. */
    long countByAtivoTrue();
}
