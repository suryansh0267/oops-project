package com.itsm.service;

import com.itsm.entity.IncidentCategoryConfig;
import com.itsm.exception.DuplicateResourceException;
import com.itsm.exception.ResourceNotFoundException;
import com.itsm.repository.IncidentCategoryConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    public IncidentCategoryConfig createCategory(String name, String description) {
        String trimmed = name.trim().toUpperCase();
        if (categoryRepository.existsByNameIgnoreCase(trimmed)) {
            throw new DuplicateResourceException("Category already exists: " + trimmed);
        }

        IncidentCategoryConfig config = IncidentCategoryConfig.builder()
                .name(trimmed)
                .description(description)
                .active(true)
                .build();

        return categoryRepository.save(config);
    }

    @Transactional
    public void deactivateCategory(Long id) {
        IncidentCategoryConfig config = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("IncidentCategory", "id", id));
        config.setActive(false);
        categoryRepository.save(config);
    }
}
