package com.itsm.notification;

import com.itsm.entity.User;
import com.itsm.enums.NotificationType;
import org.springframework.stereotype.Component;

@Component
public class InAppNotificationChannel implements NotificationChannel {

    @Override
    public NotificationType type() {
        return NotificationType.IN_APP;
    }

    @Override
    public void deliver(User recipient, String title, String message) {
        // The persisted Notification row is what the bell icon reads; nothing else to do.
    }
}
