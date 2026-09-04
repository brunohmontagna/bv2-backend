package dev.brunohm.bv2_projeto_software_uepg.integration;

import java.net.http.HttpClient;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Unica chamada HTTP de saida do projeto: entrega a notificacao ao webhook do
 * n8n, que a repassa ao WhatsApp.
 *
 * <p>
 * <b>Nunca lanca.</b> Devolve true/false porque a notificacao e um efeito
 * colateral do fluxo da OS: a troca de status ja foi commitada quando este metodo
 * roda, e uma indisponibilidade do n8n nao pode virar erro para quem chamou a API.
 * A falha aparece como notificacao FALHOU no GET /notificacoes.
 */
@Component
public class N8nWebhookClient {

    private static final Logger log = LoggerFactory.getLogger(N8nWebhookClient.class);

    /** Casa com a credencial Header Auth do node Webhook do n8n. */
    private static final String HEADER_TOKEN = "X-BV2-Token";

    private final RestClient restClient;
    private final String url;
    private final String token;

    public N8nWebhookClient(
            @Value("${n8n.webhook.url:}") String url,
            @Value("${n8n.webhook.token:}") String token,
            @Value("${n8n.webhook.timeout-segundos:10}") long timeoutSegundos) {

        this.url = url;
        this.token = token;

        /*
         * Timeouts explicitos: sem eles a thread do listener fica pendurada
         * indefinidamente se o n8n aceitar a conexao e nao responder.
         */
        Duration timeout = Duration.ofSeconds(timeoutSegundos);
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(timeout).build());
        factory.setReadTimeout(timeout);

        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    public boolean enviar(NotificacaoPayload payload) {
        /*
         * URL em branco desliga a integracao. Existe para a aplicacao subir e
         * funcionar em maquina sem n8n configurado — a notificacao fica FALHOU em
         * vez de a transicao de status quebrar.
         */
        if (url == null || url.isBlank()) {
            log.warn("n8n.webhook.url nao configurada: notificacao {} nao foi enviada.",
                    payload.notificacaoId());
            return false;
        }

        try {
            RestClient.RequestBodySpec requisicao = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON);

            if (token != null && !token.isBlank()) {
                requisicao = requisicao.header(HEADER_TOKEN, token);
            }

            requisicao.body(payload).retrieve().toBodilessEntity();
            return true;

        } catch (Exception e) {
            // Exception e nao RestClientException: timeout, DNS e erro de
            // serializacao chegam aqui como tipos diferentes, e nenhum deles pode
            // escapar para o listener.
            log.warn("Falha ao enviar a notificacao {} para o n8n: {}",
                    payload.notificacaoId(), e.toString());
            return false;
        }
    }
}
