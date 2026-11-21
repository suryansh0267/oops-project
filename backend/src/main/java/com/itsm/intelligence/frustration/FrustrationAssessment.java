package com.itsm.intelligence.frustration;

import java.util.List;

public record FrustrationAssessment(int score, FrustrationLevel level, List<String> reasons, String trend) {
    public FrustrationAssessment { reasons = List.copyOf(reasons); }
}
