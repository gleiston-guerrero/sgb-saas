package com.uteq.backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Envuelve {@link JavaMailSender} para alertas y verificación de correo.
 * Un fallo de SMTP nunca rompe el flujo que lo origina: devuelve
 * {@code boolean} y el llamador decide (ej. {@code NotificacionService}
 * lo persiste en {@code enviado_ok}/{@code error_envio}).
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${notificaciones.remitente}")
    private String remitente;

    @Value("${brevo.api-key:}")
    private String brevoApiKey;

    @Value("${brevo.api-url:https://api.brevo.com/v3/smtp/email}")
    private String brevoApiUrl;

    /**
     * Constructor con el remitente SMTP para el envío de alertas y verificación.
     *
     * @param mailSender remitente Jakarta Mail con la cuenta configurada
     */
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Envía un correo HTML intentando primero el SMTP clásico y, si falla, la API HTTP de Brevo
     * cuando hay clave configurada. Nunca interrumpe el flujo que lo origina: informa el resultado
     * para que el llamador decida (por ejemplo persistirlo en {@code enviado_ok}).
     *
     * @param destinatario dirección de correo del receptor
     * @param asunto asunto del mensaje a enviar
     * @param bodyHtml cuerpo HTML del mensaje ya maquetado por el llamador
     * @return verdadero si el correo se aceptó por SMTP o por Brevo; falso si ambos fallaron
     */
    public boolean sendEmail(String destinatario, String asunto, String bodyHtml) {
        // 1) Intento SMTP clásico
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(remitente);
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(bodyHtml, true);
            mailSender.send(message);
            return true;
        } catch (MessagingException | MailException ex) {
            log.warn("SMTP falló para {} (asunto: \"{}\"): {} — probando fallback Brevo API", destinatario, asunto, ex.getMessage());
            // 2) Fallback HTTP Brevo API
            if (brevoApiKey != null && !brevoApiKey.isBlank()) {
                boolean ok = sendViaBrevoApi(destinatario, asunto, bodyHtml);
                if (ok) return true;
            }
            log.error("Fallo al enviar correo a {} (asunto: \"{}\") por SMTP y Brevo API", destinatario, asunto);
            return false;
        }
    }

    private boolean sendViaBrevoApi(String destinatario, String asunto, String bodyHtml) {
        try {
            RestClient client = RestClient.builder().baseUrl("https://api.brevo.com").build();
            Map<String, Object> body = Map.of(
                    "sender", Map.of("email", remitente, "name", "SGB-SaaS"),
                    "to", List.of(Map.of("email", destinatario)),
                    "subject", asunto,
                    "htmlContent", bodyHtml
            );
            var resp = client.post()
                    .uri("/v3/smtp/email")
                    .header("api-key", brevoApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toEntity(String.class);
            boolean ok = resp.getStatusCode().is2xxSuccessful();
            if (ok) log.info("Correo enviado vía Brevo API a {} (asunto: \"{}\")", destinatario, asunto);
            else log.warn("Brevo API respondió {} para {}: {}", resp.getStatusCode(), destinatario, resp.getBody());
            return ok;
        } catch (Exception ex) {
            log.warn("Fallback Brevo API falló para {}: {}", destinatario, ex.getMessage());
            return false;
        }
    }
}
