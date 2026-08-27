package com.booking.system.dto.request;

import com.booking.system.enums.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResourceRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 150, message = "Name must be at most 150 characters")
        String name,

        @Size(max = 1000, message = "Description must be at most 1000 characters")
        String description,

        @NotNull(message = "Type is required")
        ResourceType type,

        @NotBlank(message = "Location is required")
        String location,

        @NotNull(message = "Availability flag is required")
        Boolean available
) {
}
