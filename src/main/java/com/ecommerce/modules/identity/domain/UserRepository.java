package com.ecommerce.modules.identity.domain;

import com.ecommerce.modules.shared.infrastructure.BaseJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends BaseJpaRepository<User> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
