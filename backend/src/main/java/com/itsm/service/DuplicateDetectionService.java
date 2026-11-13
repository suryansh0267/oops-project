package com.itsm.service;

import com.itsm.entity.Incident;
import com.itsm.enums.IncidentStatus;
import com.itsm.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DuplicateDetectionService {

    private final IncidentRepository incidentRepository;

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "in", "on", "at", "to", "for", "with", "by", "about",
            "is", "are", "was", "were", "and", "or", "not", "of", "it", "my", "this", "that"
    );

    public List<Incident> findPotentialDuplicates(Incident newIncident) {
        if (newIncident == null || newIncident.getTitle() == null) {
            return Collections.emptyList();
        }

        // Search open incidents
        List<Incident> activeIncidents = incidentRepository.findAll().stream()
                .filter(i -> !i.getId().equals(newIncident.getId()) &&
                        i.getStatus() != IncidentStatus.CLOSED &&
                        i.getStatus() != IncidentStatus.RESOLVED)
                .toList();

        List<Incident> duplicates = new ArrayList<>();
        for (Incident existing : activeIncidents) {
            // Check same service or category bonus
            boolean sameCategory = existing.getCategory() == newIncident.getCategory();
            boolean sameService = existing.getAffectedService() != null &&
                    newIncident.getAffectedService() != null &&
                    existing.getAffectedService().getId().equals(newIncident.getAffectedService().getId());

            double titleSimilarity = calculateSimilarity(newIncident.getTitle(), existing.getTitle());
            double descSimilarity = calculateSimilarity(newIncident.getDescription(), existing.getDescription());

            double totalScore = (titleSimilarity * 0.6) + (descSimilarity * 0.4);
            if (sameService) totalScore += 0.15;
            if (sameCategory) totalScore += 0.10;

            if (totalScore >= 0.45) {
                duplicates.add(existing);
            }
        }

        return duplicates.stream().limit(5).toList();
    }

    public double calculateSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null || text1.isBlank() || text2.isBlank()) {
            return 0.0;
        }

        Set<String> words1 = tokenize(text1);
        Set<String> words2 = tokenize(text2);

        if (words1.isEmpty() || words2.isEmpty()) {
            return 0.0;
        }

        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);

        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);

        return (double) intersection.size() / union.size();
    }

    private Set<String> tokenize(String text) {
        return Arrays.stream(text.toLowerCase().split("\\W+"))
                .filter(w -> !w.isBlank() && !STOP_WORDS.contains(w))
                .collect(Collectors.toSet());
    }
}
