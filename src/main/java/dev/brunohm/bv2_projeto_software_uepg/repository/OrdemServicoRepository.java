package dev.brunohm.bv2_projeto_software_uepg.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import dev.brunohm.bv2_projeto_software_uepg.domain.entity.OrdemServico;
import dev.brunohm.bv2_projeto_software_uepg.domain.enums.StatusOs;
import dev.brunohm.bv2_projeto_software_uepg.dto.painel.RankingFaturamentoResponse;
import dev.brunohm.bv2_projeto_software_uepg.dto.painel.projecao.ContagemStatusProjecao;
import dev.brunohm.bv2_projeto_software_uepg.dto.painel.projecao.SerieFaturamentoProjecao;
import dev.brunohm.bv2_projeto_software_uepg.dto.painel.projecao.SerieOrdensProjecao;
import dev.brunohm.bv2_projeto_software_uepg.dto.painel.projecao.TotaisEntreguesProjecao;

@Repository
public interface OrdemServicoRepository
        extends JpaRepository<OrdemServico, Long>, JpaSpecificationExecutor<OrdemServico> {

    /*
     * cliente e LAZY e o OrdemServicoResponse toca nele. Sem o EntityGraph a
     * listagem produz N+1 e, com open-in-view=false, LazyInitializationException
     * na serializacao.
     */
    @Override
    @EntityGraph(attributePaths = { "cliente" })
    Optional<OrdemServico> findById(Long id);

    @Override
    @EntityGraph(attributePaths = { "cliente" })
    Page<OrdemServico> findAll(Specification<OrdemServico> spec, Pageable pageable);

    // ------------------------------------------------------------------
    // Agregacoes do painel (GET /painel)
    // ------------------------------------------------------------------

    /*
     * Primeiras queries escritas a mao do projeto. Duas decisoes valem registro:
     *
     * JPQL com projecao por construtor, e nao SQL nativo. Os enums sao tipos
     * nomeados do Postgres (status_os), entao no nativo toda comparacao precisaria
     * de cast ::status_os e o retorno viria como Object[]. Em JPQL o status vai
     * como parametro tipado e o Hibernate infere o tipo pelo proprio path o.status
     * — o mesmo mecanismo que a Specification de OrdemServicoService.listar ja usa.
     *
     * Por isso: nunca escreva o literal do status dentro do JPQL. Se aparecer
     * "operator does not exist: status_os = character varying", foi porque algum
     * ponto virou string.
     */

    /** Estado atual, sem recorte de periodo: quantas OS estao em cada status agora. */
    @Query("""
            select new dev.brunohm.bv2_projeto_software_uepg.dto.painel.projecao.ContagemStatusProjecao(
                       o.status, count(o))
            from OrdemServico o
            group by o.status
            """)
    List<ContagemStatusProjecao> contarPorStatus();

    /**
     * Faturamento realizado: a OS entregue e a que virou dinheiro, e o evento que a
     * data o periodo e a entrega. A contagem vem junto por ser o divisor do ticket
     * medio — nao vale uma segunda varredura da mesma faixa.
     *
     * <p>
     * Agregada sem group by devolve sempre uma linha, entao o retorno nunca e nulo;
     * o valorTotal e que vem null quando nenhuma OS caiu no periodo.
     */
    @Query("""
            select new dev.brunohm.bv2_projeto_software_uepg.dto.painel.projecao.TotaisEntreguesProjecao(
                       sum(o.valorTotal), count(o))
            from OrdemServico o
            where o.status = :status
              and o.dataEntregue between :dataInicio and :dataFim
            """)
    TotaisEntreguesProjecao resumirEntregues(@Param("status") StatusOs status,
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim);

    /**
     * Em aberto: trabalho que entrou no periodo e ainda nao foi entregue. Recorta
     * por dataEntrada porque e o unico evento datado que uma OS nao entregue tem.
     * CANCELADA nunca entra em faturamento.
     */
    @Query("""
            select sum(o.valorTotal)
            from OrdemServico o
            where o.status in :statusEmAberto
              and o.dataEntrada between :dataInicio and :dataFim
            """)
    BigDecimal somarEmAberto(@Param("statusEmAberto") Collection<StatusOs> statusEmAberto,
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim);

    /* Pela dataEntrada: a flag e definida na criacao ou enquanto a OS esta EM_ANDAMENTO. */
    long countByValorTotalManualTrueAndDataEntradaBetween(LocalDate dataInicio, LocalDate dataFim);

    /*
     * extract(year/month) e JPQL padrao e devolve Integer. date_trunc foi descartado:
     * amarraria a query ao Postgres via function() e ainda devolveria um timestamp
     * para converter em mes no Java.
     */
    @Query("""
            select new dev.brunohm.bv2_projeto_software_uepg.dto.painel.projecao.SerieOrdensProjecao(
                       extract(year from o.dataEntrada), extract(month from o.dataEntrada), count(o))
            from OrdemServico o
            where o.dataEntrada between :dataInicio and :dataFim
            group by extract(year from o.dataEntrada), extract(month from o.dataEntrada)
            """)
    List<SerieOrdensProjecao> serieOrdensAbertas(@Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim);

    @Query("""
            select new dev.brunohm.bv2_projeto_software_uepg.dto.painel.projecao.SerieFaturamentoProjecao(
                       extract(year from o.dataEntregue), extract(month from o.dataEntregue), sum(o.valorTotal))
            from OrdemServico o
            where o.status = :status
              and o.dataEntregue between :dataInicio and :dataFim
            group by extract(year from o.dataEntregue), extract(month from o.dataEntregue)
            """)
    List<SerieFaturamentoProjecao> serieFaturamento(@Param("status") StatusOs status,
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim);

    /**
     * Soma o valorTotal da OS, nunca os valores dos itens: valorTotalManual existe
     * justamente para os dois numeros divergirem (desconto, preco fechado).
     *
     * <p>
     * O desempate por nome mantem o ranking estavel quando dois clientes empatam no
     * valor. O Pageable precisa vir sem Sort, senao o Spring anexa a ordenacao dele
     * e substitui este order by.
     */
    @Query("""
            select new dev.brunohm.bv2_projeto_software_uepg.dto.painel.RankingFaturamentoResponse(
                       c.id, c.nome, sum(o.valorTotal), count(o))
            from OrdemServico o
            join o.cliente c
            where o.status = :status
              and o.dataEntregue between :dataInicio and :dataFim
            group by c.id, c.nome
            order by sum(o.valorTotal) desc, c.nome asc
            """)
    List<RankingFaturamentoResponse> rankingClientesPorFaturamento(@Param("status") StatusOs status,
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim,
            Pageable limite);
}
