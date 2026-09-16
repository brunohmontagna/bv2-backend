package dev.brunohm.bv2_projeto_software_uepg.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import lombok.extern.slf4j.Slf4j;

/**
 * Traduz excecoes da aplicacao em respostas RFC 7807 (ProblemDetail).
 * Vale para toda a API, nao so para /clientes.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail tratarValidacao(MethodArgumentNotValidException ex) {
        Map<String, String> erros = new LinkedHashMap<>();
        for (FieldError erro : ex.getBindingResult().getFieldErrors()) {
            erros.merge(erro.getField(), erro.getDefaultMessage(), (a, b) -> a + "; " + b);
        }
        ProblemDetail problema = montar(HttpStatus.BAD_REQUEST,
                "Dados inválidos", "Um ou mais campos falharam na validação.");
        problema.setProperty("erros", erros);
        return problema;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail tratarCorpoIlegivel(HttpMessageNotReadableException ex) {
        return montar(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido",
                "O JSON enviado está malformado ou tem tipos incompatíveis.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail tratarTipoInvalido(MethodArgumentTypeMismatchException ex) {
        return montar(HttpStatus.BAD_REQUEST, "Parâmetro inválido",
                "O valor '" + ex.getValue() + "' não é válido para o parâmetro '" + ex.getName() + "'.");
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ProblemDetail tratarNaoEncontrado(RecursoNaoEncontradoException ex) {
        return montar(HttpStatus.NOT_FOUND, "Recurso não encontrado", ex.getMessage());
    }

    @ExceptionHandler(RecursoDuplicadoException.class)
    public ProblemDetail tratarDuplicado(RecursoDuplicadoException ex) {
        return montar(HttpStatus.CONFLICT, "Recurso duplicado", ex.getMessage());
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ProblemDetail tratarRegraDeNegocio(RegraDeNegocioException ex) {
        ProblemDetail problema = montar(HttpStatus.UNPROCESSABLE_ENTITY,
                "Regra de negócio violada", ex.getMessage());
        if (ex.getCampo() != null) {
            problema.setProperty("erros", Map.of(ex.getCampo(), ex.getMessage()));
        }
        return problema;
    }

    /**
     * Cobre UNIQUE e as FKs ON DELETE RESTRICT do schema (um cliente com
     * equipamentos/ordens vinculados nao pode ser removido).
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail tratarIntegridade(DataIntegrityViolationException ex) {
        log.warn("Violação de integridade no banco", ex);
        return montar(HttpStatus.CONFLICT, "Conflito de integridade",
                "A operação viola uma restrição do banco de dados. "
                        + "Verifique se o registro já existe ou se possui vínculos que impedem a exclusão.");
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail tratarCredenciaisInvalidas(BadCredentialsException ex) {
        return montar(HttpStatus.UNAUTHORIZED, "Credenciais inválidas",
                "E-mail ou senha incorretos.");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail tratarNaoAutenticado(AuthenticationException ex) {
        return montar(HttpStatus.UNAUTHORIZED, "Não autenticado", "Autenticação necessária.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail tratarAcessoNegado(AccessDeniedException ex) {
        return montar(HttpStatus.FORBIDDEN, "Acesso negado",
                "Você não tem permissão para acessar este recurso.");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail tratarErroInesperado(Exception ex) {
        log.error("Erro não tratado", ex);
        return montar(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                "Ocorreu um erro inesperado. Contate o administrador.");
    }

    private ProblemDetail montar(HttpStatus status, String titulo, String detalhe) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        problema.setProperty("timestamp", Instant.now());
        return problema;
    }
}
