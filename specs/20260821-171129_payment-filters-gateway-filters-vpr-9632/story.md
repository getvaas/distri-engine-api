**Created at**: 2026-08-21
**Status**: In Progress
**Original input**: @https://pmvaas1.atlassian.net/browse/VPR-9632
**Plan implemented**: —

# Story: Payment Filters — Gateway Filters

### Description
Algunos deals solo deben distribuir pagos que llegaron por ciertos gateways (o excluir los que
llegaron por algunos), porque no todos los canales de cobro son parte del pool distribuible. El
deal necesita poder elegir el modo (todos / solo estos / excepto estos) y la lista de gateways.

### Acceptance Criteria
- [ ] **Given** `mode=INCLUDE_ONLY` con al menos un gateway, **When** se guarda, **Then** la config
  persiste el modo y la lista de gateways tal cual.
- [ ] **Given** `mode=EXCLUDE` con al menos un gateway, **When** se guarda, **Then** la config
  persiste el modo y la lista de gateways tal cual.
- [ ] **Given** `mode=ALL`, **When** se guarda con gateways no vacíos en el request, **Then** el
  backend los ignora y persiste la lista vacía — no se valida como error.
- [ ] **Given** `mode=INCLUDE_ONLY` o `EXCLUDE` sin ningún gateway, **When** se intenta guardar,
  **Then** se rechaza — no tiene sentido incluir/excluir sin especificar de qué.
- [ ] **Given** el request no envía `mode`, **When** se guarda, **Then** se asume `ALL` por default
  (comportamiento actual sin filtro, igual que el mockup).

### Additional Context
La lista de gateways válidos (`BANCOLOMBIA, EFECTY, NEQUI, DAVIPLATA, PSE, PayU, WOMPI, JP_MORGAN` en
el mockup) es fija por ahora y se modela como `String` libre, no como enum cerrado — a futuro va a
vivir como un enum sincronizado en la API a medida que se agreguen gateways, pero eso queda fuera de
esta historia. Esta configuración es siempre por deal, nunca global a la plataforma. Accounting
Payments es una card separada del mockup y un ticket distinto (VPR-9631, ya implementado) — tipa el
campo `accountingPayments` de `DistributablePaymentsConfig`; esta historia tipa el campo
`gatewayFilters` de la misma clase, reemplazando su placeholder `JsonNode`.
