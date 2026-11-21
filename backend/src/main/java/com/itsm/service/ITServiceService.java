package com.itsm.service;

import com.itsm.dto.ITServiceRequest;
import com.itsm.entity.ITService;
import com.itsm.exception.DuplicateResourceException;
import com.itsm.exception.ResourceNotFoundException;
import com.itsm.repository.ITServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ITServiceService {

    private final ITServiceRepository itServiceRepository;

    @Transactional
    public ITService createService(ITServiceRequest request) {
        if (itServiceRepository.findByName(request.getName()).isPresent()) {
            throw new DuplicateResourceException("IT Service already exists with name: " + request.getName());
        }

        ITService service = ITService.builder()
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory())
                .businessCriticality(request.getBusinessCriticality())
                .supportContact(request.getSupportContact())
                .active(true)
                .build();

        return itServiceRepository.save(service);
    }

    @Transactional
    public ITService updateService(Long id, ITServiceRequest request) {
        ITService service = itServiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("IT service", "id", id));
        itServiceRepository.findByName(request.getName())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new DuplicateResourceException("Another service is already called " + request.getName());
                });

        service.setName(request.getName());
        service.setDescription(request.getDescription());
        service.setCategory(request.getCategory());
        service.setBusinessCriticality(request.getBusinessCriticality());
        service.setSupportContact(request.getSupportContact());

        return itServiceRepository.save(service);
    }

    public List<ITService> getAllServices() {
        return itServiceRepository.findAll();
    }

    public List<ITService> getActiveServices() {
        return itServiceRepository.findByActiveTrue();
    }

    public ITService getServiceById(Long id) {
        return itServiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ITService", "id", id));
    }

    @Transactional
    public void setActive(Long id, boolean active) {
        ITService service = itServiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("IT service", "id", id));
        service.setActive(active);
        itServiceRepository.save(service);
    }
}
