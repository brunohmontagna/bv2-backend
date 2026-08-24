package dev.brunohm.bv2_projeto_software_uepg.exception;

/** Recurso inexistente. Traduzida em HTTP 404 pelo GlobalExceptionHandler. */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }

    public static RecursoNaoEncontradoException de(String recurso, Object id) {
        return new RecursoNaoEncontradoException(recurso + " nao encontrado(a) para o id " + id);
    }
}
