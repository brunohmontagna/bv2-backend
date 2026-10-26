package dev.brunohm.bv2_projeto_software_uepg.service;

import java.text.Normalizer;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

/**
 * Busca livre das listagens ("contem"), sem diferenciar maiusculas nem acentos:
 * "joao" acha "João". E o comportamento que as telas tinham quando filtravam no
 * navegador, e a banca cobra justamente achar o cliente digitando parte do nome.
 *
 * <p>
 * translate() e nao a extensao unaccent: unaccent exige CREATE EXTENSION, que
 * depende de permissao de superusuario no banco de producao. O translate cobre os
 * acentos do portugues e e funcao nativa do PostgreSQL.
 */
final class BuscaTexto {

    private static final String COM_ACENTO = "áàâãäéèêëíìîïóòôõöúùûüçñ";
    private static final String SEM_ACENTO = "aaaaaeeeeiiiiooooouuuucn";

    private BuscaTexto() {
    }

    /** {@code campo} contem {@code termo}, ignorando caixa e acento dos dois lados. */
    static Predicate contem(CriteriaBuilder cb, Expression<String> campo, String termo) {
        return cb.like(normalizar(cb, campo), "%" + normalizar(termo) + "%");
    }

    private static Expression<String> normalizar(CriteriaBuilder cb, Expression<String> campo) {
        return cb.function("translate", String.class,
                cb.lower(campo), cb.literal(COM_ACENTO), cb.literal(SEM_ACENTO));
    }

    /* Do lado Java, NFD separa a letra do acento e o regex descarta o acento. */
    private static String normalizar(String termo) {
        return Normalizer.normalize(termo.trim().toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }
}
