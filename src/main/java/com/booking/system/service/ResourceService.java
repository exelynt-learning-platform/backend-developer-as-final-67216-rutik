package com.booking.system.service;

import com.booking.system.dto.request.ResourceRequest;
import com.booking.system.dto.response.PagedResponse;
import com.booking.system.dto.response.ResourceResponse;
import com.booking.system.entity.Resource;
import com.booking.system.exception.ResourceNotFoundException;
import com.booking.system.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceRepository resourceRepository;

    @Transactional(readOnly = true)
    public PagedResponse<ResourceResponse> getAllResources(Pageable pageable) {
        Page<ResourceResponse> page = resourceRepository.findAll(pageable)
                .map(ResourceResponse::from);
        return PagedResponse.from(page);
    }

    @Transactional(readOnly = true)
    public ResourceResponse getResourceById(Long id) {
        return ResourceResponse.from(findResourceOrThrow(id));
    }

    @Transactional
    public ResourceResponse createResource(ResourceRequest request) {
        Resource resource = Resource.builder()
                .name(request.name())
                .description(request.description())
                .type(request.type())
                .location(request.location())
                .available(request.available())
                .build();

        return ResourceResponse.from(resourceRepository.save(resource));
    }

    @Transactional
    public ResourceResponse updateResource(Long id, ResourceRequest request) {
        Resource resource = findResourceOrThrow(id);

        resource.setName(request.name());
        resource.setDescription(request.description());
        resource.setType(request.type());
        resource.setLocation(request.location());
        resource.setAvailable(request.available());

        return ResourceResponse.from(resourceRepository.save(resource));
    }

    @Transactional
    public void deleteResource(Long id) {
        Resource resource = findResourceOrThrow(id);
        resourceRepository.delete(resource);
    }

    private Resource findResourceOrThrow(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + id));
    }
}
