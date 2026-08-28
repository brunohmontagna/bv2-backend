package dev.brunohm.bv2_projeto_software_uepg.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.brunohm.bv2_projeto_software_uepg.domain.entity.Cliente;
import dev.brunohm.bv2_projeto_software_uepg.domain.entity.Equipamento;
import dev.brunohm.bv2_projeto_software_uepg.domain.entity.ItemOs;
import dev.brunohm.bv2_projeto_software_uepg.domain.entity.OrdemServico;
import dev.brunohm.bv2_projeto_software_uepg.domain.entity.Servico;
import dev.brunohm.bv2_projeto_software_uepg.domain.enums.StatusOs;
import dev.brunohm.bv2_projeto_software_uepg.dto.PaginaResponse;
import dev.brunohm.bv2_projeto_software_uepg.dto.ordemservico.ItemOsAtualizacaoRequest;
import dev.brunohm.bv2_projeto_software_uepg.dto.ordemservico.ItemOsCriacaoRequest;
import dev.brunohm.bv2_projeto_software_uepg.dto.ordemservico.ItemOsResponse;
import dev.brunohm.bv2_projeto_software_uepg.dto.ordemservico.OrdemServicoAtualizacaoRequest;
import dev.brunohm.bv2_projeto_software_uepg.dto.ordemservico.OrdemServicoCriacaoRequest;
import dev.brunohm.bv2_projeto_software_uepg.dto.ordemservico.OrdemServicoResponse;
import dev.brunohm.bv2_projeto_software_uepg.exception.RecursoDuplicadoException;
import dev.brunohm.bv2_projeto_software_uepg.exception.RecursoNaoEncontradoException;
import dev.brunohm.bv2_projeto_software_uepg.exception.RegraDeNegocioException;
import dev.brunohm.bv2_projeto_software_uepg.repository.ClienteRepository;
import dev.brunohm.bv2_projeto_software_uepg.repository.EquipamentoRepository;
import dev.brunohm.bv2_projeto_software_uepg.repository.ItemOsRepository;
import dev.brunohm.bv2_projeto_software_uepg.repository.OrdemServicoRepository;
import dev.brunohm.bv2_projeto_software_uepg.repository.ServicoRepository;
import dev.brunohm.bv2_projeto_software_uepg.security.AutenticacaoAtual;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrdemServicoService {

    private final OrdemServicoRepository ordemServicoRepository;
    private final ItemOsRepository itemOsRepository;
    private final ClienteRepository clienteRepository;
    private final EquipamentoRepository equipamentoRepository;
    private final ServicoRepository servicoRepository;
    private final AutenticacaoAtual autenticacaoAtual;

    // ------------------------------------------------------------------
    // Ordem de servico
    // ------------------------------------------------------------------

    /**
     * O ADMIN informa o clienteId; o CLIENTE pode omiti-lo e o dono e resolvido
     * a partir do usuario autenticado. A OS nasce sem itens, entao o valorTotal
     * comeca zerado (a coluna e NOT NULL) e so muda via itens.
     */
    @Transactional
    public OrdemServicoResponse criar(OrdemServicoCriacaoRequest request) {
        Cliente cliente = resolverCliente(request.clienteId());
        garantirAcessoAoCliente(cliente);

        if (Boolean.FALSE.equals(cliente.getAtivo())) {
            throw new RegraDeNegocioException(
                    "Nao e possivel abrir ordem de servico para um cliente inativo.");
        }

        OrdemServico ordemServico = ordemServicoRepository.save(OrdemServico.builder()
                .cliente(cliente)
                .observacao(request.observacao())
                .status(StatusOs.EM_ANDAMENTO)
                .dataEntrada(request.dataEntrada() != null ? request.dataEntrada() : LocalDate.now())
                .valorTotal(BigDecimal.ZERO)
                .build());

        return OrdemServicoResponse.fromEntity(ordemServico);
    }

    /**
     * Quem nao e ADMIN tem o filtro de cliente sobrescrito pelo proprio id: um
     * CLIENTE nunca lista ordem de servico alheia, mesmo passando ?clienteId de
     * outro.
     */
    public PaginaResponse<OrdemServicoResponse> listar(Long clienteId, StatusOs status,
            LocalDate dataInicio, LocalDate dataFim, Pageable pageable) {
        Long clienteFiltrado = autenticacaoAtual.isAdmin()
                ? clienteId
                : clienteAutenticado().getId();

        Page<OrdemServico> pagina = ordemServicoRepository
                .findAll(filtrar(clienteFiltrado, status, dataInicio, dataFim), pageable);
        return PaginaResponse.de(pagina, OrdemServicoResponse::fromEntity);
    }

    public OrdemServicoResponse buscarPorId(Long id) {
        OrdemServico ordemServico = buscarEntidade(id);
        garantirAcesso(ordemServico);
        return OrdemServicoResponse.fromEntity(ordemServico);
    }

    @Transactional
    public OrdemServicoResponse atualizar(Long id, OrdemServicoAtualizacaoRequest request) {
        OrdemServico ordemServico = buscarEntidade(id);
        garantirAcesso(ordemServico);
        garantirEmAndamento(ordemServico, "editar");

        ordemServico.setObservacao(request.observacao());

        return OrdemServicoResponse.fromEntity(ordemServicoRepository.save(ordemServico));
    }

    /**
     * A FK id_ordem_servico de itens_os e ON DELETE RESTRICT. O exists antecipado
     * troca o 409 generico do banco por uma mensagem que diz o que fazer; o flush
     * explicito cobre a corrida e faz a violacao virar
     * DataIntegrityViolationException (409) aqui, e nao no commit.
     */
    @Transactional
    public void excluir(Long id) {
        OrdemServico ordemServico = buscarEntidade(id);
        garantirAcesso(ordemServico);

        if (itemOsRepository.existsByOrdemServicoId(id)) {
            throw new RegraDeNegocioException(
                    "A ordem de servico possui itens. Remova os itens antes de exclui-la.");
        }

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
        garantirAcesso(ordemServico);

        StatusOs atual = ordemServico.getStatus();
        if (atual == destino) {
            return OrdemServicoResponse.fromEntity(ordemServico);
        }

        switch (atual) {
            case EM_ANDAMENTO -> {
                if (destino == StatusOs.ENTREGUE) {
                    throw new RegraDeNegocioException(
                            "A ordem de servico precisa ser concluida antes de ser entregue.");
                }
            }
            case CONCLUIDA -> {
                // CONCLUIDA aceita ENTREGUE e CANCELADA; nada a barrar.
            }
            case ENTREGUE -> throw new RegraDeNegocioException(
                    "Ordem de servico ja entregue nao pode ter o status alterado.");
            case CANCELADA -> throw new RegraDeNegocioException(
                    "Ordem de servico cancelada nao pode ser reaberta.");
        }

        if (destino == StatusOs.CONCLUIDA) {
            ordemServico.setDataConcluida(LocalDate.now());
        }
        if (destino == StatusOs.ENTREGUE) {
            ordemServico.setDataEntregue(LocalDate.now());
        }
        ordemServico.setStatus(destino);

        return OrdemServicoResponse.fromEntity(ordemServicoRepository.save(ordemServico));
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
        garantirAcesso(ordemServico);

        return itemOsRepository.findByOrdemServicoIdOrderByIdAsc(ordemServico.getId())
                .stream()
                .map(ItemOsResponse::fromEntity)
                .toList();
    }

    @Transactional
    public ItemOsResponse adicionarItem(Long ordemServicoId, ItemOsCriacaoRequest request) {
        OrdemServico ordemServico = buscarEntidade(ordemServicoId);
        garantirAcesso(ordemServico);
        garantirEmAndamento(ordemServico, "adicionar itens");

        Equipamento equipamento = buscarEquipamento(request.equipamentoId());
        if (!equipamento.getCliente().getId().equals(ordemServico.getCliente().getId())) {
            throw new RegraDeNegocioException(
                    "O equipamento nao pertence ao cliente desta ordem de servico.");
        }

        Servico servico = buscarServico(request.servicoId());
        if (Boolean.FALSE.equals(servico.getAtivo())) {
            throw new RegraDeNegocioException(
                    "Nao e possivel adicionar um servico inativo a ordem de servico.");
        }

        if (itemOsRepository.existsByOrdemServicoIdAndEquipamentoIdAndServicoId(
                ordemServico.getId(), equipamento.getId(), servico.getId())) {
            throw new RecursoDuplicadoException(
                    "Este servico ja foi lancado para este equipamento nesta ordem de servico.");
        }

        ItemOs item = itemOsRepository.save(ItemOs.builder()
                .ordemServico(ordemServico)
                .equipamento(equipamento)
                .servico(servico)
                .observacao(request.observacao())
                .build());

        servico.setContadorUso(servico.getContadorUso() + 1);
        servicoRepository.save(servico);

        recalcularValorTotal(ordemServico);

        return ItemOsResponse.fromEntity(item);
    }

    @Transactional
    public ItemOsResponse atualizarItem(Long ordemServicoId, Long itemId, ItemOsAtualizacaoRequest request) {
        OrdemServico ordemServico = buscarEntidade(ordemServicoId);
        garantirAcesso(ordemServico);
        garantirEmAndamento(ordemServico, "editar itens");

        ItemOs item = buscarItem(ordemServico, itemId);
        item.setObservacao(request.observacao());

        // Sem recalculo: a observacao nao altera o valor do item.
        return ItemOsResponse.fromEntity(itemOsRepository.save(item));
    }

    @Transactional
    public void removerItem(Long ordemServicoId, Long itemId) {
        OrdemServico ordemServico = buscarEntidade(ordemServicoId);
        garantirAcesso(ordemServico);
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
     * Unico ponto que escreve valorTotal. OrdemServico nao mapeia @OneToMany de
     * itens, entao a soma vem sempre do repositorio, ja com os servicos no
     * EntityGraph.
     */
    private void recalcularValorTotal(OrdemServico ordemServico) {
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
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Ordem de servico", id));
    }

    /**
     * Item de outra OS responde 404, e nao 403: pela rota informada ele nao
     * existe, e revelar a diferenca vazaria a existencia de itens alheios.
     */
    private ItemOs buscarItem(OrdemServico ordemServico, Long itemId) {
        ItemOs item = itemOsRepository.findById(itemId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Item da ordem de servico", itemId));

        if (!item.getOrdemServico().getId().equals(ordemServico.getId())) {
            throw RecursoNaoEncontradoException.de("Item da ordem de servico", itemId);
        }
        return item;
    }

    private Equipamento buscarEquipamento(Long id) {
        return equipamentoRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Equipamento", id));
    }

    private Servico buscarServico(Long id) {
        return servicoRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Servico", id));
    }

    private Cliente resolverCliente(Long clienteId) {
        if (clienteId == null) {
            return clienteAutenticado();
        }
        return clienteRepository.findById(clienteId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Cliente", clienteId));
    }

    private Cliente clienteAutenticado() {
        return clienteRepository.findByUsuarioId(autenticacaoAtual.usuario().getId())
                .orElseThrow(() -> new RegraDeNegocioException(
                        "O usuario autenticado nao possui cadastro de cliente. Informe o clienteId."));
    }

    /**
     * Comparacao pelo id do cliente, nao pelo do usuario: evita disparar o lazy
     * load de cliente.usuario so para conferir a posse.
     */
    private void garantirAcesso(OrdemServico ordemServico) {
        if (autenticacaoAtual.isAdmin()) {
            return;
        }
        if (!ordemServico.getCliente().getId().equals(clienteAutenticado().getId())) {
            throw new AccessDeniedException("Voce so pode acessar as proprias ordens de servico.");
        }
    }

    private void garantirAcessoAoCliente(Cliente cliente) {
        if (autenticacaoAtual.isAdmin()) {
            return;
        }
        if (!cliente.getId().equals(clienteAutenticado().getId())) {
            throw new AccessDeniedException("Voce so pode abrir ordens de servico no proprio cadastro.");
        }
    }

    /** A OS so e mutavel enquanto EM_ANDAMENTO; depois disso vira historico. */
    private void garantirEmAndamento(OrdemServico ordemServico, String acao) {
        if (ordemServico.getStatus() != StatusOs.EM_ANDAMENTO) {
            throw new RegraDeNegocioException(
                    "So e possivel " + acao + " enquanto a ordem de servico esta EM_ANDAMENTO. Status atual: "
                            + ordemServico.getStatus() + ".");
        }
    }

    private Specification<OrdemServico> filtrar(Long clienteId, StatusOs status,
            LocalDate dataInicio, LocalDate dataFim) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
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
