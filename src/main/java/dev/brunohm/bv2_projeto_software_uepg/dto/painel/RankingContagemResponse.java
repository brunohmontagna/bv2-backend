package dev.brunohm.bv2_projeto_software_uepg.dto.painel;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Linha de ranking por contagem. Serve servicos e marcas: os dois sao
 * (id, nome, quantidade), e um record por recurso nao acrescentaria nada.
 *
 * <p>
 * O que "quantidade" conta muda conforme o ranking, e a diferenca e proposital:
 * em servicos e o numero de execucoes (o mesmo servico entra duas vezes na OS se
 * for para equipamentos diferentes); em marcas e o numero de OS atendidas.
 *
 * <p>
 * E montado direto pelo JPQL (select new), com id e nome escalares — nunca com a
 * entidade gerenciada, que quebraria na serializacao com open-in-view=false.
 */
@Schema(description = "Item de ranking por contagem")
public record RankingContagemResponse(Long id, String nome, Long quantidade) {
}
