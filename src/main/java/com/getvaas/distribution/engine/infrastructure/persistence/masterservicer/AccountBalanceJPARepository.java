package com.getvaas.distribution.engine.infrastructure.persistence.masterservicer;

import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.entity.AccountBalanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountBalanceJPARepository extends JpaRepository<AccountBalanceEntity, String> {

    Optional<AccountBalanceEntity> findFirstByAccountIdOrderByCreationDateDesc(Long accountId);
}
