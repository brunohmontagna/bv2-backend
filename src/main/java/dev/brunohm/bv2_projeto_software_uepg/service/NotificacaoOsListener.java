package dev.brunohm.bv2_projeto_software_uepg.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import dev.brunohm.bv2_projeto_software_uepg.domain.evento.OrdemServicoStatusAlteradoEvent;
import lombok.RequiredArgsConstructor;

/**
 * Liga a transicao de status da OS ao envio da notificacao.
 *
 * <p>
 * AFTER_COMMIT e nao um metodo chamado direto no OrdemServicoService: a
 * notificacao e efeito colateral, e o cliente nao pode receber um WhatsApp de
 * "servico concluido" se a transacao que concluiu a OS acabar revertida.
 *
 * <p>
 * Classe separada do NotificacaoService de proposito. Chamada interna nao passa
 * pelo proxy do Spring, entao o REQUIRES_NEW de processarTransicao seria ignorado
 * se o listener morasse la dentro.
 */
@Component
@RequiredArgsConstructor
public class NotificacaoOsListener {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoOsListener.class);

    private final NotificacaoService notificacaoService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoAlterarStatus(OrdemServicoStatusAlteradoEvent evento) {
        try {
            notificacaoService.processarTransicao(evento);
        } catch (Exception e) {
            /*
             * Ultima barreira. A OS ja esta commitada quando isto roda, entao nada
             * aqui pode escapar e virar erro na resposta de quem trocou o status.
             */
            log.error("Falha ao notificar a transicao da OS {} para {}: {}",
                    evento.ordemServicoId(), evento.statusNovo(), e.toString(), e);
        }
    }
}
