package dev.brunohm.bv2_projeto_software_uepg.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

/**
 * Equipamento pertence a um cliente da M2, que nao e usuario do sistema. Nao ha
 * checagem de posse: MASTER e ADMIN operam todos os equipamentos.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EquipamentoService {

    private final EquipamentoRepository equipamentoRepository;
    private final ClienteRepository clienteRepository;
    private final MarcaRepository marcaRepository;

    @Transactional
    public EquipamentoResponse criar(EquipamentoCriacaoRequest request) {
        Cliente cliente = buscarCliente(request.clienteId());

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

    public PaginaResponse<EquipamentoResponse> listar(Long clienteId, Long marcaId, String nome, Pageable pageable) {
        Page<Equipamento> pagina = equipamentoRepository
                .findAll(filtrar(clienteId, marcaId, nome), pageable);
        return PaginaResponse.de(pagina, EquipamentoResponse::fromEntity);
    }

    public EquipamentoResponse buscarPorId(Long id) {
        return EquipamentoResponse.fromEntity(buscarEntidade(id));
    }

    @Transactional
    public EquipamentoResponse atualizar(Long id, EquipamentoAtualizacaoRequest request) {
        Equipamento equipamento = buscarEntidade(id);

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
        equipamentoRepository.delete(buscarEntidade(id));
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

    private Cliente buscarCliente(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Cliente", id));
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
