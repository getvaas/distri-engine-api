**Created at**: 2026-08-21
**Status**: Approved
**Original input**: @https://pmvaas1.atlassian.net/browse/VPR-9629
**Plan implemented**: —

# Story: Pool Strategy — Account Balance

### Description
Algunos deals (Somos, Solvento) no distribuyen la suma de payment tapes, sino el saldo de una o más
cuentas bancarias — el pool es lo que hay en la cuenta, no lo que llegó por pagos. El deal tiene que
poder elegir esta estrategia y decir qué cuentas componen el pool, con qué tipo de saldo cada una.

### Acceptance Criteria
- [ ] **Given** `strategy=ACCOUNT_BALANCE` con al menos una cuenta, **When** se guarda, **Then** cada
  cuenta queda con su `accountId`, `balanceType` y descripción.
- [ ] **Given** una cuenta sin `balanceType` especificado, **When** se guarda, **Then** usa
  `USABLE_BALANCE` por default (igual que "Available balance" preseleccionado en el mockup).
- [ ] **Given** `strategy=ACCOUNT_BALANCE` sin ninguna cuenta, **When** se intenta guardar, **Then** se
  rechaza — no tiene sentido esta estrategia sin al menos una cuenta.
- [ ] **Given** la misma cuenta repetida dos veces en la lista, **When** se intenta guardar, **Then** se
  rechaza — contaría el mismo saldo dos veces en el pool.
- [ ] **Given** una config con `strategy=PAYMENT_TAPE` ya configurada, **When** se cambia a
  `ACCOUNT_BALANCE`, **Then** la config de `paymentTape` anterior no queda mezclada con la nueva.

### Additional Context
Solo 2 tipos de balance existen de verdad: `CURRENT_BALANCE` (columna real `current_balance`) y
`USABLE_BALANCE` (calculado: `projectedBalance ?: currentBalance`). La tercera opción del mockup
("Available - reserves") se descartó — ya se decidió en una conversación anterior que "reserves" es en sí
mismo un valor calculado, no algo restable como opción independiente. El riesgo de que `reservedBalance`
y `usableBalance` coincidan cuando no hay `projectedBalance` cargado queda documentado, no resuelto,
como en la conversación previa. La frescura del saldo (alertar si tiene +48h) y el agujero del tier
"resto" sin balance check quedan para Readiness Checks y Distribution Rules respectivamente — no en esta
historia.
