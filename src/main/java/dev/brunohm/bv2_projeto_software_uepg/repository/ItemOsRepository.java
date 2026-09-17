package dev.brunohm.bv2_projeto_software_uepg.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import dev.brunohm.bv2_projeto_software_uepg.domain.entity.ItemOs;
import dev.brunohm.bv2_projeto_software_uepg.domain.enums.StatusOs;
import dev.brunohm.bv2_projeto_software_uepg.dto.painel.RankingContagemResponse;

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

    // ------------------------------------------------------------------
    // Rankings do painel (GET /painel)
    // ------------------------------------------------------------------

    /*
     * Estes dois rankings moram aqui, e nao no OrdemServicoRepository, porque o
     * join OS<->itens so existe deste lado: OrdemServico nao mapeia @OneToMany de
     * itens, entao a travessia parte sempre de ItemOs.
     *
     * Nos dois casos a OS CANCELADA fica de fora — servico de ordem cancelada nao
     * foi executado — e o recorte e por dataEntrada, o evento que data a execucao.
     *
     * ItemOs so e lida para CONTAR. Dinheiro sai de ordens_servico.valor_total.
     */

    /**
     * count(i) conta execucoes, nao ordens: como a chave unica e
     * (os, equipamento, servico), o mesmo servico aparece duas vezes na mesma OS se
     * for para equipamentos diferentes — e sao mesmo duas execucoes.
     */
    @Query("""
            select new dev.brunohm.bv2_projeto_software_uepg.dto.painel.RankingContagemResponse(
                       s.id, s.nome, count(i))
            from ItemOs i
            join i.servico s
            join i.ordemServico o
            where o.cliente.usuarioId = :usuarioId
              and o.dataEntrada between :dataInicio and :dataFim
              and o.status <> :statusCancelada
            group by s.id, s.nome
            order by count(i) desc, s.nome asc
            """)
    List<RankingContagemResponse> rankingServicosMaisExecutados(@Param("usuarioId") Long usuarioId,
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim,
            @Param("statusCancelada") StatusOs statusCancelada,
            Pageable limite);

    /**
     * count(distinct o.id), diferente do ranking de servicos: "marca atendida" e um
     * evento por ordem de servico, entao tres itens da mesma marca na mesma OS
     * contam uma vez so. A assimetria entre os dois rankings e proposital.
     */
    @Query("""
            select new dev.brunohm.bv2_projeto_software_uepg.dto.painel.RankingContagemResponse(
                       m.id, m.nome, count(distinct o.id))
            from ItemOs i
            join i.equipamento e
            join e.marca m
            join i.ordemServico o
            where o.cliente.usuarioId = :usuarioId
              and o.dataEntrada between :dataInicio and :dataFim
              and o.status <> :statusCancelada
            group by m.id, m.nome
            order by count(distinct o.id) desc, m.nome asc
            """)
    List<RankingContagemResponse> rankingMarcasMaisAtendidas(@Param("usuarioId") Long usuarioId,
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim,
            @Param("statusCancelada") StatusOs statusCancelada,
            Pageable limite);

    /*
     * Exclusao de conta (UsuarioService.excluir). Pega o item pelos tres lados — OS, equipamento e servico da conta —
     * para nenhuma FK RESTRICT sobrar, mesmo em dado anterior ao escopo por conta.
     */
    @Modifying
    @Query("""
            delete from ItemOs i
             where i.ordemServico.id in (select o.id from OrdemServico o where o.cliente.usuarioId = :usuarioId)
                or i.equipamento.id in (select e.id from Equipamento e where e.cliente.usuarioId = :usuarioId)
                or i.servico.id in (select s.id from Servico s where s.usuarioId = :usuarioId)
            """)
    int excluirDaConta(@Param("usuarioId") Long usuarioId);
}
