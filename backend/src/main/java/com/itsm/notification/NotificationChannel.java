package com.itsm.notification;

import com.itsm.entity.User;
import com.itsm.enums.NotificationType;

/**
 * Strategy/Adapter for a delivery mechanism. Adding WhatsApp or Slack means
 * one new @Component implementing this interface; NotificationService picks it up automatically.
 */
public interface NotificationChannel {

    NotificationType type();

    /** Whether this channel can reach the recipient (e.g. SMS needs a phone number). */
    default boolean canReach(User recipient) {
        return true;
    }

    /**
     * Hand the message to the external system. In-app delivery is just the stored
     * row, so its implementation does nothing extra.
     */
    void deliver(User recipient, String title, String message);
}
