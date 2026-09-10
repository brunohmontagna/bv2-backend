package dev.brunohm.bv2_projeto_software_uepg.service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import dev.brunohm.bv2_projeto_software_uepg.domain.entity.OrdemServico;
import dev.brunohm.bv2_projeto_software_uepg.domain.enums.StatusOs;

/**
 * Resolve os placeholders do template de notificacao com os dados da OS.
 *
 * <p>
 * O conjunto de placeholders e <b>fechado</b> e vive so aqui: e o mesmo que a
 * validacao do PUT usa para recusar chave desconhecida e que o
 * GET /notificacoes/placeholders publica para o front. Acrescentar um placeholder
 * novo e acrescentar uma constante neste enum, e nada mais.
 *
 * <p>
 * Substituicao literal, sem String.format e sem template engine: o texto e
 * escrito por usuario final, e um % ou uma chave solta nao pode derrubar o envio.
 */
@Component
public class RenderizadorMensagem {

    /**
     * Toda constante aqui e uma chave aceita no template. Descricao e exemplo
     * existem para o modal do front montar os chips clicaveis sem duplicar a lista.
     */
    public enum Placeholder {

        CLIENTE("cliente", "Nome do cliente da ordem de servico", "Oficina do Ze"),
        OS("os", "Numero da ordem de servico", "34"),
        VALOR("valor", "Valor total da OS, no formato brasileiro e sem o R$", "1.234,56"),
        STATUS("status", "Status para o qual a OS acabou de mudar", "Concluida"),
        DATA_ENTRADA("dataEntrada", "Data de entrada do equipamento", "20/08/2026"),
        DATA_CONCLUIDA("dataConcluida", "Data de conclusao; vazio se a OS ainda nao foi concluida", "03/09/2026"),
        DATA_ENTREGUE("dataEntregue", "Data de entrega; vazio se a OS ainda nao foi entregue", "05/09/2026");

        private final String chave;
        private final String descricao;
        private final String exemplo;

        Placeholder(String chave, String descricao, String exemplo) {
            this.chave = chave;
            this.descricao = descricao;
            this.exemplo = exemplo;
        }

        public String getChave() {
            return chave;
        }

        public String getDescricao() {
            return descricao;
        }

        public String getExemplo() {
            return exemplo;
        }

        /** Como a chave aparece escrita dentro do template. */
        public String getMarcador() {
            return "{" + chave + "}";
        }
    }

    private static final Pattern MARCADOR = Pattern.compile("\\{(\\w+)\\}");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public List<Placeholder> placeholders() {
        return List.of(Placeholder.values());
    }

    /**
     * Chaves escritas no template que nao correspondem a nenhum placeholder. Se
     * a M2 digitar {nome} em vez de {cliente}, sem essa checagem o cliente
     * receberia "{nome}" literal no WhatsApp — por isso o erro e no PUT, e nao no
     * envio.
     */
    public Set<String> chavesDesconhecidas(String template) {
        Set<String> conhecidas = Arrays.stream(Placeholder.values())
                .map(Placeholder::getChave)
                .collect(java.util.stream.Collectors.toSet());

        Set<String> desconhecidas = new LinkedHashSet<>();
        Matcher matcher = MARCADOR.matcher(template);
        while (matcher.find()) {
            String chave = matcher.group(1);
            if (!conhecidas.contains(chave)) {
                desconhecidas.add(chave);
            }
        }
        return desconhecidas;
    }

    /** Lista pronta para a mensagem de erro do 422. */
    public String chavesAceitas() {
        return Arrays.stream(Placeholder.values())
                .map(Placeholder::getMarcador)
                .collect(java.util.stream.Collectors.joining(", "));
    }

    /**
     * statusNovo vem a parte da OS de proposito: no momento do envio a entidade ja
     * foi salva com o status novo, mas passa-lo explicitamente deixa o metodo
     * independente de quando o listener roda.
     */
    public String renderizar(String template, OrdemServico ordemServico, StatusOs statusNovo) {
        String texto = template;
        for (Placeholder placeholder : Placeholder.values()) {
            texto = texto.replace(placeholder.getMarcador(), valorDe(placeholder, ordemServico, statusNovo));
        }
        return texto;
    }

    private String valorDe(Placeholder placeholder, OrdemServico os, StatusOs statusNovo) {
        return switch (placeholder) {
            case CLIENTE -> os.getCliente().getNome();
            case OS -> String.valueOf(os.getId());
            case VALOR -> formatarValor(os.getValorTotal());
            case STATUS -> rotuloDe(statusNovo);
            case DATA_ENTRADA -> formatarData(os.getDataEntrada());
            case DATA_CONCLUIDA -> formatarData(os.getDataConcluida());
            case DATA_ENTREGUE -> formatarData(os.getDataEntregue());
        };
    }

    /**
     * DecimalFormat nao e thread-safe e este componente e singleton, entao a
     * instancia nasce e morre dentro da chamada. O volume nao justifica ThreadLocal.
     */
    private String formatarValor(BigDecimal valor) {
        if (valor == null) {
            return "";
        }
        DecimalFormatSymbols simbolos = new DecimalFormatSymbols(Locale.of("pt", "BR"));
        return new DecimalFormat("#,##0.00", simbolos).format(valor);
    }

    /** Data ausente vira string vazia, e nao "null" na mensagem do cliente. */
    private String formatarData(LocalDate data) {
        return data == null ? "" : data.format(DATA);
    }

    /*
     * Sem acentos, como todo texto gerado pelo projeto. O que a M2 escrever no
     * template dela e outra historia: a coluna e UTF-8 e aceita acento normalmente.
     */
    private String rotuloDe(StatusOs status) {
        return switch (status) {
            case EM_ANDAMENTO -> "Em andamento";
            case CONCLUIDA -> "Concluida";
            case ENTREGUE -> "Entregue";
            case CANCELADA -> "Cancelada";
        };
    }
}
