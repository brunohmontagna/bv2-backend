package dev.brunohm.bv2_projeto_software_uepg.service;

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
import dev.brunohm.bv2_projeto_software_uepg.domain.entity.Marca;
import dev.brunohm.bv2_projeto_software_uepg.dto.PaginaResponse;
import dev.brunohm.bv2_projeto_software_uepg.dto.equipamento.EquipamentoAtualizacaoRequest;
import dev.brunohm.bv2_projeto_software_uepg.dto.equipamento.EquipamentoCriacaoRequest;
import dev.brunohm.bv2_projeto_software_uepg.dto.equipamento.EquipamentoResponse;
import dev.brunohm.bv2_projeto_software_uepg.exception.RecursoNaoEncontradoException;
import dev.brunohm.bv2_projeto_software_uepg.exception.RegraDeNegocioException;
import dev.brunohm.bv2_projeto_software_uepg.repository.ClienteRepository;
import dev.brunohm.bv2_projeto_software_uepg.repository.EquipamentoRepository;
import dev.brunohm.bv2_projeto_software_uepg.repository.MarcaRepository;
import dev.brunohm.bv2_projeto_software_uepg.security.AutenticacaoAtual;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EquipamentoService {

    private final EquipamentoRepository equipamentoRepository;
    private final ClienteRepository clienteRepository;
    private final MarcaRepository marcaRepository;
    private final AutenticacaoAtual autenticacaoAtual;

    /**
     * O ADMIN informa o clienteId; o CLIENTE pode omiti-lo e o dono e resolvido
     * a partir do usuario autenticado.
     */
    @Transactional
    public EquipamentoResponse criar(EquipamentoCriacaoRequest request) {
        Cliente cliente = resolverCliente(request.clienteId());
        garantirAcessoAoCliente(cliente);

        if (Boolean.FALSE.equals(cliente.getAtivo())) {
            throw new RegraDeNegocioException(
                    "Nao e possivel cadastrar equipamento para um cliente inativo.");
        }

        Equipamento equipamento = equipamentoRepository.save(Equipamento.builder()
                .cliente(cliente)
                .marca(buscarMarca(request.marcaId()))
                .nome(request.nome())
                .build());

        return EquipamentoResponse.fromEntity(equipamento);
    }

    /**
     * Quem nao e ADMIN tem o filtro de cliente sobrescrito pelo proprio id: um
     * CLIENTE nunca lista equipamento alheio, mesmo passando ?clienteId de outro.
     */
    public PaginaResponse<EquipamentoResponse> listar(Long clienteId, Long marcaId, String nome, Pageable pageable) {
        Long clienteFiltrado = autenticacaoAtual.isAdmin()
                ? clienteId
                : clienteAutenticado().getId();

        Page<Equipamento> pagina = equipamentoRepository
                .findAll(filtrar(clienteFiltrado, marcaId, nome), pageable);
        return PaginaResponse.de(pagina, EquipamentoResponse::fromEntity);
    }

    public EquipamentoResponse buscarPorId(Long id) {
        Equipamento equipamento = buscarEntidade(id);
        garantirAcesso(equipamento);
        return EquipamentoResponse.fromEntity(equipamento);
    }

    @Transactional
    public EquipamentoResponse atualizar(Long id, EquipamentoAtualizacaoRequest request) {
        Equipamento equipamento = buscarEntidade(id);
        garantirAcesso(equipamento);

        equipamento.setMarca(buscarMarca(request.marcaId()));
        equipamento.setNome(request.nome());

        return EquipamentoResponse.fromEntity(equipamentoRepository.save(equipamento));
    }

    /**
     * A FK id_equipamento de itens_os e ON DELETE RESTRICT: o flush explicito faz
     * a violacao virar DataIntegrityViolationException (409) aqui.
     */
    @Transactional
    public void excluir(Long id) {
        Equipamento equipamento = buscarEntidade(id);
        garantirAcesso(equipamento);

        equipamentoRepository.delete(equipamento);
        equipamentoRepository.flush();
    }

    private Equipamento buscarEntidade(Long id) {
        return equipamentoRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Equipamento", id));
    }

    private Marca buscarMarca(Long id) {
        return marcaRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Marca", id));
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
    private void garantirAcesso(Equipamento equipamento) {
        if (autenticacaoAtual.isAdmin()) {
            return;
        }
        if (!equipamento.getCliente().getId().equals(clienteAutenticado().getId())) {
            throw new AccessDeniedException("Voce so pode acessar os proprios equipamentos.");
        }
    }

    private void garantirAcessoAoCliente(Cliente cliente) {
        if (autenticacaoAtual.isAdmin()) {
            return;
        }
        if (!cliente.getId().equals(clienteAutenticado().getId())) {
            throw new AccessDeniedException("Voce so pode cadastrar equipamentos no proprio cadastro.");
        }
    }

    private Specification<Equipamento> filtrar(Long clienteId, Long marcaId, String nome) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (clienteId != null) {
                predicados.add(cb.equal(root.get("cliente").get("id"), clienteId));
            }
            if (marcaId != null) {
                predicados.add(cb.equal(root.get("marca").get("id"), marcaId));
            }
            if (nome != null && !nome.isBlank()) {
                predicados.add(cb.like(cb.lower(root.get("nome")), "%" + nome.toLowerCase() + "%"));
            }
            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }
}
