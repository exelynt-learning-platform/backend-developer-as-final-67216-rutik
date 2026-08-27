package com.booking.system.dto.response;

import com.booking.system.entity.Resource;
import com.booking.system.enums.ResourceType;

import java.time.Instant;

public record ResourceResponse(
        Long id,
        String name,
        String description,
        ResourceType type,
        String location,
        boolean available,
        Instant createdAt,
        Instant updatedAt
) {
    public static ResourceResponse from(Resource resource) {
        return new ResourceResponse(
                resource.getId(),
                resource.getName(),
                resource.getDescription(),
                resource.getType(),
                resource.getLocation(),
                resource.isAvailable(),
                resource.getCreatedAt(),
                resource.getUpdatedAt()
        );
    }
}
