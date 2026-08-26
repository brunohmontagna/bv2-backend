package dev.brunohm.bv2_projeto_software_uepg.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import dev.brunohm.bv2_projeto_software_uepg.domain.entity.Equipamento;

@Repository
public interface EquipamentoRepository
        extends JpaRepository<Equipamento, Long>, JpaSpecificationExecutor<Equipamento> {

    /*
     * cliente e marca sao LAZY e o EquipamentoResponse toca nos dois. Sem o
     * EntityGraph a listagem produz N+1 e, com open-in-view=false,
     * LazyInitializationException na serializacao.
     */
    @Override
    @EntityGraph(attributePaths = { "cliente", "marca" })
    Optional<Equipamento> findById(Long id);

    @Override
    @EntityGraph(attributePaths = { "cliente", "marca" })
    Page<Equipamento> findAll(Specification<Equipamento> spec, Pageable pageable);
}
