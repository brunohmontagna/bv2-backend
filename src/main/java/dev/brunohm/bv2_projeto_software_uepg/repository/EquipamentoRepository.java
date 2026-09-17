package dev.brunohm.bv2_projeto_software_uepg.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    boolean existsByClienteIdAndMarcaIdAndNomeIgnoreCase(Long clienteId, Long marcaId, String nome);

    /* Resumo do painel. Equipamento nao tem dono proprio: herda o do cliente. */
    long countByClienteUsuarioId(Long usuarioId);

    /* Usado na atualizacao, para o equipamento nao colidir com ele mesmo. */
    boolean existsByClienteIdAndMarcaIdAndNomeIgnoreCaseAndIdNot(
            Long clienteId, Long marcaId, String nome, Long id);

    /* Exclusao de conta (UsuarioService.excluir). */
    @Modifying
    @Query("""
            delete from Equipamento e
             where e.cliente.id in (select c.id from Cliente c where c.usuarioId = :usuarioId)
            """)
    int excluirDaConta(@Param("usuarioId") Long usuarioId);
}
