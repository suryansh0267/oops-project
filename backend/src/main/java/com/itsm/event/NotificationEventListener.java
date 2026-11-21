package com.itsm.event;

import com.itsm.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Concrete observer that turns incident events into notifications. */
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @EventListener
    public void onIncidentEvent(IncidentEvent event) {
        notificationService.handle(event);
    }
}
