package com.ecommerce.modules.tenant.infrastructure;

import com.ecommerce.modules.tenant.application.*;
import com.ecommerce.modules.tenant.application.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
@Tag(name = "Tenants", description = "Multi-tenant store management")
public class TenantController {

    private final RegisterTenantUseCase registerTenantUseCase;
    private final GetTenantUseCase getTenantUseCase;

    @PostMapping
    @Operation(summary = "Register a new store/tenant")
    public ResponseEntity<TenantResponse> register(@Valid @RequestBody RegisterTenantRequest request) {
        TenantResponse response = registerTenantUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get store/tenant by slug (public)")
    public ResponseEntity<TenantResponse> getBySlug(@PathVariable String slug) {
        TenantResponse response = getTenantUseCase.bySlug(slug);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Get my current store/tenant")
    public ResponseEntity<TenantResponse> getMyTenant() {
        TenantResponse response = getTenantUseCase.myTenant();
        return ResponseEntity.ok(response);
    }
}
