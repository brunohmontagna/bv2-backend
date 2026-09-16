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
 * Envio de e-mail transacional: o link de recuperacao de senha e o de
 * confirmacao de troca de e-mail.
 *
 * <p>
 * <b>Nunca lanca</b>, pelo mesmo motivo do N8nWebhookClient: o endpoint que chama
 * responde 202 independentemente do resultado, para nao revelar se o e-mail existe.
 * Uma falha de SMTP nao pode virar erro HTTP nem, pior, virar um sinal de que a
 * conta existe.
 *
 * <p>
 * <b>O token nunca aparece no log</b> — nem ele, nem o link montado. Log costuma ir
 * para arquivo e ser lido por gente que nao deveria poder trocar a senha nem o
 * e-mail de ninguem.
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
     * Envia o link de redefinicao de senha ao endereco atual do usuario.
     */
    public void enviarRecuperacaoSenha(String destinatario, String nome, String link,
            long expiracaoMinutos) {
        enviar(destinatario, "Redefinição de senha - BV2",
                corpoRecuperacaoSenha(nome, link, expiracaoMinutos), "recuperação de senha");
    }

    /**
     * Envia o link de confirmacao ao endereco <b>novo</b>. E para la que vai porque
     * so o clique no destino prova que o endereco existe e pertence a quem pediu —
     * um typo vira link que nunca chega, e nao conta trancada.
     */
    public void enviarConfirmacaoEmail(String destinatario, String nome, String link,
            long expiracaoMinutos) {
        enviar(destinatario, "Confirme seu novo e-mail - BV2",
                corpoConfirmacaoEmail(nome, link, expiracaoMinutos), "confirmação de e-mail");
    }

    /**
     * Devolve void: nao ha nada que o chamador possa fazer com a falha, e propaga-la
     * mudaria a resposta do endpoint.
     */
    private void enviar(String destinatario, String assunto, String html, String contexto) {

        /*
         * Usuario SMTP em branco desliga o envio. Existe para a aplicacao subir e
         * funcionar em maquina sem SMTP configurado — o fluxo continua gravando o
         * token, so nao entrega o e-mail.
         */
        if (usuarioSmtp == null || usuarioSmtp.isBlank()) {
            log.warn("spring.mail.username não configurado: e-mail de {} não enviado.", contexto);
            return;
        }

        try {
            MimeMessage mensagem = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mensagem, false, StandardCharsets.UTF_8.name());

            helper.setFrom(remetente, remetenteNome);
            helper.setTo(destinatario);
            helper.setSubject(assunto);
            helper.setText(html, true);

            mailSender.send(mensagem);
            log.info("E-mail de {} enviado para {}.", contexto, destinatario);

        } catch (Exception e) {
            // Exception e nao MailException: falha de encoding e de montagem do
            // MimeMessage sobem como tipos diferentes, e nenhuma pode escapar daqui.
            log.warn("Falha ao enviar o e-mail de {} para {}: {}",
                    contexto, destinatario, e.toString());
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
    private String corpoRecuperacaoSenha(String nome, String link, long expiracaoMinutos) {
        return """
                <div style="font-family: Arial, Helvetica, sans-serif; font-size: 15px; color: #1a1a1a; line-height: 1.6;">
                  <p>Olá, %s!</p>

                  <p>Recebemos um pedido para redefinir a senha da sua conta no BV2.
                     Para escolher uma nova senha, acesse o endereço abaixo:</p>

                  <p><a href="%s" style="color: #1d4ed8;">%s</a></p>

                  <p>O link vale por %d minutos e pode ser usado uma única vez.</p>

                  <p>Se você não pediu a redefinição, ignore este e-mail: sua senha
                     atual continua valendo e nada muda.</p>

                  <p style="margin-top: 24px; color: #666; font-size: 13px;">
                    Atenciosamente, BV2
                  </p>
                </div>
                """.formatted(nome, link, link, expiracaoMinutos);
    }

    /*
     * Vai para um endereco que ainda nao foi verificado, entao quem le pode nao ser
     * o dono da conta: se alguem digitou o e-mail errado, o destinatario e um
     * estranho. Por isso o texto deixa explicito que ignorar nao muda nada.
     */
    private String corpoConfirmacaoEmail(String nome, String link, long expiracaoMinutos) {
        return """
                <div style="font-family: Arial, Helvetica, sans-serif; font-size: 15px; color: #1a1a1a; line-height: 1.6;">
                  <p>Olá, %s!</p>

                  <p>Recebemos um pedido para que este endereço passe a ser o e-mail de acesso
                     da sua conta no BV2. Para confirmar a alteração, acesse o endereço abaixo:</p>

                  <p><a href="%s" style="color: #1d4ed8;">%s</a></p>

                  <p>O link vale por %d minutos e pode ser usado uma única vez. Depois da
                     confirmação, você passará a entrar com este e-mail.</p>

                  <p>Se você não pediu esta alteração, ignore este e-mail: nenhuma conta
                     será alterada.</p>

                  <p style="margin-top: 24px; color: #666; font-size: 13px;">
                    Atenciosamente, BV2
                  </p>
                </div>
                """.formatted(nome, link, link, expiracaoMinutos);
    }
}
