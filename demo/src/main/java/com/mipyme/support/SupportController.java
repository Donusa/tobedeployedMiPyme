package com.mipyme.support;

import com.mipyme.email.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/support")
public class SupportController {

    private final EmailService emailService;

    @Value("${spring.mail.username:noreply@mipyme.com}")
    private String supportEmail;

    public SupportController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/contact")
    public ResponseEntity<?> sendSupportMessage(@RequestBody SupportRequest request) {
        try {
            String subject = "Nuevo Mensaje de Soporte: " + (request.getSubject() != null ? request.getSubject() : "Sin asunto");
            String messageContent = request.getMessage() != null ? request.getMessage().replace("\n", "<br>") : "Sin mensaje";

            String body = String.format("""
                <h3>Nuevo mensaje de contacto desde la plataforma</h3>
                <p><strong>De:</strong> %s (%s)</p>
                <p><strong>Asunto:</strong> %s</p>
                <hr/>
                <p><strong>Mensaje:</strong></p>
                <p>%s</p>
                """,
                request.getName() != null ? request.getName() : "Anónimo",
                request.getEmail() != null ? request.getEmail() : "Sin email",
                subject,
                messageContent);


            emailService.sendHtmlEmail(supportEmail, subject, body);

            return ResponseEntity.ok(java.util.Map.of("message", "Mensaje enviado correctamente"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(java.util.Map.of("message", "Error interno al enviar el mensaje: " + e.getMessage()));
        }
    }

    public static class SupportRequest {
        private String name;
        private String email;
        private String subject;
        private String message;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
