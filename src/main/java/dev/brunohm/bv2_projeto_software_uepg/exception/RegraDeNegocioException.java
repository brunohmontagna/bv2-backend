package dev.brunohm.bv2_projeto_software_uepg.exception;

/** Requisicao sintaticamente valida que viola uma regra de negocio. HTTP 422. */
public class RegraDeNegocioException extends RuntimeException {

    /**
     * Campo do corpo a que a regra se refere, ou nulo quando ela e do pedido como um
     * todo. Preenchido, a resposta ganha o mesmo mapa {@code erros} da validacao, e o
     * front mostra a mensagem embaixo do campo em vez de num alerta geral.
     */
    private final String campo;

    public RegraDeNegocioException(String mensagem) {
        this(null, mensagem);
    }

    public RegraDeNegocioException(String campo, String mensagem) {
        super(mensagem);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
