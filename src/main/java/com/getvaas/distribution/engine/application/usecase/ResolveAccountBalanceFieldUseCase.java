package com.getvaas.distribution.engine.application.usecase;

import com.getvaas.distribution.engine.domain.model.enums.AccountType;
import com.getvaas.distribution.engine.domain.model.enums.PoolBalanceType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Deriva qué campo de balance corresponde leer para una cuenta, a partir de su {@link AccountType}
 * real y su código — reemplaza la selección manual de {@link PoolBalanceType} que existía antes.
 * Verificado contra el sistema real (`master-trust-servicer-api`, rama {@code develop}):
 * <ul>
 *   <li>{@code COLLECTION}/{@code RESERVE}/{@code INVESTMENT} (caso general): misma fórmula,
 *       {@code USABLE_BALANCE} ({@code projectedBalance ?: currentBalance}).
 *   <li>Excepción puntual por {@code accountCode}, no por tipo: la cuenta {@code WELLI_INVESTMENT}
 *       fuerza {@code CURRENT_BALANCE} siempre, ignorando {@code projectedBalance}.
 * </ul>
 * {@code investmentGains} (VPR real: {@code INVESTMENT} sin la excepción WELLI calcula además una
 * ganancia de inversión vía {@code getInvestmentGainsBetweenDates}, que requiere una distribución
 * previa — {@code lastDistribution} — o explota con {@code IllegalStateException}) queda como gap
 * explícito: no se inventa esa fórmula sin verificar, solo se deja constancia en el log de que el
 * sistema real sumaría algo más acá.
 */
@Slf4j
@Component
public class ResolveAccountBalanceFieldUseCase {

    public static final String WELLI_INVESTMENT_ACCOUNT_CODE = "WELLI_INVESTMENT";

    public PoolBalanceType execute(AccountType accountType, String accountCode) {
        if (WELLI_INVESTMENT_ACCOUNT_CODE.equals(accountCode)) {
            log.info("Cuenta {} ({}): excepción WELLI_INVESTMENT — fuerza CURRENT_BALANCE", accountCode, accountType);
            return PoolBalanceType.CURRENT_BALANCE;
        }

        if (accountType == AccountType.INVESTMENT) {
            log.info("Cuenta {} (INVESTMENT, sin excepción WELLI): el sistema real sumaría "
                    + "investmentGains acá además de USABLE_BALANCE — gap no implementado todavía", accountCode);
        }

        return PoolBalanceType.USABLE_BALANCE;
    }
}
