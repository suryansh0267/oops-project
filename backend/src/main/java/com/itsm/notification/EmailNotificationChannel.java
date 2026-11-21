package com.itsm.notification;

import com.itsm.entity.User;
import com.itsm.enums.NotificationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Enterprise e-mail gateway connecting to real SMTP provider (e.g. Gmail).
 * Messages land in the database outbox and are dispatched through JavaMailSender.
 */
@Slf4j
@Component
public class EmailNotificationChannel implements NotificationChannel {

    private final ObjectProvider<JavaMailSender> sender;
    @Value("${app.mail.enabled:true}") private boolean enabled;
    @Value("${app.mail.from:itsmprojectzero@gmail.com}") private String from;

    public EmailNotificationChannel(ObjectProvider<JavaMailSender> sender) {
        this.sender = sender;
    }

    @Override
    public NotificationType type() {
        return NotificationType.EMAIL;
    }

    @Override
    public boolean canReach(User recipient) {
        return recipient.getEmail() != null && !recipient.getEmail().isBlank();
    }

    @Override
    public void deliver(User recipient, String title, String message) {
        if (!enabled) {
            log.info("[EMAIL DISABLED] app.mail.enabled is false. Skipping email delivery for: {}", title);
            return;
        }

        String recipientEmail = recipient.getEmail();
        if (recipientEmail == null || recipientEmail.isBlank()) {
            return;
        }

        // If dummy local domain (e.g. admin@itsm.local), fallback to the system mailbox so emails can actually be delivered!
        if (recipientEmail.endsWith(".local")) {
            log.warn("[EMAIL REDIRECT] Recipient '{}' has simulated domain '{}'. Redirecting delivery to '{}'",
                    recipient.getUsername(), recipientEmail, from);
            recipientEmail = from;
        }

        try {
            JavaMailSender mailer = sender.getIfAvailable();
            if (mailer == null) {
                log.warn("[EMAIL FAILED] JavaMailSender bean is not available in Spring context");
                return;
            }

            SimpleMailMessage email = new SimpleMailMessage();
            email.setFrom(from);
            email.setTo(recipientEmail);
            email.setSubject("[I-ITSM] " + title);
            email.setText(message + "\n\n--\nIntelligent IT Service Desk System");

            log.info("[EMAIL SENDING] Sending email to {} - Subject: \"{}\"", recipientEmail, title);
            mailer.send(email);
            log.info("[EMAIL DELIVERED] Successfully dispatched email to {}", recipientEmail);
        } catch (Exception ex) {
            log.error("[EMAIL ERROR] SMTP delivery failed to {}: {}", recipientEmail, ex.getMessage(), ex);
        }
    }
}
