package dev.brunohm.bv2_projeto_software_uepg.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.brunohm.bv2_projeto_software_uepg.domain.entity.Usuario;
import dev.brunohm.bv2_projeto_software_uepg.domain.enums.RoleUsuario;
import dev.brunohm.bv2_projeto_software_uepg.dto.PaginaResponse;
import dev.brunohm.bv2_projeto_software_uepg.dto.usuario.UsuarioAtualizacaoRequest;
import dev.brunohm.bv2_projeto_software_uepg.dto.usuario.UsuarioCriacaoRequest;
import dev.brunohm.bv2_projeto_software_uepg.dto.usuario.UsuarioResponse;
import dev.brunohm.bv2_projeto_software_uepg.exception.RecursoDuplicadoException;
import dev.brunohm.bv2_projeto_software_uepg.exception.RecursoNaoEncontradoException;
import dev.brunohm.bv2_projeto_software_uepg.exception.RegraDeNegocioException;
import dev.brunohm.bv2_projeto_software_uepg.repository.UsuarioRepository;
import dev.brunohm.bv2_projeto_software_uepg.security.AutenticacaoAtual;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

/**
 * Cadastro dos usuarios do sistema. E o unico recurso com restricao de papel: so
 * o MASTER enxerga a lista. O ADMIN chega aqui apenas pelo "eu".
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AutenticacaoAtual autenticacaoAtual;

    /** Todo usuario criado pela API nasce ADMIN: MASTER nao e atribuivel. */
    @Transactional
    public UsuarioResponse criar(UsuarioCriacaoRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new RecursoDuplicadoException(
                    "Ja existe um usuario cadastrado com o e-mail " + request.email());
        }

        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nome(request.nome())
                .email(request.email())
                .senha(passwordEncoder.encode(request.senha()))
                .role(RoleUsuario.ADMIN)
                .ativo(true)
                .build());

        return UsuarioResponse.fromEntity(usuario);
    }

    public PaginaResponse<UsuarioResponse> listar(String nome, Boolean ativo, Pageable pageable) {
        Page<Usuario> pagina = usuarioRepository.findAll(filtrar(nome, ativo), pageable);
        return PaginaResponse.de(pagina, UsuarioResponse::fromEntity);
    }

    public UsuarioResponse buscarPorId(Long id) {
        return UsuarioResponse.fromEntity(buscarEntidade(id));
    }

    public UsuarioResponse buscarAutenticado() {
        return UsuarioResponse.fromEntity(usuarioAutenticado());
    }

    @Transactional
    public UsuarioResponse atualizar(Long id, UsuarioAtualizacaoRequest request) {
        return UsuarioResponse.fromEntity(aplicarAtualizacao(buscarEntidade(id), request));
    }

    /** Rota do proprio usuario: o id vem do token, nunca do path. */
    @Transactional
    public UsuarioResponse atualizarAutenticado(UsuarioAtualizacaoRequest request) {
        return UsuarioResponse.fromEntity(aplicarAtualizacao(usuarioAutenticado(), request));
    }

    /**
     * Idempotente. Nao ha exclusao definitiva de usuario: desativar preserva o
     * registro de quem operou o sistema, como em cliente e servico.
     */
    @Transactional
    public UsuarioResponse alterarSituacao(Long id, boolean ativo) {
        Usuario usuario = buscarEntidade(id);

        // O MASTER e unico: desativa-lo trancaria o cadastro de usuarios para sempre.
        if (!ativo && RoleUsuario.MASTER.equals(usuario.getRole())) {
            throw new RegraDeNegocioException("O usuario MASTER nao pode ser desativado.");
        }

        usuario.setAtivo(ativo);
        return UsuarioResponse.fromEntity(usuarioRepository.save(usuario));
    }

    private Usuario aplicarAtualizacao(Usuario usuario, UsuarioAtualizacaoRequest request) {
        if (usuarioRepository.existsByEmailAndIdNot(request.email(), usuario.getId())) {
            throw new RecursoDuplicadoException(
                    "Ja existe um usuario cadastrado com o e-mail " + request.email());
        }

        usuario.setNome(request.nome());
        usuario.setEmail(request.email());

        // Senha ausente significa "manter a atual", e nao "apagar".
        if (request.senha() != null && !request.senha().isBlank()) {
            usuario.setSenha(passwordEncoder.encode(request.senha()));
        }

        return usuarioRepository.save(usuario);
    }

    private Usuario usuarioAutenticado() {
        return buscarEntidade(autenticacaoAtual.usuario().getId());
    }

    private Usuario buscarEntidade(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Usuario", id));
    }

    private Specification<Usuario> filtrar(String nome, Boolean ativo) {
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
