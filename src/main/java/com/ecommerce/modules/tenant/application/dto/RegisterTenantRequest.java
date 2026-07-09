package com.ecommerce.modules.tenant.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterTenantRequest {

    @NotBlank(message = "Store name is required")
    @Size(min = 2, max = 100)
    private String name;

    @NotBlank(message = "Store slug is required")
    @Size(min = 2, max = 50)
    private String slug;

    private String description;
}
