package dev.brunohm.bv2_projeto_software_uepg.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import dev.brunohm.bv2_projeto_software_uepg.domain.entity.Notificacao;
import dev.brunohm.bv2_projeto_software_uepg.domain.entity.OrdemServico;
import dev.brunohm.bv2_projeto_software_uepg.domain.entity.TemplateNotificacao;
import dev.brunohm.bv2_projeto_software_uepg.domain.enums.StatusNotificacao;
import dev.brunohm.bv2_projeto_software_uepg.domain.enums.StatusOs;
import dev.brunohm.bv2_projeto_software_uepg.domain.evento.OrdemServicoStatusAlteradoEvent;
import dev.brunohm.bv2_projeto_software_uepg.dto.PaginaResponse;
import dev.brunohm.bv2_projeto_software_uepg.dto.notificacao.NotificacaoResponse;
import dev.brunohm.bv2_projeto_software_uepg.dto.notificacao.PlaceholderResponse;
import dev.brunohm.bv2_projeto_software_uepg.dto.notificacao.TemplateNotificacaoRequest;
import dev.brunohm.bv2_projeto_software_uepg.dto.notificacao.TemplateNotificacaoResponse;
import dev.brunohm.bv2_projeto_software_uepg.exception.RecursoNaoEncontradoException;
import dev.brunohm.bv2_projeto_software_uepg.exception.RegraDeNegocioException;
import dev.brunohm.bv2_projeto_software_uepg.integration.N8nWebhookClient;
import dev.brunohm.bv2_projeto_software_uepg.integration.NotificacaoPayload;
import dev.brunohm.bv2_projeto_software_uepg.repository.NotificacaoRepository;
import dev.brunohm.bv2_projeto_software_uepg.repository.OrdemServicoRepository;
import dev.brunohm.bv2_projeto_software_uepg.repository.TemplateNotificacaoRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

/**
 * Notificacoes tem dois lados que nao se confundem: o TemplateNotificacao e
 * configuracao editavel pela M2, e a Notificacao e o log do que foi enviado.
 * A API le o log e edita a configuracao; quem escreve o log e o listener da
 * transicao de status.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificacaoService {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoService.class);

    /** Limite da coluna notificacoes.conteudo (V18). */
    private static final int CONTEUDO_MAX = 1000;

    private final NotificacaoRepository notificacaoRepository;
    private final TemplateNotificacaoRepository templateNotificacaoRepository;
    private final OrdemServicoRepository ordemServicoRepository;
    private final RenderizadorMensagem renderizadorMensagem;
    private final N8nWebhookClient n8nWebhookClient;

    // ------------------------------------------------------------------
    // Log de envios (somente leitura pela API)
    // ------------------------------------------------------------------

    public PaginaResponse<NotificacaoResponse> listar(Long ordemServicoId, Long clienteId,
            StatusOs statusOs, StatusNotificacao status, Pageable pageable) {

        Page<Notificacao> pagina = notificacaoRepository.findAll(
                filtrar(ordemServicoId, clienteId, statusOs, status), pageable);

        return PaginaResponse.de(pagina, NotificacaoResponse::fromEntity);
    }

    public NotificacaoResponse buscarPorId(Long id) {
        return NotificacaoResponse.fromEntity(buscarEntidade(id));
    }

    private Notificacao buscarEntidade(Long id) {
        return notificacaoRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Notificacao", id));
    }

    private Specification<Notificacao> filtrar(Long ordemServicoId, Long clienteId,
            StatusOs statusOs, StatusNotificacao status) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();

            if (ordemServicoId != null) {
                predicados.add(cb.equal(root.get("ordemServico").get("id"), ordemServicoId));
            }
            if (clienteId != null) {
                predicados.add(cb.equal(root.get("cliente").get("id"), clienteId));
            }
            if (statusOs != null) {
                predicados.add(cb.equal(root.get("statusOs"), statusOs));
            }
            if (status != null) {
                predicados.add(cb.equal(root.get("status"), status));
            }

            return cb.and(predicados.toArray(new Predicate[0]));
        };
    }

    // ------------------------------------------------------------------
    // Templates (configuracao)
    // ------------------------------------------------------------------

    /**
     * Ordenado pelo ordinal do StatusOs para o modal do front nao trocar a ordem
     * das abas a cada requisicao — findAll numa PK de enum nao garante ordem.
     */
    public List<TemplateNotificacaoResponse> listarTemplates() {
        return templateNotificacaoRepository.findAll().stream()
                .sorted(Comparator.comparing(TemplateNotificacao::getStatus))
                .map(TemplateNotificacaoResponse::fromEntity)
                .toList();
    }

    public List<PlaceholderResponse> listarPlaceholders() {
        return renderizadorMensagem.placeholders().stream()
                .map(PlaceholderResponse::fromPlaceholder)
                .toList();
    }

    /**
     * Nao existe criacao: o conjunto de status notificaveis e semeado na migration
     * V18. Pedir um status fora dele (EM_ANDAMENTO, hoje) e 404 — pela rota
     * informada o recurso nao existe mesmo.
     */
    @Transactional
    public TemplateNotificacaoResponse atualizarTemplate(StatusOs status, TemplateNotificacaoRequest request) {
        TemplateNotificacao template = templateNotificacaoRepository.findById(status)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Nao ha notificacao configuravel para o status " + status + "."));

        Set<String> desconhecidas = renderizadorMensagem.chavesDesconhecidas(request.conteudo());
        if (!desconhecidas.isEmpty()) {
            throw new RegraDeNegocioException(
                    "Placeholder desconhecido no conteudo: " + String.join(", ", desconhecidas)
                            + ". Aceitos: " + renderizadorMensagem.chavesAceitas() + ".");
        }

        template.setConteudo(request.conteudo());
        template.setAtivo(request.ativo());

        /*
         * saveAndFlush e nao save: @UpdateTimestamp so preenche atualizadoEm no
         * flush, que sem isto acontece depois do fromEntity — a resposta sairia com
         * o timestamp anterior e o modal mostraria "atualizado em" defasado logo
         * apos salvar.
         */
        return TemplateNotificacaoResponse.fromEntity(templateNotificacaoRepository.saveAndFlush(template));
    }

    // ------------------------------------------------------------------
    // Disparo automatico
    // ------------------------------------------------------------------

    /**
     * Chamado pelo NotificacaoOsListener depois do commit da transicao de status.
     *
     * <p>
     * REQUIRES_NEW e obrigatorio: em AFTER_COMMIT nao ha mais transacao ativa, e
     * sem uma nova o save abaixo e descartado em silencio — o n8n recebe a chamada
     * e o GET /notificacoes volta vazio.
     *
     * <p>
     * Nao ha try/catch aqui de proposito. O caminho comum de falha (n8n fora do ar)
     * nao lanca: o client devolve false e a linha e gravada como FALHOU. Uma falha
     * catastrofica deve mesmo desfazer esta transacao, e quem a engole para nao
     * contaminar a requisicao original e o listener.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processarTransicao(OrdemServicoStatusAlteradoEvent evento) {
        TemplateNotificacao template = templateNotificacaoRepository.findById(evento.statusNovo())
                .orElse(null);

        /*
         * Status sem template configurado ou com a notificacao desligada nao gera
         * linha nenhuma: o log registra tentativa de envio, nao evento ignorado.
         * E tambem o que cobre EM_ANDAMENTO sem precisar de if especial.
         */
        if (template == null || Boolean.FALSE.equals(template.getAtivo())) {
            return;
        }

        OrdemServico ordemServico = ordemServicoRepository.findById(evento.ordemServicoId())
                .orElse(null);
        if (ordemServico == null) {
            log.warn("OS {} nao encontrada ao notificar a transicao para {}.",
                    evento.ordemServicoId(), evento.statusNovo());
            return;
        }

        String mensagem = truncar(renderizadorMensagem.renderizar(
                template.getConteudo(), ordemServico, evento.statusNovo()));

        Notificacao notificacao = notificacaoRepository.save(Notificacao.builder()
                .ordemServico(ordemServico)
                .cliente(ordemServico.getCliente())
                .statusOs(evento.statusNovo())
                .conteudo(mensagem)
                .status(StatusNotificacao.PENDENTE)
                .tentativas(0)
                .build());

        boolean enviado = n8nWebhookClient.enviar(NotificacaoPayload.de(
                notificacao.getId(), mensagem, ordemServico,
                evento.statusAnterior(), evento.statusNovo()));

        notificacao.setTentativas(notificacao.getTentativas() + 1);
        if (enviado) {
            notificacao.setStatus(StatusNotificacao.ENVIADO);
            notificacao.setDataEnvio(LocalDateTime.now());
        } else {
            notificacao.setStatus(StatusNotificacao.FALHOU);
        }

        notificacaoRepository.save(notificacao);
    }

    /*
     * O template cabe em 500, mas a substituicao dos placeholders cresce o texto.
     * A folga da coluna (1000) cobre o pior caso real; o corte existe para que um
     * cadastro extremo vire mensagem truncada e nao erro de insert.
     */
    private String truncar(String mensagem) {
        return mensagem.length() <= CONTEUDO_MAX ? mensagem : mensagem.substring(0, CONTEUDO_MAX);
    }
}
