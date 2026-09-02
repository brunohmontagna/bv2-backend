package dev.brunohm.bv2_projeto_software_uepg.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dev.brunohm.bv2_projeto_software_uepg.domain.entity.ItemOs;

@Repository
public interface ItemOsRepository extends JpaRepository<ItemOs, Long> {

    /*
     * equipamento e servico sao LAZY e o ItemOsResponse toca nos dois; o
     * EntityGraph evita o N+1 e a LazyInitializationException (open-in-view=false).
     */
    @Override
    @EntityGraph(attributePaths = { "equipamento", "servico" })
    Optional<ItemOs> findById(Long id);

    @EntityGraph(attributePaths = { "equipamento", "servico" })
    List<ItemOs> findByOrdemServicoIdOrderByIdAsc(Long ordemServicoId);

    /** Pre-checagem da UNIQUE uq_item_os_os_equip_servico, para dar 409 com mensagem propria. */
    boolean existsByOrdemServicoIdAndEquipamentoIdAndServicoId(Long ordemServicoId, Long equipamentoId, Long servicoId);

    boolean existsByOrdemServicoId(Long ordemServicoId);
}
