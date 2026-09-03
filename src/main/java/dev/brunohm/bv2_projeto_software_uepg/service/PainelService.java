package dev.brunohm.bv2_projeto_software_uepg.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.brunohm.bv2_projeto_software_uepg.domain.enums.StatusOs;
import dev.brunohm.bv2_projeto_software_uepg.dto.painel.FaturamentoResponse;
import dev.brunohm.bv2_projeto_software_uepg.dto.painel.PainelResponse;
import dev.brunohm.bv2_projeto_software_uepg.dto.painel.PeriodoResponse;
import dev.brunohm.bv2_projeto_software_uepg.dto.painel.RankingsResponse;
import dev.brunohm.bv2_projeto_software_uepg.dto.painel.ResumoGeralResponse;
import dev.brunohm.bv2_projeto_software_uepg.dto.painel.SerieMensalResponse;
import dev.brunohm.bv2_projeto_software_uepg.dto.painel.projecao.SerieFaturamentoProjecao;
import dev.brunohm.bv2_projeto_software_uepg.dto.painel.projecao.SerieOrdensProjecao;
import dev.brunohm.bv2_projeto_software_uepg.dto.painel.projecao.TotaisEntreguesProjecao;
import dev.brunohm.bv2_projeto_software_uepg.exception.RegraDeNegocioException;
import dev.brunohm.bv2_projeto_software_uepg.repository.ClienteRepository;
import dev.brunohm.bv2_projeto_software_uepg.repository.EquipamentoRepository;
import dev.brunohm.bv2_projeto_software_uepg.repository.ItemOsRepository;
import dev.brunohm.bv2_projeto_software_uepg.repository.OrdemServicoRepository;
import dev.brunohm.bv2_projeto_software_uepg.repository.ServicoRepository;
import lombok.RequiredArgsConstructor;

/**
 * Consolida os indicadores da operacao para o dashboard. Somente leitura: nao ha
 * escrita nem estado proprio.
 *
 * <p>
 * <b>Qual data recorta cada metrica.</b> Cada numero usa a data do evento que
 * mede, e nao uma data unica para tudo: entrada de trabalho e execucao de servico
 * por {@code dataEntrada}, faturamento realizado e entrega por
 * {@code dataEntregue}. Faturamento em aberto e a contagem de valor fixado a mao
 * ficam com {@code dataEntrada} por eliminacao — uma OS que ainda nao foi entregue
 * nao tem outro evento datado. A alternativa, trata-las como fotografia sem
 * recorte, faria um periodo de 2024 devolver o pipeline de hoje.
 *
 * <p>
 * <b>Doze consultas por requisicao</b>, todas na mesma transacao para o painel sair
 * de um snapshot so. O numero e fixo e nao cresce com o volume — nao ha N+1 aqui.
 * Se um dia pesar, o caminho e agregacao condicional (sum com case) juntando
 * {@code resumirEntregues}, {@code somarEmAberto} e a contagem de valor manual numa
 * varredura; hoje isso trocaria tres consultas legiveis por um quebra-cabeca.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PainelService {

    /** Sem query param: mais superficie de API do que o ganho justifica. */
    private static final int TOP_N = 5;

    private static final int JANELA_PADRAO_DIAS = 30;

    /** OS ja aberta e ainda nao entregue. CANCELADA nunca entra em faturamento. */
    private static final Set<StatusOs> STATUS_EM_ABERTO = EnumSet.of(StatusOs.EM_ANDAMENTO, StatusOs.CONCLUIDA);

    private final OrdemServicoRepository ordemServicoRepository;
    private final ItemOsRepository itemOsRepository;
    private final ClienteRepository clienteRepository;
    private final EquipamentoRepository equipamentoRepository;
    private final ServicoRepository servicoRepository;

    public PainelResponse consultar(LocalDate dataInicio, LocalDate dataFim) {
        PeriodoResponse periodo = resolverPeriodo(dataInicio, dataFim);

        return new PainelResponse(
                periodo,
                montarResumo(),
                montarFaturamento(periodo),
                montarSerieMensal(periodo),
                montarRankings(periodo));
    }

    /**
     * Periodo invertido responde 422 em vez de devolver um painel zerado, que o
     * usuario leria como "nao houve movimento" em vez de "a data esta errada".
     *
     * <p>
     * O inicio padrao ancora no fim informado, e nao em hoje: com
     * {@code ?dataFim=2024-01-01} sozinho, ancorar em hoje produziria um intervalo
     * invertido artificial.
     */
    private PeriodoResponse resolverPeriodo(LocalDate dataInicio, LocalDate dataFim) {
        LocalDate fim = dataFim != null ? dataFim : LocalDate.now();
        LocalDate inicio = dataInicio != null ? dataInicio : fim.minusDays(JANELA_PADRAO_DIAS);

        if (inicio.isAfter(fim)) {
            throw new RegraDeNegocioException("A data inicial nao pode ser posterior a data final.");
        }

        return new PeriodoResponse(inicio, fim);
    }

    /** Fotografia de agora: quantas OS estao em cada status e o tamanho dos cadastros. */
    private ResumoGeralResponse montarResumo() {
        return ResumoGeralResponse.consolidar(
                ordemServicoRepository.contarPorStatus(),
                clienteRepository.countByAtivoTrue(),
                equipamentoRepository.count(),
                servicoRepository.countByAtivoTrue());
    }

    private FaturamentoResponse montarFaturamento(PeriodoResponse periodo) {
        TotaisEntreguesProjecao entregues = ordemServicoRepository.resumirEntregues(
                StatusOs.ENTREGUE, periodo.dataInicio(), periodo.dataFim());

        BigDecimal emAberto = ordemServicoRepository.somarEmAberto(
                STATUS_EM_ABERTO, periodo.dataInicio(), periodo.dataFim());

        long comValorManual = ordemServicoRepository.countByValorTotalManualTrueAndDataEntradaBetween(
                periodo.dataInicio(), periodo.dataFim());

        // O tratamento de sum() nulo e do divisor zerado mora no proprio record.
        return FaturamentoResponse.calcular(
                entregues.valorTotal(), emAberto, entregues.quantidade(), comValorManual);
    }

    /**
     * Agrupa no banco e completa em Java: o group by so devolve mes que teve
     * movimento, e o grafico precisa dos vazios para nao abrir buracos na linha.
     */
    private List<SerieMensalResponse> montarSerieMensal(PeriodoResponse periodo) {
        Map<YearMonth, Long> ordens = ordemServicoRepository
                .serieOrdensAbertas(periodo.dataInicio(), periodo.dataFim())
                .stream()
                .collect(Collectors.toMap(PainelService::mesDe, SerieOrdensProjecao::quantidade));

        Map<YearMonth, BigDecimal> faturamento = ordemServicoRepository
                .serieFaturamento(StatusOs.ENTREGUE, periodo.dataInicio(), periodo.dataFim())
                .stream()
                .collect(Collectors.toMap(PainelService::mesDe, SerieFaturamentoProjecao::valorTotal));

        List<SerieMensalResponse> serie = new ArrayList<>();
        YearMonth ultimo = YearMonth.from(periodo.dataFim());
        for (YearMonth mes = YearMonth.from(periodo.dataInicio()); !mes.isAfter(ultimo); mes = mes.plusMonths(1)) {
            serie.add(SerieMensalResponse.de(mes,
                    ordens.getOrDefault(mes, 0L),
                    faturamento.getOrDefault(mes, BigDecimal.ZERO)));
        }
        return serie;
    }

    /* Pageable.ofSize sem Sort: um Sort aqui substituiria o order by da query. */
    private RankingsResponse montarRankings(PeriodoResponse periodo) {
        Pageable limite = Pageable.ofSize(TOP_N);

        return new RankingsResponse(
                itemOsRepository.rankingServicosMaisExecutados(
                        periodo.dataInicio(), periodo.dataFim(), StatusOs.CANCELADA, limite),
                ordemServicoRepository.rankingClientesPorFaturamento(
                        StatusOs.ENTREGUE, periodo.dataInicio(), periodo.dataFim(), limite),
                itemOsRepository.rankingMarcasMaisAtendidas(
                        periodo.dataInicio(), periodo.dataFim(), StatusOs.CANCELADA, limite));
    }

    private static YearMonth mesDe(SerieOrdensProjecao projecao) {
        return YearMonth.of(projecao.ano(), projecao.mes());
    }

    private static YearMonth mesDe(SerieFaturamentoProjecao projecao) {
        return YearMonth.of(projecao.ano(), projecao.mes());
    }
}
