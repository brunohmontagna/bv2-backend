package dev.brunohm.bv2_projeto_software_uepg.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import dev.brunohm.bv2_projeto_software_uepg.exception.RecursoNaoEncontradoException;
import dev.brunohm.bv2_projeto_software_uepg.exception.RegraDeNegocioException;
import dev.brunohm.bv2_projeto_software_uepg.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

/**
 * Conta dona dos dados da requisicao em curso. Cada usuario tem a propria base
 * (clientes, servicos, templates e tudo que pende deles); e por aqui que os
 * services descobrem em qual base ler e gravar.
 *
 * <p>
 * O ADMIN opera sempre a propria conta. O MASTER escolhe a conta pelo header
 * {@value #HEADER}; sem ele, opera a propria. Header e nao query param para nao
 * mudar a assinatura de nenhum endpoint.
 */
@Component
@RequiredArgsConstructor
public class ContaAtual {

    public static final String HEADER = "X-Conta-Id";

    /* Os services chamam id() varias vezes por requisicao; a checagem roda uma vez. */
    private static final String ATRIBUTO = ContaAtual.class.getName();

    private final AutenticacaoAtual autenticacaoAtual;
    private final UsuarioRepository usuarioRepository;

    public Long id() {
        RequestAttributes atributos = RequestContextHolder.getRequestAttributes();
        if (atributos != null && atributos.getAttribute(ATRIBUTO, RequestAttributes.SCOPE_REQUEST) instanceof Long id) {
            return id;
        }

        Long id = resolver(atributos);
        if (atributos != null) {
            atributos.setAttribute(ATRIBUTO, id, RequestAttributes.SCOPE_REQUEST);
        }
        return id;
    }

    private Long resolver(RequestAttributes atributos) {
        UsuarioAutenticado usuario = autenticacaoAtual.usuario();
        Long pedido = lerHeader(atributos);

        if (pedido == null || pedido.equals(usuario.getId())) {
            return usuario.getId();
        }

        /*
         * 403 explicito, e nao ignorar o header em silencio: um ADMIN pedindo outra
         * conta e tentativa de acesso, e o front nunca manda o header para ele.
         */
        if (!usuario.isMaster()) {
            throw new AccessDeniedException("Apenas o MASTER pode operar a conta de outro usuário.");
        }

        if (!usuarioRepository.existsById(pedido)) {
            throw RecursoNaoEncontradoException.de("Usuário", pedido);
        }
        return pedido;
    }

    private static Long lerHeader(RequestAttributes atributos) {
        if (!(atributos instanceof ServletRequestAttributes servlet)) {
            return null;
        }
        String valor = servlet.getRequest().getHeader(HEADER);
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(valor.strip());
        } catch (NumberFormatException e) {
            throw new RegraDeNegocioException("O cabeçalho " + HEADER + " deve ser o id numérico de um usuário.");
        }
    }
}
