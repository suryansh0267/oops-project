package com.itsm.intelligence.frustration;

import com.itsm.entity.User;
import com.itsm.entity.Incident;

/** Pattern: Observer. A rising flag is independent of channel delivery. */
public record FrustrationDetectedEvent(User user, Incident incident, FrustrationAssessment assessment) { }
