package com.itsm.service;

import com.itsm.dto.admin.CategoryRequest;
import com.itsm.entity.IncidentCategoryConfig;
import com.itsm.exception.BadRequestException;
import com.itsm.exception.DuplicateResourceException;
import com.itsm.exception.ResourceNotFoundException;
import com.itsm.intelligence.CategoryClassifier;
import com.itsm.repository.IncidentCategoryConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Admin-managed incident categories. Incidents store the category name, so adding a
 * category here is enough for it to be used in reports, filters, skills matching and
 * the automatic categoriser.
 */
@Service
@RequiredArgsConstructor
public class IncidentCategoryService {

    private final IncidentCategoryConfigRepository categoryRepository;

    public List<IncidentCategoryConfig> getAllCategories() {
        return categoryRepository.findAll();
    }

    public List<IncidentCategoryConfig> getActiveCategories() {
        return categoryRepository.findByActiveTrue();
    }

    @Transactional
    public IncidentCategoryConfig createCategory(CategoryRequest request) {
        String name = normalise(request.name());
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Category already exists: " + name);
        }
        IncidentCategoryConfig config = IncidentCategoryConfig.builder()
                .name(name)
                .description(request.description())
                .keywords(cleanKeywords(request.keywords()))
                .active(true)
                .build();
        return categoryRepository.save(config);
    }

    /** Name is fixed once created (incidents refer to it); description and keywords can change. */
    @Transactional
    public IncidentCategoryConfig updateCategory(Long id, CategoryRequest request) {
        IncidentCategoryConfig config = find(id);
        if (!config.getName().equalsIgnoreCase(normalise(request.name()))) {
            throw new BadRequestException("A category cannot be renamed once incidents may use it. Create a new one instead.");
        }
        config.setDescription(request.description());
        if (request.keywords() != null) {
            config.setKeywords(cleanKeywords(request.keywords()));
        }
        return categoryRepository.save(config);
    }

    @Transactional
    public void setActive(Long id, boolean active) {
        IncidentCategoryConfig config = find(id);
        config.setActive(active);
        categoryRepository.save(config);
    }

    /** Returns the canonical name, or throws if it is unknown or switched off. */
    public String requireActive(String name) {
        String normalised = normalise(name);
        return categoryRepository.findByNameIgnoreCase(normalised)
                .filter(IncidentCategoryConfig::isActive)
                .map(IncidentCategoryConfig::getName)
                .orElseThrow(() -> new BadRequestException("Unknown or inactive category: " + normalised));
    }

    public CategoryClassifier.Suggestion suggest(String title, String description) {
        String text = (title == null ? "" : title) + " " + (description == null ? "" : description);
        CategoryClassifier.Suggestion s = CategoryClassifier.classify(text, keywordMap());
        boolean fallbackActive = categoryRepository.findByNameIgnoreCase(s.category())
                .map(IncidentCategoryConfig::isActive).orElse(false);
        return fallbackActive ? s : new CategoryClassifier.Suggestion(
                getActiveCategories().stream().findFirst().map(IncidentCategoryConfig::getName)
                        .orElse(CategoryClassifier.FALLBACK), 0.0, List.of());
    }

    /** Active categories mapped to their keywords (configured ones, else built-in defaults). */
    public Map<String, List<String>> keywordMap() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        for (IncidentCategoryConfig c : getActiveCategories()) {
            List<String> keywords = c.getKeywords() != null && !c.getKeywords().isEmpty()
                    ? c.getKeywords()
                    : CategoryClassifier.DEFAULT_KEYWORDS.getOrDefault(c.getName(), List.of());
            map.put(c.getName(), keywords);
        }
        return map;
    }

    private IncidentCategoryConfig find(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("IncidentCategory", "id", id));
    }

    private static String normalise(String name) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("Category name is required");
        }
        return name.trim().toUpperCase(Locale.ROOT).replaceAll("[\\s-]+", "_");
    }

    private static List<String> cleanKeywords(List<String> keywords) {
        List<String> out = new ArrayList<>();
        if (keywords == null) return out;
        for (String k : keywords) {
            if (k == null) continue;
            String t = k.trim().toLowerCase(Locale.ROOT);
            if (!t.isEmpty() && !out.contains(t)) out.add(t);
        }
        return out;
    }
}
