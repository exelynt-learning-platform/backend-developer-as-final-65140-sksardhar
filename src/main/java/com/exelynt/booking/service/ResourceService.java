package com.exelynt.booking.service;

import com.exelynt.booking.dto.PagedResponse;
import com.exelynt.booking.dto.ResourceRequest;
import com.exelynt.booking.dto.ResourceResponse;
import com.exelynt.booking.entity.Resource;
import com.exelynt.booking.exception.ResourceNotFoundException;
import com.exelynt.booking.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceRepository resourceRepository;

    public PagedResponse<ResourceResponse> getAllResources(int page, int size, String sortBy, String sortDir) {
        Pageable pageable = buildPageable(page, size, sortBy, sortDir);
        Page<Resource> resourcePage = resourceRepository.findAll(pageable);
        return toPagedResponse(resourcePage);
    }

    public ResourceResponse getResourceById(Long id) {
        Resource resource = findResourceOrThrow(id);
        return toResponse(resource);
    }

    public ResourceResponse createResource(ResourceRequest request) {
        Resource resource = Resource.builder()
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType())
                .location(request.getLocation())
                .pricePerHour(request.getPricePerHour())
                .available(request.getAvailable() == null || request.getAvailable())
                .build();
        return toResponse(resourceRepository.save(resource));
    }

    public ResourceResponse updateResource(Long id, ResourceRequest request) {
        Resource resource = findResourceOrThrow(id);
        resource.setName(request.getName());
        resource.setDescription(request.getDescription());
        resource.setType(request.getType());
        resource.setLocation(request.getLocation());
        resource.setPricePerHour(request.getPricePerHour());
        if (request.getAvailable() != null) {
            resource.setAvailable(request.getAvailable());
        }
        return toResponse(resourceRepository.save(resource));
    }

    public void deleteResource(Long id) {
        Resource resource = findResourceOrThrow(id);
        resourceRepository.delete(resource);
    }

    Resource findResourceOrThrow(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with id: " + id));
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortDir) {
        String property = (sortBy == null || sortBy.isBlank()) ? "id" : sortBy;
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by(direction, property));
    }

    private PagedResponse<ResourceResponse> toPagedResponse(Page<Resource> resourcePage) {
        return PagedResponse.<ResourceResponse>builder()
                .content(resourcePage.getContent().stream().map(this::toResponse).toList())
                .page(resourcePage.getNumber())
                .size(resourcePage.getSize())
                .totalElements(resourcePage.getTotalElements())
                .totalPages(resourcePage.getTotalPages())
                .last(resourcePage.isLast())
                .build();
    }

    private ResourceResponse toResponse(Resource resource) {
        return ResourceResponse.builder()
                .id(resource.getId())
                .name(resource.getName())
                .description(resource.getDescription())
                .type(resource.getType())
                .location(resource.getLocation())
                .pricePerHour(resource.getPricePerHour())
                .available(resource.isAvailable())
                .createdAt(resource.getCreatedAt())
                .build();
    }
}
