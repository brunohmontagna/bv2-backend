package dev.brunohm.bv2_projeto_software_uepg.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import dev.brunohm.bv2_projeto_software_uepg.domain.entity.Marca;

@Repository
public interface MarcaRepository extends JpaRepository<Marca, Long>, JpaSpecificationExecutor<Marca> {

    boolean existsByNomeIgnoreCase(String nome);

    /* Usado na atualizacao, para a marca nao colidir com ela mesma. */
    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);
}
