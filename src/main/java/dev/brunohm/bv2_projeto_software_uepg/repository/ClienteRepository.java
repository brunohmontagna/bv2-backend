package dev.brunohm.bv2_projeto_software_uepg.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import dev.brunohm.bv2_projeto_software_uepg.domain.entity.Cliente;

/*
 * Sem @EntityGraph: Cliente nao tem mais associacao LAZY alguma desde que o
 * vinculo com Usuario saiu (V12), entao o ClienteResponse se monta com os
 * proprios campos da entidade.
 */
@Repository
public interface ClienteRepository
        extends JpaRepository<Cliente, Long>, JpaSpecificationExecutor<Cliente> {

    /* Resumo do painel: cliente inativo saiu da carteira e nao conta como base ativa. */
    long countByAtivoTrue();
}
