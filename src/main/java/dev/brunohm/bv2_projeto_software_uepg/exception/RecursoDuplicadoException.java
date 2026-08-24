package dev.brunohm.bv2_projeto_software_uepg.exception;

/** Violacao de unicidade detectada pela aplicacao. Traduzida em HTTP 409. */
public class RecursoDuplicadoException extends RuntimeException {

    public RecursoDuplicadoException(String mensagem) {
        super(mensagem);
    }
}
