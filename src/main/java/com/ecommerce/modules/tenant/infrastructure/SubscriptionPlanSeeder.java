package com.ecommerce.modules.tenant.infrastructure;

import com.ecommerce.modules.tenant.domain.*;
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
public class SubscriptionPlanSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionPlanSeeder.class);
    private final SubscriptionPlanRepository planRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (planRepository.count() > 0) {
            log.info("Subscription plans already seeded, skipping.");
            return;
        }

        SubscriptionPlan free = new SubscriptionPlan();
        free.setNombre("Free");
        free.setTipoPlan(SubscriptionPlanType.FREE);
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

        SubscriptionPlan pro = new SubscriptionPlan();
        pro.setNombre("Professional");
        pro.setTipoPlan(SubscriptionPlanType.PRO);
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

        SubscriptionPlan enterprise = new SubscriptionPlan();
        enterprise.setNombre("Enterprise");
        enterprise.setTipoPlan(SubscriptionPlanType.ENTERPRISE);
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

        log.info("Seeded {} subscription plans: FREE, PRO, ENTERPRISE", planRepository.count());
    }
}
