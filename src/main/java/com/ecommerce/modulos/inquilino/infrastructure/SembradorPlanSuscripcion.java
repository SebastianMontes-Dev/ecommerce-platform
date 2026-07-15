package com.ecommerce.modulos.inquilino.infrastructure;

import com.ecommerce.modulos.inquilino.domain.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SembradorPlanSuscripcion implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SembradorPlanSuscripcion.class);
    private final RepositorioPlanSuscripcion planRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (planRepository.count() > 0) {
            log.info("Suscripcion plans already seeded, skipping.");
            return;
        }

        PlanSuscripcion free = new PlanSuscripcion();
        free.setNombre("Free");
        free.setTipoPlan(TipoPlanSuscripcion.FREE);
        free.setPrecio(BigDecimal.ZERO);
        free.setMaxProducts(10);
        free.setTasaComision(new BigDecimal("5.00"));
        free.setFeatures(Map.of(
                "customDomain", false,
                "analytics", false,
                "apiAccess", false,
                "prioritySupport", false,
                "customReports", false
        ));
        free.setActive(true);
        planRepository.save(free);

        PlanSuscripcion pro = new PlanSuscripcion();
        pro.setNombre("Professional");
        pro.setTipoPlan(TipoPlanSuscripcion.PRO);
        pro.setPrecio(new BigDecimal("29.99"));
        pro.setMaxProducts(1000);
        pro.setTasaComision(new BigDecimal("2.00"));
        pro.setFeatures(Map.of(
                "customDomain", true,
                "analytics", true,
                "apiAccess", true,
                "prioritySupport", false,
                "customReports", false
        ));
        pro.setActive(true);
        planRepository.save(pro);

        PlanSuscripcion enterprise = new PlanSuscripcion();
        enterprise.setNombre("Enterprise");
        enterprise.setTipoPlan(TipoPlanSuscripcion.ENTERPRISE);
        enterprise.setPrecio(new BigDecimal("99.99"));
        enterprise.setMaxProducts(100000);
        enterprise.setTasaComision(new BigDecimal("0.50"));
        enterprise.setFeatures(Map.of(
                "customDomain", true,
                "analytics", true,
                "apiAccess", true,
                "prioritySupport", true,
                "customReports", true
        ));
        enterprise.setActive(true);
        planRepository.save(enterprise);

        log.info("Seeded {} suscripcion plans: FREE, PRO, ENTERPRISE", planRepository.count());
    }
}
