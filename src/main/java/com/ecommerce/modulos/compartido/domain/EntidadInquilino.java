package com.ecommerce.modulos.compartido.domain;

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
@FilterDef(name = "filtroInquilino", parameters = @ParamDef(name = "idTienda", type = UUID.class))
@Filter(name = "filtroInquilino", condition = "tenant_id = :idTienda")
public abstract class EntidadInquilino extends EntidadAuditableBase {

    @Column(name = "tenant_id", nullable = false)
    private UUID idTienda;
}
