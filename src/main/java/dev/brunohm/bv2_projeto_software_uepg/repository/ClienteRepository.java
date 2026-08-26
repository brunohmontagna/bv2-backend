package dev.brunohm.bv2_projeto_software_uepg.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import dev.brunohm.bv2_projeto_software_uepg.domain.entity.Cliente;

@Repository
public interface ClienteRepository
        extends JpaRepository<Cliente, Long>, JpaSpecificationExecutor<Cliente> {

    /*
     * Cliente.usuario e LAZY e a serializacao do ClienteResponse toca no usuario.
     * Sem o EntityGraph a listagem produz N+1 (e, com open-in-view=false,
     * LazyInitializationException). Por isso as leituras usadas pela API
     * declaram explicitamente o fetch do usuario.
     */
    @Override
    @EntityGraph(attributePaths = "usuario")
    Optional<Cliente> findById(Long id);

    @Override
    @EntityGraph(attributePaths = "usuario")
    Page<Cliente> findAll(Specification<Cliente> spec, Pageable pageable);

    @EntityGraph(attributePaths = "usuario")
    Optional<Cliente> findByUsuarioId(Long usuarioId);

    boolean existsByUsuarioEmail(String email);
}
