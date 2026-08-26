package dev.brunohm.bv2_projeto_software_uepg.dto;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * Envelope de paginacao proprio da API. Existe para nao serializar o Page do
 * Spring Data direto, cuja estrutura JSON nao e estavel entre versoes.
 * Reutilizavel por todos os recursos.
 */
public record PaginaResponse<T>(
        List<T> conteudo,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas,
        boolean primeira,
        boolean ultima) {

    public static <E, T> PaginaResponse<T> de(Page<E> page, Function<E, T> mapeador) {
        return new PaginaResponse<>(
                page.getContent().stream().map(mapeador).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }
}
