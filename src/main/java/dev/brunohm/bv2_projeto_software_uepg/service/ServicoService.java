package dev.brunohm.bv2_projeto_software_uepg.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.brunohm.bv2_projeto_software_uepg.domain.entity.Servico;
import dev.brunohm.bv2_projeto_software_uepg.dto.PaginaResponse;
import dev.brunohm.bv2_projeto_software_uepg.dto.servico.ServicoRequest;
import dev.brunohm.bv2_projeto_software_uepg.dto.servico.ServicoResponse;
import dev.brunohm.bv2_projeto_software_uepg.exception.RecursoDuplicadoException;
import dev.brunohm.bv2_projeto_software_uepg.exception.RecursoNaoEncontradoException;
import dev.brunohm.bv2_projeto_software_uepg.repository.ServicoRepository;
import dev.brunohm.bv2_projeto_software_uepg.security.ContaAtual;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

/**
 * Catalogo de servicos de uma conta. Leitura e escrita restritas a conta da
 * requisicao (ContaAtual); servico de outra conta responde 404.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServicoService {

    private final ServicoRepository servicoRepository;
    private final ContaAtual contaAtual;

    /** Nasce ativo e com contador zerado (defaults da entidade). */
    @Transactional
    public ServicoResponse criar(ServicoRequest request) {
        Long usuarioId = contaAtual.id();
        if (servicoRepository.existsByUsuarioIdAndNomeIgnoreCase(usuarioId, request.nome())) {
            throw new RecursoDuplicadoException(
                    "Já existe um serviço chamado '" + request.nome() + "'.");
        }

        Servico servico = servicoRepository.save(Servico.builder()
                .usuarioId(usuarioId)
                .nome(request.nome())
                .descricao(request.descricao())
                .valor(request.valor())
                .build());

        return ServicoResponse.fromEntity(servico);
    }

    public PaginaResponse<ServicoResponse> listar(String nome, String busca, Boolean ativo,
            Pageable pageable) {
        Page<Servico> pagina = servicoRepository.findAll(filtrar(nome, busca, ativo), pageable);
        return PaginaResponse.de(pagina, ServicoResponse::fromEntity);
    }

    public ServicoResponse buscarPorId(Long id) {
        return ServicoResponse.fromEntity(buscarEntidade(id));
    }

    /** Nao toca em ativo nem em contadorUso: nenhum dos dois vem do request. */
    @Transactional
    public ServicoResponse atualizar(Long id, ServicoRequest request) {
        Servico servico = buscarEntidade(id);

        if (servicoRepository.existsByUsuarioIdAndNomeIgnoreCaseAndIdNot(
                servico.getUsuarioId(), request.nome(), id)) {
            throw new RecursoDuplicadoException(
                    "Já existe outro serviço chamado '" + request.nome() + "'.");
        }

        servico.setNome(request.nome());
        servico.setDescricao(request.descricao());
        servico.setValor(request.valor());

        return ServicoResponse.fromEntity(servicoRepository.save(servico));
    }

    /** Idempotente: nao falha se o servico ja estiver no estado pedido. */
    @Transactional
    public ServicoResponse alterarSituacao(Long id, boolean ativo) {
        Servico servico = buscarEntidade(id);
        servico.setAtivo(ativo);
        return ServicoResponse.fromEntity(servicoRepository.save(servico));
    }

    /**
     * A FK id_servico de itens_os e ON DELETE RESTRICT: o flush explicito faz a
     * violacao virar DataIntegrityViolationException (409) aqui. Para o caso
     * comum use desativar().
     */
    @Transactional
    public void excluir(Long id) {
        servicoRepository.delete(buscarEntidade(id));
        servicoRepository.flush();
    }

    private Servico buscarEntidade(Long id) {
        return servicoRepository.findById(id)
                .filter(servico -> servico.getUsuarioId().equals(contaAtual.id()))
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Serviço", id));
    }

    /* busca e o campo livre da tela: parte do nome ou da descricao. */
    private Specification<Servico> filtrar(String nome, String busca, Boolean ativo) {
        Long usuarioId = contaAtual.id();
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            predicados.add(cb.equal(root.get("usuarioId"), usuarioId));
            if (nome != null && !nome.isBlank()) {
                predicados.add(cb.like(cb.lower(root.get("nome")), "%" + nome.toLowerCase() + "%"));
            }
            if (busca != null && !busca.isBlank()) {
                predicados.add(cb.or(
                        BuscaTexto.contem(cb, root.get("nome"), busca),
                        BuscaTexto.contem(cb, root.get("descricao"), busca)));
            }
            if (ativo != null) {
                predicados.add(cb.equal(root.get("ativo"), ativo));
            }
            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }
}
