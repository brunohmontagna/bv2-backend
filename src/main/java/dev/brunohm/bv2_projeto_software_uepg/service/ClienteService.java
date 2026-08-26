package dev.brunohm.bv2_projeto_software_uepg.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.brunohm.bv2_projeto_software_uepg.domain.entity.Cliente;
import dev.brunohm.bv2_projeto_software_uepg.domain.entity.Usuario;
import dev.brunohm.bv2_projeto_software_uepg.domain.enums.RoleUsuario;
import dev.brunohm.bv2_projeto_software_uepg.dto.PaginaResponse;
import dev.brunohm.bv2_projeto_software_uepg.dto.cliente.ClienteAtualizacaoRequest;
import dev.brunohm.bv2_projeto_software_uepg.dto.cliente.ClienteCriacaoRequest;
import dev.brunohm.bv2_projeto_software_uepg.dto.cliente.ClienteResponse;
import dev.brunohm.bv2_projeto_software_uepg.dto.cliente.UsuarioCriacaoRequest;
import dev.brunohm.bv2_projeto_software_uepg.exception.RecursoDuplicadoException;
import dev.brunohm.bv2_projeto_software_uepg.exception.RecursoNaoEncontradoException;
import dev.brunohm.bv2_projeto_software_uepg.repository.ClienteRepository;
import dev.brunohm.bv2_projeto_software_uepg.repository.UsuarioRepository;
import dev.brunohm.bv2_projeto_software_uepg.security.UsuarioAutenticado;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Cria Usuario e Cliente na mesma transacao: a FK id_usuario e NOT NULL,
     * entao nao existe cliente sem usuario.
     */
    @Transactional
    public ClienteResponse criar(ClienteCriacaoRequest request) {
        UsuarioCriacaoRequest dadosUsuario = request.usuario();

        if (usuarioRepository.existsByEmail(dadosUsuario.email())) {
            throw new RecursoDuplicadoException(
                    "Ja existe um usuario cadastrado com o e-mail " + dadosUsuario.email());
        }

        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nome(dadosUsuario.nome())
                .email(dadosUsuario.email())
                .senha(passwordEncoder.encode(dadosUsuario.senha()))
                .role(RoleUsuario.CLIENTE)
                .build());

        Cliente cliente = clienteRepository.save(Cliente.builder()
                .usuario(usuario)
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
        Cliente cliente = buscarEntidade(id);
        garantirAcesso(cliente);
        return ClienteResponse.fromEntity(cliente);
    }

    @Transactional
    public ClienteResponse atualizar(Long id, ClienteAtualizacaoRequest request) {
        Cliente cliente = buscarEntidade(id);
        garantirAcesso(cliente);

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
     * Remocao definitiva do cliente e do usuario associado. Todas as FKs do
     * schema sao ON DELETE RESTRICT: havendo equipamentos, ordens ou
     * notificacoes vinculadas, o flush lanca DataIntegrityViolationException,
     * traduzida em 409. Para o caso comum use desativar().
     */
    @Transactional
    public void excluir(Long id) {
        Cliente cliente = buscarEntidade(id);
        Usuario usuario = cliente.getUsuario();

        clienteRepository.delete(cliente);
        clienteRepository.flush();

        usuarioRepository.delete(usuario);
        usuarioRepository.flush();
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

    /** ADMIN acessa qualquer cliente; um CLIENTE so acessa o proprio cadastro. */
    private void garantirAcesso(Cliente cliente) {
        UsuarioAutenticado autenticado = autenticado();
        if (autenticado.isAdmin()) {
            return;
        }
        if (!autenticado.getId().equals(cliente.getUsuario().getId())) {
            throw new AccessDeniedException("Voce so pode acessar o proprio cadastro.");
        }
    }

    private UsuarioAutenticado autenticado() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || !(autenticacao.getPrincipal() instanceof UsuarioAutenticado usuario)) {
            throw new AccessDeniedException("Requisicao sem usuario autenticado.");
        }
        return usuario;
    }
}
