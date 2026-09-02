package dev.brunohm.bv2_projeto_software_uepg.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import dev.brunohm.bv2_projeto_software_uepg.domain.entity.Usuario;

@Repository
public interface UsuarioRepository
        extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    /* Usado na atualizacao, para o e-mail nao colidir com o do proprio usuario. */
    boolean existsByEmailAndIdNot(String email, Long id);
}
