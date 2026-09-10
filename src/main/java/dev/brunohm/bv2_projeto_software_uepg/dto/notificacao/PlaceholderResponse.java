package dev.brunohm.bv2_projeto_software_uepg.dto.notificacao;

import dev.brunohm.bv2_projeto_software_uepg.service.RenderizadorMensagem.Placeholder;

/**
 * Alimenta os chips clicaveis do modal de template. Existe para o front nao
 * manter uma copia da lista de placeholders que sairia de sincronia com o
 * RenderizadorMensagem no primeiro placeholder novo.
 */
public record PlaceholderResponse(
        String chave,
        String marcador,
        String descricao,
        String exemplo) {

    public static PlaceholderResponse fromPlaceholder(Placeholder placeholder) {
        return new PlaceholderResponse(
                placeholder.getChave(),
                placeholder.getMarcador(),
                placeholder.getDescricao(),
                placeholder.getExemplo());
    }
}
