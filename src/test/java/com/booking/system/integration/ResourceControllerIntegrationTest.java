package com.booking.system.integration;

import com.booking.system.dto.request.ResourceRequest;
import com.booking.system.entity.Resource;
import com.booking.system.enums.ResourceType;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ResourceControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    void listResources_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/resources"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listResources_accessibleToAuthenticatedUser() throws Exception {
        resourceRepository.save(sampleResource("Room X"));

        mockMvc.perform(get("/api/resources").header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].name").value("Room X"));
    }

    @Test
    void createResource_deniedForRegularUser() throws Exception {
        ResourceRequest request = new ResourceRequest("New Room", "desc", ResourceType.ROOM, "Floor 3", true);

        mockMvc.perform(post("/api/resources")
                        .header("Authorization", bearer(userToken))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createResource_allowedForAdmin() throws Exception {
        ResourceRequest request = new ResourceRequest("New Room", "desc", ResourceType.ROOM, "Floor 3", true);

        mockMvc.perform(post("/api/resources")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New Room"))
                .andExpect(jsonPath("$.type").value("ROOM"));
    }

    @Test
    void createResource_rejectsInvalidPayload() throws Exception {
        String invalidJson = "{\"name\": \"\", \"location\": \"\"}"; // blank required fields, missing type/available

        mockMvc.perform(post("/api/resources")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.details.name").exists());
    }

    @Test
    void updateResource_deniedForRegularUser() throws Exception {
        Resource saved = resourceRepository.save(sampleResource("Editable Room"));
        ResourceRequest update = new ResourceRequest("Renamed", "desc", ResourceType.ROOM, "Floor 3", false);

        mockMvc.perform(put("/api/resources/" + saved.getId())
                        .header("Authorization", bearer(userToken))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteResource_allowedForAdmin_returnsNotFoundOnSecondDelete() throws Exception {
        Resource saved = resourceRepository.save(sampleResource("Deletable Room"));

        mockMvc.perform(delete("/api/resources/" + saved.getId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/resources/" + saved.getId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getResourceById_returnsNotFoundForMissingId() throws Exception {
        mockMvc.perform(get("/api/resources/999999").header("Authorization", bearer(userToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    private Resource sampleResource(String name) {
        return Resource.builder()
                .name(name)
                .description("A test resource")
                .type(ResourceType.ROOM)
                .location("Floor 1")
                .available(true)
                .build();
    }
}
