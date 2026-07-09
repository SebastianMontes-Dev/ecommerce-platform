package com.ecommerce.modules.tenant.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantResponse {

    private UUID id;
    private String name;
    private String slug;
    private String description;
    private String logoUrl;
    private String bannerUrl;
    private String status;
    private UUID ownerId;
    private LocalDateTime createdAt;
}
