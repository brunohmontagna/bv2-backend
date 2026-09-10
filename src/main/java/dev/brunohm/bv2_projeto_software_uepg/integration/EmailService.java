package dev.brunohm.bv2_projeto_software_uepg.integration;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Envio de e-mail transacional. Hoje so o link de recuperacao de senha.
 *
 * <p>
 * <b>Nunca lanca</b>, pelo mesmo motivo do N8nWebhookClient: o endpoint que chama
 * responde 202 independentemente do resultado, para nao revelar se o e-mail existe.
 * Uma falha de SMTP nao pode virar erro HTTP nem, pior, virar um sinal de que a
 * conta existe.
 *
 * <p>
 * <b>O token nunca aparece no log</b> — nem ele, nem o link montado. Log costuma ir
 * para arquivo e ser lido por gente que nao deveria poder trocar a senha de ninguem.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String usuarioSmtp;

    @Value("${app.mail.remetente:}")
    private String remetente;

    @Value("${app.mail.remetente-nome:}")
    private String remetenteNome;

    /**
     * Envia o link de redefinicao. Devolve void: nao ha nada que o chamador possa
     * fazer com a falha, e propaga-la mudaria a resposta do endpoint.
     */
    public void enviarRecuperacaoSenha(String destinatario, String nome, String link,
            long expiracaoMinutos) {

        /*
         * Usuario SMTP em branco desliga o envio. Existe para a aplicacao subir e
         * funcionar em maquina sem SMTP configurado — o fluxo continua gravando o
         * token, so nao entrega o e-mail.
         */
        if (usuarioSmtp == null || usuarioSmtp.isBlank()) {
            log.warn("spring.mail.username nao configurado: e-mail de recuperacao nao enviado.");
            return;
        }

        try {
            MimeMessage mensagem = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mensagem, false, StandardCharsets.UTF_8.name());

            helper.setFrom(remetente, remetenteNome);
            helper.setTo(destinatario);
            helper.setSubject("Redefinicao de senha - BV2");
            helper.setText(corpo(nome, link, expiracaoMinutos), true);

            mailSender.send(mensagem);
            log.info("E-mail de recuperacao enviado para {}.", destinatario);

        } catch (Exception e) {
            // Exception e nao MailException: falha de encoding e de montagem do
            // MimeMessage sobem como tipos diferentes, e nenhuma pode escapar daqui.
            log.warn("Falha ao enviar o e-mail de recuperacao para {}: {}",
                    destinatario, e.toString());
        }
    }

    /*
     * HTML montado a mao, sem Thymeleaf: e um e-mail so, e a dependencia nao se paga.
     *
     * Duas decisoes contra a caixa de spam e contra a cara de phishing: o link
     * aparece tambem como texto legivel (quem desconfia consegue conferir o dominio
     * antes de clicar) e o e-mail diz explicitamente o que fazer se o pedido nao
     * partiu do destinatario.
     */
    private String corpo(String nome, String link, long expiracaoMinutos) {
        return """
                <div style="font-family: Arial, Helvetica, sans-serif; font-size: 15px; color: #1a1a1a; line-height: 1.6;">
                  <p>Olá, %s!</p>

                  <p>Recebemos um pedido para redefinir a senha da sua conta no BV2.
                     Para escolher uma nova senha, acesse o endereco abaixo:</p>

                  <p><a href="%s" style="color: #1d4ed8;">%s</a></p>

                  <p>O link vale por %d minutos e pode ser usado uma única vez.</p>

                  <p>Se voce não pediu a redefinição, ignore este e-mail: sua senha
                     atual continua valendo e nada muda.</p>

                  <p style="margin-top: 24px; color: #666; font-size: 13px;">
                    Atenciosamente, BV2
                  </p>
                </div>
                """.formatted(nome, link, link, expiracaoMinutos);
    }
}
