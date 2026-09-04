package com.getvaas.distribution.engine.infrastructure.persistence.masterservicer;

import com.getvaas.distribution.engine.infrastructure.persistence.masterservicer.entity.DistributionEngineConfigEntity;
import org.springframework.data.jpa.domain.Specification;

/**
 * Specifications para el listado de {@code distribution_engine_config} (VPR-9745). Cada filtro es
 * opcional e independiente — cuando el valor es {@code null}/blank no restringe la query (devuelve
 * una spec "no-op", nunca {@code null}: en esta versión de Spring Data JPA, {@code Specification.and}
 * valida explícitamente que el otro operando no sea {@code null}) — para poder combinarlos libremente
 * por AND según qué filtros mande el caller.
 */
public class DistributionConfigSpecifications {

    private DistributionConfigSpecifications() {
    }

    private static final Specification<DistributionEngineConfigEntity> NO_OP =
            (root, query, cb) -> cb.conjunction();

    public static Specification<DistributionEngineConfigEntity> hasName(String name) {
        if (name == null || name.isBlank()) {
            return NO_OP;
        }
        var pattern = "%" + name.toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern);
    }

    public static Specification<DistributionEngineConfigEntity> hasMasterTrustId(Long masterTrustId) {
        if (masterTrustId == null) {
            return NO_OP;
        }
        return (root, query, cb) -> cb.equal(root.get("masterTrustId"), masterTrustId);
    }

    public static Specification<DistributionEngineConfigEntity> hasCompanyId(Long companyId) {
        if (companyId == null) {
            return NO_OP;
        }
        return (root, query, cb) -> cb.equal(root.get("companyId"), companyId);
    }

    public static Specification<DistributionEngineConfigEntity> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }
}
