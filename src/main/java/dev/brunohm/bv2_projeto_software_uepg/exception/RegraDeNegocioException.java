package dev.brunohm.bv2_projeto_software_uepg.exception;

/** Requisicao sintaticamente valida que viola uma regra de negocio. HTTP 422. */
public class RegraDeNegocioException extends RuntimeException {

    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}
