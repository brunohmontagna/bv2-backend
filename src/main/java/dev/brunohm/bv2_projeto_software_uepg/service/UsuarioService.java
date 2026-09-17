package dev.brunohm.bv2_projeto_software_uepg.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
import dev.brunohm.bv2_projeto_software_uepg.dto.usuario.AlteracaoSenhaRequest;
import dev.brunohm.bv2_projeto_software_uepg.dto.usuario.UsuarioAtualizacaoRequest;
import dev.brunohm.bv2_projeto_software_uepg.dto.usuario.UsuarioCriacaoRequest;
import dev.brunohm.bv2_projeto_software_uepg.dto.usuario.UsuarioResponse;
import dev.brunohm.bv2_projeto_software_uepg.exception.RecursoDuplicadoException;
import dev.brunohm.bv2_projeto_software_uepg.exception.RecursoNaoEncontradoException;
import dev.brunohm.bv2_projeto_software_uepg.exception.RegraDeNegocioException;
import dev.brunohm.bv2_projeto_software_uepg.repository.ClienteRepository;
import dev.brunohm.bv2_projeto_software_uepg.repository.EquipamentoRepository;
import dev.brunohm.bv2_projeto_software_uepg.repository.ItemOsRepository;
import dev.brunohm.bv2_projeto_software_uepg.repository.NotificacaoRepository;
import dev.brunohm.bv2_projeto_software_uepg.repository.OrdemServicoRepository;
import dev.brunohm.bv2_projeto_software_uepg.repository.ServicoRepository;
import dev.brunohm.bv2_projeto_software_uepg.repository.TemplateNotificacaoRepository;
import dev.brunohm.bv2_projeto_software_uepg.repository.TokenVerificacaoRepository;
import dev.brunohm.bv2_projeto_software_uepg.repository.UsuarioRepository;
import dev.brunohm.bv2_projeto_software_uepg.security.AutenticacaoAtual;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

/**
 * Cadastro dos usuarios do sistema. E o unico recurso com restricao de papel: so
 * o MASTER enxerga a lista. O ADMIN chega aqui apenas pelo "eu". Cada usuario e
 * dono de uma conta, entao criar e excluir usuario tambem cria e apaga a conta.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final TemplateNotificacaoRepository templateNotificacaoRepository;
    private final NotificacaoRepository notificacaoRepository;
    private final ItemOsRepository itemOsRepository;
    private final OrdemServicoRepository ordemServicoRepository;
    private final EquipamentoRepository equipamentoRepository;
    private final ClienteRepository clienteRepository;
    private final ServicoRepository servicoRepository;
    private final TokenVerificacaoRepository tokenVerificacaoRepository;
    private final PasswordEncoder passwordEncoder;
    private final AutenticacaoAtual autenticacaoAtual;

    /**
     * Todo usuario criado pela API nasce ADMIN: MASTER nao e atribuivel. A conta ja
     * nasce com os templates de notificacao (desligados), porque o modal do front e
     * o disparo automatico contam com as tres linhas existindo.
     */
    @Transactional
    public UsuarioResponse criar(UsuarioCriacaoRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new RecursoDuplicadoException(
                    "Já existe um usuário cadastrado com o e-mail " + request.email());
        }

        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nome(request.nome())
                .email(request.email())
                .senha(passwordEncoder.encode(request.senha()))
                .role(RoleUsuario.ADMIN)
                .ativo(true)
                .build());

        templateNotificacaoRepository.saveAll(TemplatesNotificacaoPadrao.para(usuario.getId()));

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
            throw new RegraDeNegocioException("O usuário MASTER não pode ser desativado.");
        }

        usuario.setAtivo(ativo);
        return UsuarioResponse.fromEntity(usuarioRepository.save(usuario));
    }

    /**
     * Exclusao definitiva do usuario <b>e de toda a conta dele</b>: clientes,
     * equipamentos, servicos, OS, itens, notificacoes, templates e tokens. Nao ha
     * volta; para tirar o acesso preservando o historico, o caminho e desativar.
     *
     * <p>
     * As FKs sao todas ON DELETE RESTRICT de proposito — nada some por engano em
     * outro lugar do sistema. Por isso a limpeza e explicita e na ordem das
     * dependencias (folhas primeiro), em deletes em massa: carregar a conta inteira
     * em memoria para apagar linha a linha nao acrescentaria nada. Tudo na mesma
     * transacao: se qualquer passo falhar, a conta fica inteira.
     *
     * <p>
     * Os tokens JWT ja emitidos para o usuario morrem sozinhos: o JwtAuthFilter nao
     * encontra mais o e-mail e nao autentica.
     */
    @Transactional
    public void excluir(Long id) {
        Usuario usuario = buscarEntidade(id);

        // O MASTER e unico: exclui-lo trancaria o cadastro de usuarios para sempre.
        if (RoleUsuario.MASTER.equals(usuario.getRole())) {
            throw new RegraDeNegocioException("O usuário MASTER não pode ser excluído.");
        }

        notificacaoRepository.excluirDaConta(id);
        itemOsRepository.excluirDaConta(id);
        ordemServicoRepository.excluirDaConta(id);
        equipamentoRepository.excluirDaConta(id);
        clienteRepository.excluirDaConta(id);
        servicoRepository.excluirDaConta(id);
        templateNotificacaoRepository.excluirDaConta(id);
        tokenVerificacaoRepository.excluirDoUsuario(id);

        usuarioRepository.delete(usuario);
        usuarioRepository.flush();
    }

    /*
     * So o nome. Senha e e-mail ja estiveram aqui e sairam pelo mesmo motivo: mudavam
     * sem prova de posse, e os dois sao credenciais — o e-mail e o login. Cada um tem
     * agora seu fluxo verificado (PUT /usuarios/eu/senha e PUT /usuarios/eu/email).
     */
    private Usuario aplicarAtualizacao(Usuario usuario, UsuarioAtualizacaoRequest request) {
        usuario.setNome(request.nome());
        return usuarioRepository.save(usuario);
    }

    /**
     * Troca a senha do proprio usuario, exigindo a atual como prova de posse.
     *
     * <p>
     * Este e o <b>unico</b> caminho autenticado para trocar a senha: o PUT deixou de
     * aceitar o campo justamente porque nao pedia a senha antiga — um token roubado
     * bastava para tomar a conta.
     *
     * <p>
     * Os tres 422 sao deliberados. Senha atual errada nao pode ser 401: o token e
     * valido e o usuario <i>esta</i> autenticado, e interceptadores de front costumam
     * deslogar em qualquer 401 — um erro de digitacao expulsaria o usuario da sessao.
     */
    @Transactional
    public void alterarSenhaAutenticado(AlteracaoSenhaRequest request) {
        Usuario usuario = usuarioAutenticado();

        if (!passwordEncoder.matches(request.senhaAtual(), usuario.getSenha())) {
            throw new RegraDeNegocioException("A senha atual está incorreta.");
        }

        if (!request.senhaNova().equals(request.senhaNovaConfirmacao())) {
            throw new RegraDeNegocioException("A nova senha e a confirmação não conferem.");
        }

        if (passwordEncoder.matches(request.senhaNova(), usuario.getSenha())) {
            throw new RegraDeNegocioException("A nova senha deve ser diferente da atual.");
        }

        usuario.setSenha(passwordEncoder.encode(request.senhaNova()));
        // Truncado a segundos para casar com o iat do JWT. Ver Usuario.senhaAlteradaEm.
        usuario.setSenhaAlteradaEm(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        usuarioRepository.save(usuario);
    }

    private Usuario usuarioAutenticado() {
        return buscarEntidade(autenticacaoAtual.usuario().getId());
    }

    private Usuario buscarEntidade(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Usuário", id));
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
