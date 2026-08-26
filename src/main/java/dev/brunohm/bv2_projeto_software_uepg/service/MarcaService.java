package dev.brunohm.bv2_projeto_software_uepg.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.brunohm.bv2_projeto_software_uepg.domain.entity.Marca;
import dev.brunohm.bv2_projeto_software_uepg.dto.PaginaResponse;
import dev.brunohm.bv2_projeto_software_uepg.dto.marca.MarcaRequest;
import dev.brunohm.bv2_projeto_software_uepg.dto.marca.MarcaResponse;
import dev.brunohm.bv2_projeto_software_uepg.exception.RecursoDuplicadoException;
import dev.brunohm.bv2_projeto_software_uepg.exception.RecursoNaoEncontradoException;
import dev.brunohm.bv2_projeto_software_uepg.repository.MarcaRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MarcaService {

    private final MarcaRepository marcaRepository;

    @Transactional
    public MarcaResponse criar(MarcaRequest request) {
        if (marcaRepository.existsByNomeIgnoreCase(request.nome())) {
            throw new RecursoDuplicadoException("Ja existe uma marca com o nome " + request.nome());
        }

        Marca marca = marcaRepository.save(Marca.builder()
                .nome(request.nome())
                .build());

        return MarcaResponse.fromEntity(marca);
    }

    public PaginaResponse<MarcaResponse> listar(String nome, Pageable pageable) {
        Page<Marca> pagina = marcaRepository.findAll(filtrar(nome), pageable);
        return PaginaResponse.de(pagina, MarcaResponse::fromEntity);
    }

    public MarcaResponse buscarPorId(Long id) {
        return MarcaResponse.fromEntity(buscarEntidade(id));
    }

    @Transactional
    public MarcaResponse atualizar(Long id, MarcaRequest request) {
        Marca marca = buscarEntidade(id);

        if (marcaRepository.existsByNomeIgnoreCaseAndIdNot(request.nome(), id)) {
            throw new RecursoDuplicadoException("Ja existe uma marca com o nome " + request.nome());
        }

        marca.setNome(request.nome());

        return MarcaResponse.fromEntity(marcaRepository.save(marca));
    }

    /**
     * A FK id_marca de equipamentos e ON DELETE RESTRICT: o flush explicito faz
     * a violacao aparecer como DataIntegrityViolationException (409) aqui, e nao
     * so no commit da transacao.
     */
    @Transactional
    public void excluir(Long id) {
        marcaRepository.delete(buscarEntidade(id));
        marcaRepository.flush();
    }

    private Marca buscarEntidade(Long id) {
        return marcaRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Marca", id));
    }

    private Specification<Marca> filtrar(String nome) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            if (nome != null && !nome.isBlank()) {
                predicados.add(cb.like(cb.lower(root.get("nome")), "%" + nome.toLowerCase() + "%"));
            }
            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }
}
