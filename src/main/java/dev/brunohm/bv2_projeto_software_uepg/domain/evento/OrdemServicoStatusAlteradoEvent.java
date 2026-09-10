package dev.brunohm.bv2_projeto_software_uepg.domain.evento;

import dev.brunohm.bv2_projeto_software_uepg.domain.enums.StatusOs;

/**
 * Publicado pelo OrdemServicoService quando uma OS muda de status, e consumido
 * depois do commit para disparar a notificacao.
 *
 * <p>
 * Carrega <b>apenas id e enums</b>, nunca a entidade. O consumidor roda em
 * AFTER_COMMIT, quando a OrdemServico ja esta destacada da sessao: com
 * open-in-view=false, tocar getCliente() num objeto assim estoura
 * LazyInitializationException. O listener recarrega pelo repositorio, que tem
 * o @EntityGraph do cliente.
 */
public record OrdemServicoStatusAlteradoEvent(
        Long ordemServicoId,
        StatusOs statusAnterior,
        StatusOs statusNovo) {
}
