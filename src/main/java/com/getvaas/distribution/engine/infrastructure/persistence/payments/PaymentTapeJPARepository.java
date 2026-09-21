package com.getvaas.distribution.engine.infrastructure.persistence.payments;

import com.getvaas.distribution.engine.infrastructure.persistence.payments.entity.PaymentTapeEntity;
import com.getvaas.distribution.engine.infrastructure.persistence.payments.entity.PaymentTapeId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * {@code payment_tape} está particionada por {@code company_id} (ver {@link PaymentTapeEntity}) — todo
 * método nuevo de este repositorio tiene que llevar {@code companyId} en el {@code WHERE}. No agregar
 * una query que lo omita.
 */
public interface PaymentTapeJPARepository extends JpaRepository<PaymentTapeEntity, PaymentTapeId> {

    List<PaymentTapeEntity> findByCompanyIdAndPaymentDateBetweenAndDistributionIdIsNull(
            Long companyId, LocalDateTime fromDate, LocalDateTime untilDate);

    // VPR-9670: sheets del reporte distribuido/no-distribuido — "no distribuido" es sin filtro de
    // fecha (coincide con el comportamiento verificado del motor real, paso 10d).
    List<PaymentTapeEntity> findByCompanyIdAndDistributionId(Long companyId, String distributionId);

    List<PaymentTapeEntity> findByCompanyIdAndDistributionIdIsNull(Long companyId);
}
