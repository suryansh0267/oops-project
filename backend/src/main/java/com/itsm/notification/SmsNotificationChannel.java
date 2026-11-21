package com.itsm.notification;

import com.itsm.entity.User;
import com.itsm.enums.NotificationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Simulated SMS gateway, used only for urgent events (P1 incidents, SLA breaches, escalations).
 * Swap the body of deliver() for a Twilio/MSG91 call to make it real.
 */
@Slf4j
@Component
public class SmsNotificationChannel implements NotificationChannel {

    private static final int SMS_LIMIT = 160;

    @Override
    public NotificationType type() {
        return NotificationType.SMS;
    }

    @Override
    public boolean canReach(User recipient) {
        return recipient.getPhoneNumber() != null && !recipient.getPhoneNumber().isBlank();
    }

    @Override
    public void deliver(User recipient, String title, String message) {
        String text = title + ": " + message;
        if (text.length() > SMS_LIMIT) {
            text = text.substring(0, SMS_LIMIT - 1) + "…";
        }
        log.info("[SMS] to={} text=\"{}\"", recipient.getPhoneNumber(), text);
    }
}
