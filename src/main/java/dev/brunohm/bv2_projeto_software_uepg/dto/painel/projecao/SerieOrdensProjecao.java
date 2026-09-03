package dev.brunohm.bv2_projeto_software_uepg.dto.painel.projecao;

/**
 * Um mes da serie de OS abertas. Ano e mes vem separados porque o agrupamento usa
 * extract(year/month), que e JPQL padrao — date_trunc devolveria um timestamp e
 * amarraria a query ao Postgres.
 */
public record SerieOrdensProjecao(Integer ano, Integer mes, Long quantidade) {
}
