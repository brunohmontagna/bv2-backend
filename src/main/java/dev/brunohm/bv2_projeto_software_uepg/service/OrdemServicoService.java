package dev.brunohm.bv2_projeto_software_uepg.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.brunohm.bv2_projeto_software_uepg.domain.entity.Cliente;
import dev.brunohm.bv2_projeto_software_uepg.domain.entity.Equipamento;
import dev.brunohm.bv2_projeto_software_uepg.domain.entity.ItemOs;
import dev.brunohm.bv2_projeto_software_uepg.domain.entity.OrdemServico;
import dev.brunohm.bv2_projeto_software_uepg.domain.entity.Servico;
import dev.brunohm.bv2_projeto_software_uepg.domain.enums.StatusOs;
import dev.brunohm.bv2_projeto_software_uepg.domain.evento.OrdemServicoStatusAlteradoEvent;
import dev.brunohm.bv2_projeto_software_uepg.dto.PaginaResponse;
import dev.brunohm.bv2_projeto_software_uepg.dto.ordemservico.ItemOsAtualizacaoRequest;
import dev.brunohm.bv2_projeto_software_uepg.dto.ordemservico.ItemOsCriacaoRequest;
import dev.brunohm.bv2_projeto_software_uepg.dto.ordemservico.ItemOsResponse;
import dev.brunohm.bv2_projeto_software_uepg.dto.ordemservico.OrdemServicoAtualizacaoRequest;
import dev.brunohm.bv2_projeto_software_uepg.dto.ordemservico.OrdemServicoCriacaoRequest;
import dev.brunohm.bv2_projeto_software_uepg.dto.ordemservico.OrdemServicoResponse;
import dev.brunohm.bv2_projeto_software_uepg.dto.ordemservico.OrdemServicoValorTotalRequest;
import dev.brunohm.bv2_projeto_software_uepg.exception.RecursoDuplicadoException;
import dev.brunohm.bv2_projeto_software_uepg.exception.RecursoNaoEncontradoException;
import dev.brunohm.bv2_projeto_software_uepg.exception.RegraDeNegocioException;
import dev.brunohm.bv2_projeto_software_uepg.repository.ClienteRepository;
import dev.brunohm.bv2_projeto_software_uepg.repository.EquipamentoRepository;
import dev.brunohm.bv2_projeto_software_uepg.repository.ItemOsRepository;
import dev.brunohm.bv2_projeto_software_uepg.repository.NotificacaoRepository;
import dev.brunohm.bv2_projeto_software_uepg.repository.OrdemServicoRepository;
import dev.brunohm.bv2_projeto_software_uepg.repository.ServicoRepository;
import dev.brunohm.bv2_projeto_software_uepg.security.ContaAtual;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

/**
 * A OS pertence a um cliente e herda dele a conta dona. Tudo fica restrito a conta
 * da requisicao (ContaAtual): OS, cliente, equipamento ou servico de outra conta
 * responde 404. Como as buscas de apoio ja filtram pela conta, nao ha como montar
 * uma OS misturando cadastros de contas diferentes.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrdemServicoService {

    private final OrdemServicoRepository ordemServicoRepository;
    private final ItemOsRepository itemOsRepository;
    private final ClienteRepository clienteRepository;
    private final EquipamentoRepository equipamentoRepository;
    private final ServicoRepository servicoRepository;
    private final NotificacaoRepository notificacaoRepository;
    private final ApplicationEventPublisher eventos;
    private final ContaAtual contaAtual;

    // ------------------------------------------------------------------
    // Ordem de servico
    // ------------------------------------------------------------------

    /**
     * A OS nasce com pelo menos um item (@NotEmpty no request): uma ordem sem
     * servico lancado nao representa trabalho nenhum. A linha e salva antes dos
     * itens porque eles precisam do id da OS, entao o valorTotal comeca zerado (a
     * coluna e NOT NULL) e recebe a soma logo em seguida.
     */
    @Transactional
    public OrdemServicoResponse criar(OrdemServicoCriacaoRequest request) {
        Cliente cliente = buscarCliente(request.clienteId());

        if (Boolean.FALSE.equals(cliente.getAtivo())) {
            throw new RegraDeNegocioException(
                    "Não é possível abrir ordem de serviço para um cliente inativo.");
        }

        OrdemServico ordemServico = ordemServicoRepository.save(OrdemServico.builder()
                .cliente(cliente)
                .observacao(request.observacao())
                .status(StatusOs.EM_ANDAMENTO)
                .dataEntrada(request.dataEntrada() != null ? request.dataEntrada() : LocalDate.now())
                .valorTotal(BigDecimal.ZERO)
                .valorTotalManual(false)
                .build());

        // Cada item passa pelas mesmas validacoes do POST de item; a soma vira o
        // valorTotal logo abaixo.
        for (ItemOsCriacaoRequest item : request.itens()) {
            criarItem(ordemServico, item);
        }

        // valorTotal enviado a mao congela o total; senao, soma dos servicos dos itens.
        if (request.valorTotal() != null) {
            ordemServico.setValorTotalManual(true);
            ordemServico.setValorTotal(request.valorTotal());
            ordemServicoRepository.save(ordemServico);
        } else {
            recalcularValorTotal(ordemServico);
        }

        return OrdemServicoResponse.fromEntity(ordemServico);
    }

    /**
     * Fixa ou reseta o valorTotal a mao. Um valor congela o total (para de
     * recalcular pelos itens); null volta ao automatico e recalcula na hora.
     * So enquanto EM_ANDAMENTO, como toda mutacao da OS.
     */
    @Transactional
    public OrdemServicoResponse definirValorTotal(Long id, OrdemServicoValorTotalRequest request) {
        OrdemServico ordemServico = buscarEntidade(id);
        garantirEmAndamento(ordemServico, "alterar o valor total");

        if (request.valorTotal() != null) {
            ordemServico.setValorTotalManual(true);
            ordemServico.setValorTotal(request.valorTotal());
            ordemServicoRepository.save(ordemServico);
        } else {
            ordemServico.setValorTotalManual(false);
            recalcularValorTotal(ordemServico);
        }

        return OrdemServicoResponse.fromEntity(ordemServico);
    }

    public PaginaResponse<OrdemServicoResponse> listar(Long clienteId, StatusOs status,
            LocalDate dataInicio, LocalDate dataFim, Pageable pageable) {
        Page<OrdemServico> pagina = ordemServicoRepository
                .findAll(filtrar(clienteId, status, dataInicio, dataFim), pageable);
        return PaginaResponse.de(pagina, OrdemServicoResponse::fromEntity);
    }

    public OrdemServicoResponse buscarPorId(Long id) {
        return OrdemServicoResponse.fromEntity(buscarEntidade(id));
    }

    @Transactional
    public OrdemServicoResponse atualizar(Long id, OrdemServicoAtualizacaoRequest request) {
        OrdemServico ordemServico = buscarEntidade(id);
        garantirEmAndamento(ordemServico, "editar");

        ordemServico.setObservacao(request.observacao());

        return OrdemServicoResponse.fromEntity(ordemServicoRepository.save(ordemServico));
    }

    /**
     * Exclusao definitiva: leva junto os itens e o log de notificacoes da ordem.
     * Nao ha desfazer — o caminho normal para tirar uma OS de circulacao continua
     * sendo cancelar, que preserva o historico.
     *
     * <p>
     * So EM_ANDAMENTO e CANCELADA. CONCLUIDA e ENTREGUE carregam faturamento: apagar
     * uma delas reescreveria o painel de um periodo ja fechado, em silencio.
     *
     * <p>
     * As FKs de itens_os e notificacoes sao ON DELETE RESTRICT de proposito, entao a
     * limpeza e explicita e na ordem das dependencias. O contadorUso dos servicos e
     * devolvido item a item: sem isso o ranking de mais executados contaria para
     * sempre uma execucao que deixou de existir.
     */
    @Transactional
    public void excluir(Long id) {
        OrdemServico ordemServico = buscarEntidade(id);

        StatusOs status = ordemServico.getStatus();
        if (status == StatusOs.CONCLUIDA || status == StatusOs.ENTREGUE) {
            throw new RegraDeNegocioException(
                    "Ordem de serviço " + status + " não pode ser excluída, porque entra no faturamento. "
                            + "Para tirá-la de circulação, cancele-a.");
        }

        List<ItemOs> itens = itemOsRepository.findByOrdemServicoIdOrderByIdAsc(id);
        for (ItemOs item : itens) {
            Servico servico = item.getServico();
            // Piso em zero por causa do chk_servicos_contador_uso_positivo.
            servico.setContadorUso(Math.max(0, servico.getContadorUso() - 1));
            servicoRepository.save(servico);
        }
        itemOsRepository.deleteAll(itens);
        notificacaoRepository.excluirDaOrdemServico(id);
        itemOsRepository.flush();

        ordemServicoRepository.delete(ordemServico);
        ordemServicoRepository.flush();
    }

    @Transactional
    public OrdemServicoResponse concluir(Long id) {
        return transicionar(id, StatusOs.CONCLUIDA);
    }

    @Transactional
    public OrdemServicoResponse entregar(Long id) {
        return transicionar(id, StatusOs.ENTREGUE);
    }

    @Transactional
    public OrdemServicoResponse cancelar(Long id) {
        return transicionar(id, StatusOs.CANCELADA);
    }

    /**
     * Maquina de estados da OS. Espelha o trigger trg_datas_os (V6) em Java para
     * devolver 422 com mensagem legivel em vez de deixar o Postgres estourar um
     * RAISE EXCEPTION generico. Repetir o status atual e no-op (idempotente).
     *
     * <p>
     * As datas sao preenchidas aqui, e nao apenas pelo trigger: o trigger altera
     * a linha no banco, mas a entidade em memoria continuaria com null e a
     * resposta HTTP sairia sem dataConcluida/dataEntregue. O trigger grava o mesmo
     * CURRENT_DATE e permanece como rede de seguranca.
     */
    private OrdemServicoResponse transicionar(Long id, StatusOs destino) {
        OrdemServico ordemServico = buscarEntidade(id);

        StatusOs atual = ordemServico.getStatus();
        if (atual == destino) {
            return OrdemServicoResponse.fromEntity(ordemServico);
        }

        switch (atual) {
            case EM_ANDAMENTO -> {
                if (destino == StatusOs.ENTREGUE) {
                    throw new RegraDeNegocioException(
                            "A ordem de serviço precisa ser concluída antes de ser entregue.");
                }
            }
            case CONCLUIDA -> {
                // CONCLUIDA aceita ENTREGUE e CANCELADA; nada a barrar.
            }
            case ENTREGUE -> throw new RegraDeNegocioException(
                    "Ordem de serviço já entregue não pode ter o status alterado.");
            case CANCELADA -> throw new RegraDeNegocioException(
                    "Ordem de serviço cancelada não pode ser reaberta.");
        }

        if (destino == StatusOs.CONCLUIDA) {
            ordemServico.setDataConcluida(LocalDate.now());
        }
        if (destino == StatusOs.ENTREGUE) {
            ordemServico.setDataEntregue(LocalDate.now());
        }
        ordemServico.setStatus(destino);

        OrdemServicoResponse resposta = OrdemServicoResponse.fromEntity(ordemServicoRepository.save(ordemServico));

        /*
         * Dispara a notificacao ao cliente. O consumidor roda em AFTER_COMMIT, entao
         * o WhatsApp so sai se esta transacao realmente for adiante — e uma falha do
         * n8n nao volta como erro desta requisicao.
         *
         * Fica depois do return idempotente la em cima (atual == destino): repetir o
         * status atual e no-op e nao pode remandar mensagem para o cliente.
         */
        eventos.publishEvent(new OrdemServicoStatusAlteradoEvent(ordemServico.getId(), atual, destino));

        return resposta;
    }

    // ------------------------------------------------------------------
    // Itens da ordem de servico (subrecurso de composicao)
    // ------------------------------------------------------------------

    /**
     * Sem paginacao: os itens sao a composicao de uma unica OS, uma colecao
     * naturalmente pequena e sempre consumida por inteiro junto com a OS.
     */
    public List<ItemOsResponse> listarItens(Long ordemServicoId) {
        OrdemServico ordemServico = buscarEntidade(ordemServicoId);

        return itemOsRepository.findByOrdemServicoIdOrderByIdAsc(ordemServico.getId())
                .stream()
                .map(ItemOsResponse::fromEntity)
                .toList();
    }

    @Transactional
    public ItemOsResponse adicionarItem(Long ordemServicoId, ItemOsCriacaoRequest request) {
        OrdemServico ordemServico = buscarEntidade(ordemServicoId);
        garantirEmAndamento(ordemServico, "adicionar itens");

        ItemOs item = criarItem(ordemServico, request);

        recalcularValorTotal(ordemServico);

        return ItemOsResponse.fromEntity(item);
    }

    /**
     * Cria e persiste um item, com todas as validacoes de posse/estado do servico,
     * e incrementa o contadorUso. Nao recalcula o valorTotal: quem chama decide
     * quando (uma vez ao final, na abertura em lote ou no POST de item avulso).
     */
    private ItemOs criarItem(OrdemServico ordemServico, ItemOsCriacaoRequest request) {
        Equipamento equipamento = buscarEquipamento(request.equipamentoId());
        if (!equipamento.getCliente().getId().equals(ordemServico.getCliente().getId())) {
            throw new RegraDeNegocioException(
                    "O equipamento não pertence ao cliente desta ordem de serviço.");
        }

        Servico servico = buscarServico(request.servicoId());
        if (Boolean.FALSE.equals(servico.getAtivo())) {
            throw new RegraDeNegocioException(
                    "Não é possível adicionar um serviço inativo à ordem de serviço.");
        }

        if (itemOsRepository.existsByOrdemServicoIdAndEquipamentoIdAndServicoId(
                ordemServico.getId(), equipamento.getId(), servico.getId())) {
            throw new RecursoDuplicadoException(
                    "Este serviço já foi lançado para este equipamento nesta ordem de serviço.");
        }

        ItemOs item = itemOsRepository.save(ItemOs.builder()
                .ordemServico(ordemServico)
                .equipamento(equipamento)
                .servico(servico)
                .observacao(request.observacao())
                .build());

        servico.setContadorUso(servico.getContadorUso() + 1);
        servicoRepository.save(servico);

        return item;
    }

    @Transactional
    public ItemOsResponse atualizarItem(Long ordemServicoId, Long itemId, ItemOsAtualizacaoRequest request) {
        OrdemServico ordemServico = buscarEntidade(ordemServicoId);
        garantirEmAndamento(ordemServico, "editar itens");

        ItemOs item = buscarItem(ordemServico, itemId);
        item.setObservacao(request.observacao());

        // Sem recalculo: a observacao nao altera o valor do item.
        return ItemOsResponse.fromEntity(itemOsRepository.save(item));
    }

    @Transactional
    public void removerItem(Long ordemServicoId, Long itemId) {
        OrdemServico ordemServico = buscarEntidade(ordemServicoId);
        garantirEmAndamento(ordemServico, "remover itens");

        ItemOs item = buscarItem(ordemServico, itemId);
        Servico servico = item.getServico();

        itemOsRepository.delete(item);
        itemOsRepository.flush();

        // Piso em zero por causa do chk_servicos_contador_uso_positivo.
        servico.setContadorUso(Math.max(0, servico.getContadorUso() - 1));
        servicoRepository.save(servico);

        recalcularValorTotal(ordemServico);
    }

    /**
     * Recalcula o valorTotal como a soma dos servicos dos itens. OrdemServico nao
     * mapeia @OneToMany de itens, entao a soma vem sempre do repositorio, ja com os
     * servicos no EntityGraph.
     *
     * <p>
     * No-op quando valorTotalManual e TRUE: o total foi fixado a mao e so um reset
     * (definirValorTotal com null) volta a deixar os itens mandarem nele.
     */
    private void recalcularValorTotal(OrdemServico ordemServico) {
        if (Boolean.TRUE.equals(ordemServico.getValorTotalManual())) {
            return;
        }

        BigDecimal total = itemOsRepository.findByOrdemServicoIdOrderByIdAsc(ordemServico.getId())
                .stream()
                .map(item -> item.getServico().getValor())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        ordemServico.setValorTotal(total);
        ordemServicoRepository.save(ordemServico);
    }

    // ------------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------------

    private OrdemServico buscarEntidade(Long id) {
        return ordemServicoRepository.findById(id)
                .filter(ordemServico -> ordemServico.getCliente().getUsuarioId().equals(contaAtual.id()))
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Ordem de serviço", id));
    }

    /**
     * Item de outra OS responde 404: pela rota informada ele de fato nao existe,
     * e o id do item sozinho nao enderecca nada.
     */
    private ItemOs buscarItem(OrdemServico ordemServico, Long itemId) {
        ItemOs item = itemOsRepository.findById(itemId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Item da ordem de serviço", itemId));

        if (!item.getOrdemServico().getId().equals(ordemServico.getId())) {
            throw RecursoNaoEncontradoException.de("Item da ordem de serviço", itemId);
        }
        return item;
    }

    private Equipamento buscarEquipamento(Long id) {
        return equipamentoRepository.findById(id)
                .filter(equipamento -> equipamento.getCliente().getUsuarioId().equals(contaAtual.id()))
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Equipamento", id));
    }

    private Servico buscarServico(Long id) {
        return servicoRepository.findById(id)
                .filter(servico -> servico.getUsuarioId().equals(contaAtual.id()))
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Serviço", id));
    }

    private Cliente buscarCliente(Long clienteId) {
        return clienteRepository.findById(clienteId)
                .filter(cliente -> cliente.getUsuarioId().equals(contaAtual.id()))
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Cliente", clienteId));
    }

    /** A OS so e mutavel enquanto EM_ANDAMENTO; depois disso vira historico. */
    private void garantirEmAndamento(OrdemServico ordemServico, String acao) {
        if (ordemServico.getStatus() != StatusOs.EM_ANDAMENTO) {
            throw new RegraDeNegocioException(
                    "Só é possível " + acao + " enquanto a ordem de serviço está EM_ANDAMENTO. Status atual: "
                            + ordemServico.getStatus() + ".");
        }
    }

    private Specification<OrdemServico> filtrar(Long clienteId, StatusOs status,
            LocalDate dataInicio, LocalDate dataFim) {
        Long usuarioId = contaAtual.id();
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            predicados.add(cb.equal(root.get("cliente").get("usuarioId"), usuarioId));
            if (clienteId != null) {
                predicados.add(cb.equal(root.get("cliente").get("id"), clienteId));
            }
            if (status != null) {
                predicados.add(cb.equal(root.get("status"), status));
            }
            if (dataInicio != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.get("dataEntrada"), dataInicio));
            }
            if (dataFim != null) {
                predicados.add(cb.lessThanOrEqualTo(root.get("dataEntrada"), dataFim));
            }
            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }
}
