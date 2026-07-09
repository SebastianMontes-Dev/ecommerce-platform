package com.ecommerce.modules.shared.infrastructure;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

@Component
public class HibernateTenantFilterConfig {

    private static final Logger log = LoggerFactory.getLogger(HibernateTenantFilterConfig.class);

    private final PlatformTransactionManager transactionManager;

    public HibernateTenantFilterConfig(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void enableTenantFilter() {
        TransactionTemplate tt = new TransactionTemplate(transactionManager);
        tt.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                log.info("Hibernate tenant filter registered");
            }
        });
    }

    public static void enableFilter(Session session, UUID tenantId) {
        if (tenantId != null) {
            session.enableFilter("tenantFilter")
                    .setParameter("tenantId", tenantId);
        }
    }
}
