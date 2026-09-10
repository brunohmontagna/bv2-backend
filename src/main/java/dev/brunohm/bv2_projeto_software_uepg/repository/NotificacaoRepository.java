package dev.brunohm.bv2_projeto_software_uepg.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import dev.brunohm.bv2_projeto_software_uepg.domain.entity.Notificacao;

@Repository
public interface NotificacaoRepository
        extends JpaRepository<Notificacao, Long>, JpaSpecificationExecutor<Notificacao> {

    /*
     * cliente e LAZY e o NotificacaoResponse o aninha. Sem o EntityGraph a listagem
     * produz N+1 e, com open-in-view=false, LazyInitializationException na
     * serializacao.
     *
     * ordemServico fica de fora de proposito: a resposta expoe so o id, e ler o id
     * de um proxy LAZY nao dispara query.
     */
    @Override
    @EntityGraph(attributePaths = { "cliente" })
    Optional<Notificacao> findById(Long id);

    @Override
    @EntityGraph(attributePaths = { "cliente" })
    Page<Notificacao> findAll(Specification<Notificacao> spec, Pageable pageable);
}
