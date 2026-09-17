package dev.brunohm.bv2_projeto_software_uepg.service;

import java.util.List;
import java.util.Map;

import dev.brunohm.bv2_projeto_software_uepg.domain.entity.TemplateNotificacao;
import dev.brunohm.bv2_projeto_software_uepg.domain.enums.StatusOs;

/**
 * Templates com que toda conta nova nasce. Mesmo texto semeado na V18 e copiado
 * para as contas existentes na V22 — se mudar aqui, e so para contas criadas
 * daqui em diante; as antigas continuam com o que ja tem salvo.
 *
 * <p>
 * Nascem desligados: criar um usuario nao pode, por si so, passar a mandar
 * WhatsApp para cliente nenhum. EM_ANDAMENTO fica de fora porque abertura de OS
 * nao notifica.
 */
final class TemplatesNotificacaoPadrao {

    private static final Map<StatusOs, String> CONTEUDOS = Map.of(
            StatusOs.CONCLUIDA,
            "Ola, {cliente}! O servico da ordem #{os} foi concluido e o equipamento ja pode ser retirado. Valor: R$ {valor}. - M2 Equipamentos",
            StatusOs.ENTREGUE,
            "Ola, {cliente}! Confirmamos a entrega do equipamento da ordem #{os}. Obrigado pela preferencia! - M2 Equipamentos",
            StatusOs.CANCELADA,
            "Ola, {cliente}! A ordem de servico #{os} foi cancelada. Qualquer duvida, e so chamar. - M2 Equipamentos");

    private TemplatesNotificacaoPadrao() {
    }

    static List<TemplateNotificacao> para(Long usuarioId) {
        return CONTEUDOS.entrySet().stream()
                .map(entrada -> TemplateNotificacao.builder()
                        .usuarioId(usuarioId)
                        .status(entrada.getKey())
                        .conteudo(entrada.getValue())
                        .ativo(false)
                        .build())
                .toList();
    }
}
