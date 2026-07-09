package com.ecommerce.modules.notification.domain;

import com.ecommerce.modules.shared.infrastructure.BaseJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends BaseJpaRepository<Notification> {
}
