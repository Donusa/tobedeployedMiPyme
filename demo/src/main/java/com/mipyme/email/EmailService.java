package com.mipyme.email;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@mipyme.com}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendEmail(String to, String subject, String body) {

        sendHtmlEmail(to, subject, body.replace("\n", "<br>"));
    }

    @Async
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Error enviando email a: " + to);
            e.printStackTrace();
        }
    }

    public void sendTwoFactorCode(String to, String code) {
        String subject = "Código de Verificación - MiPyme";
        String htmlBody = """
                <div style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; margin: 0 auto; padding: 0; border: 1px solid #e0e0e0; border-radius: 8px; background-color: #ffffff; overflow: hidden;">
                    <!-- Header -->
                    <div style="background-color: #2563eb; padding: 20px; text-align: center;">
                        <h1 style="color: #ffffff; margin: 0; font-size: 24px; font-weight: 600;">MiPyme</h1>
                        <p style="color: #e0e0e0; font-size: 14px; margin: 5px 0 0 0;">Gestión Inteligente</p>
                    </div>

                    <!-- Content -->
                    <div style="padding: 30px 20px;">
                        <h2 style="color: #333333; margin-top: 0; font-size: 20px;">Verificación de Seguridad</h2>
                        <p style="color: #555555; font-size: 16px; line-height: 1.5;">Hola,</p>
                        <p style="color: #555555; font-size: 16px; line-height: 1.5;">Hemos recibido una solicitud de inicio de sesión para su cuenta. Utilice el siguiente código para completar el proceso:</p>

                        <div style="background-color: #f8f9fa; border: 1px dashed #2563eb; padding: 20px; text-align: center; margin: 25px 0; border-radius: 6px;">
                            <span style="font-size: 36px; font-weight: 700; letter-spacing: 8px; color: #2563eb; display: block;">%s</span>
                            <span style="font-size: 12px; color: #6c757d; display: block; margin-top: 10px;">Válido por 10 minutos</span>
                        </div>
                    </div>

                    <!-- Security Warning -->
                    <div style="background-color: #fff3cd; padding: 15px 20px; border-top: 1px solid #ffeeba; border-bottom: 1px solid #ffeeba;">
                        <p style="color: #856404; font-size: 14px; margin: 0; font-weight: 600;">⚠ Aviso de Seguridad Importante</p>
                        <p style="color: #856404; font-size: 13px; margin: 5px 0 0 0; line-height: 1.4;">
                            Si usted <strong>NO</strong> solicitó este código, es posible que un tercero esté intentando acceder a su cuenta. Le recomendamos <a href="#" style="color: #856404; text-decoration: underline;">cambiar su contraseña inmediatamente</a> para proteger su información.
                        </p>
                    </div>

                    <!-- Footer -->
                    <div style="background-color: #f8f9fa; padding: 20px; text-align: center; border-top: 1px solid #e0e0e0;">
                        <p style="color: #999999; font-size: 12px; margin: 0;">&copy; 2026 MiPyme System. Todos los derechos reservados.</p>
                        <p style="color: #999999; font-size: 12px; margin: 5px 0 0 0;">Este es un mensaje automático, por favor no responda a este correo.</p>
                    </div>
                </div>
                """
                .formatted(code);

        sendHtmlEmail(to, subject, htmlBody);
    }

    @Async
    public void sendPasswordRecoveryCode(String to, String code) {
        String subject = "Recuperación de Contraseña - MiPyme";
        String htmlBody = """
                <div style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 10px rgba(0,0,0,0.05);">
                    <!-- Header -->
                    <div style="background-color: #2563eb; padding: 20px; text-align: center;">
                        <h1 style="color: white; margin: 0; font-size: 24px; font-weight: 600;">MiPyme</h1>
                    </div>

                    <!-- Content -->
                    <div style="padding: 40px 30px; background-color: #ffffff;">
                        <h2 style="color: #333333; margin-top: 0; text-align: center; font-size: 22px;">Recuperación de Contraseña</h2>

                        <p style="color: #555555; font-size: 16px; line-height: 1.5; margin-bottom: 30px; text-align: center;">
                            Has solicitado restablecer tu contraseña. Utiliza el siguiente código para continuar con el proceso.
                        </p>

                        <!-- Code Box -->
                        <div style="background-color: #f8f9fa; border: 1px dashed #ced4da; border-radius: 6px; padding: 20px; text-align: center; margin: 0 auto 30px auto; width: fit-content; min-width: 200px;">
                            <span style="font-size: 36px; font-weight: 700; letter-spacing: 8px; color: #2563eb; display: block;">%s</span>
                            <span style="font-size: 12px; color: #6c757d; display: block; margin-top: 10px;">Válido por 15 minutos</span>
                        </div>
                    </div>

                    <!-- Security Warning -->
                    <div style="background-color: #fff3cd; padding: 15px 20px; border-top: 1px solid #ffeeba; border-bottom: 1px solid #ffeeba;">
                        <p style="color: #856404; font-size: 14px; margin: 0; font-weight: 600;">⚠ Aviso de Seguridad</p>
                        <p style="color: #856404; font-size: 13px; margin: 5px 0 0 0; line-height: 1.4;">
                            Si usted <strong>NO</strong> solicitó este cambio, ignore este correo. Su contraseña actual seguirá siendo válida.
                        </p>
                    </div>

                    <!-- Footer -->
                    <div style="background-color: #f8f9fa; padding: 20px; text-align: center; border-top: 1px solid #e0e0e0;">
                        <p style="color: #999999; font-size: 12px; margin: 0;">&copy; 2026 MiPyme System. Todos los derechos reservados.</p>
                    </div>
                </div>
                """
                .formatted(code);

        sendHtmlEmail(to, subject, htmlBody);
    }
}
