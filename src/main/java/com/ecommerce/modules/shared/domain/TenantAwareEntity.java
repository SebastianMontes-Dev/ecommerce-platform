package com.ecommerce.modules.shared.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import java.util.UUID;

@Getter
@Setter
@MappedSuperclass
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "idTienda", type = UUID.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :idTienda")
public abstract class TenantAwareEntity extends BaseAuditableEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID idTienda;
}
