package com.itsm.event;

import com.itsm.entity.Incident;
import com.itsm.entity.User;

/**
 * Observer pattern: IncidentService and the SLA monitor publish these events;
 * any number of listeners (notifications today, webhooks or analytics tomorrow)
 * react without the publisher knowing they exist.
 *
 * @param actor  who caused the event (null for system events such as SLA breaches)
 * @param detail free-text context, e.g. the question asked or the old priority
 */
public record IncidentEvent(IncidentEventType type, Incident incident, User actor, String detail) {

    public static IncidentEvent of(IncidentEventType type, Incident incident, User actor) {
        return new IncidentEvent(type, incident, actor, null);
    }
}
