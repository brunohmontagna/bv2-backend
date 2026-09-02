package dev.brunohm.bv2_projeto_software_uepg.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.brunohm.bv2_projeto_software_uepg.domain.entity.Cliente;
import dev.brunohm.bv2_projeto_software_uepg.dto.PaginaResponse;
import dev.brunohm.bv2_projeto_software_uepg.dto.cliente.ClienteAtualizacaoRequest;
import dev.brunohm.bv2_projeto_software_uepg.dto.cliente.ClienteCriacaoRequest;
import dev.brunohm.bv2_projeto_software_uepg.dto.cliente.ClienteResponse;
import dev.brunohm.bv2_projeto_software_uepg.exception.RecursoNaoEncontradoException;
import dev.brunohm.bv2_projeto_software_uepg.repository.ClienteRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

/**
 * Cliente aqui e o cliente da M2, um cadastro sem login. Nao ha checagem de
 * posse: qualquer usuario autenticado opera toda a carteira.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClienteService {

    private final ClienteRepository clienteRepository;

    @Transactional
    public ClienteResponse criar(ClienteCriacaoRequest request) {
        Cliente cliente = clienteRepository.save(Cliente.builder()
                .nome(request.nome())
                .telefone(request.telefone())
                .ativo(true)
                .build());

        return ClienteResponse.fromEntity(cliente);
    }

    public PaginaResponse<ClienteResponse> listar(String nome, Boolean ativo, Pageable pageable) {
        Page<Cliente> pagina = clienteRepository.findAll(filtrar(nome, ativo), pageable);
        return PaginaResponse.de(pagina, ClienteResponse::fromEntity);
    }

    public ClienteResponse buscarPorId(Long id) {
        return ClienteResponse.fromEntity(buscarEntidade(id));
    }

    @Transactional
    public ClienteResponse atualizar(Long id, ClienteAtualizacaoRequest request) {
        Cliente cliente = buscarEntidade(id);

        cliente.setNome(request.nome());
        cliente.setTelefone(request.telefone());

        return ClienteResponse.fromEntity(clienteRepository.save(cliente));
    }

    /** Idempotente: nao falha se o cliente ja estiver no estado pedido. */
    @Transactional
    public ClienteResponse alterarSituacao(Long id, boolean ativo) {
        Cliente cliente = buscarEntidade(id);
        cliente.setAtivo(ativo);
        return ClienteResponse.fromEntity(clienteRepository.save(cliente));
    }

    /**
     * As FKs de equipamentos, ordens_servico e notificacoes sao ON DELETE RESTRICT:
     * havendo vinculos, o flush lanca DataIntegrityViolationException, traduzida em
     * 409. Para o caso comum use desativar().
     */
    @Transactional
    public void excluir(Long id) {
        clienteRepository.delete(buscarEntidade(id));
        clienteRepository.flush();
    }

    private Cliente buscarEntidade(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Cliente", id));
    }

    private Specification<Cliente> filtrar(String nome, Boolean ativo) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (nome != null && !nome.isBlank()) {
                predicados.add(cb.like(cb.lower(root.get("nome")), "%" + nome.toLowerCase() + "%"));
            }
            if (ativo != null) {
                predicados.add(cb.equal(root.get("ativo"), ativo));
            }
            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }
}
