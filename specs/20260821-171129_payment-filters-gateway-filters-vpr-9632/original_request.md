# Original Request

**Source**: Jira ticket [VPR-9632](https://pmvaas1.atlassian.net/browse/VPR-9632) — "Payment filters: Gateway Filters"
**Cloud ID**: fd15f28a-875e-4a3c-89f3-fb3c51ed1ba1

## Extracted Jira description (as of 2026-08-21, post-refinamiento)

**Etapa 3 — Payment Filters: Gateway Filters.** Incluir/excluir pagos según el gateway de origen.
Card separada de Accounting Payments (`docs/screen-payments-filters-2.png`):

```
Mode: [All gateways (default) | Include only | Exclude]
Gateways: tags (ej. PayU, WOMPI) + "Add gateway"
```

**Resuelto (2026-08-20/21):**
1. El toggle "Distribute accounting payments" no pertenece a esta historia — fusionado en VPR-9631.
2. La lista de gateways (`BANCOLOMBIA, EFECTY, NEQUI, DAVIPLATA, PSE, PayU, WOMPI, JP_MORGAN`) es una
   lista fija, que a futuro va a vivir como enum sincronizado en la API.
3. Este filtro se puede configurar con varias reglas según el/los gateway(s), y esas
   configuraciones son únicamente para el deal en cuestión (no globales a la plataforma).
4. Cuando `Mode = All gateways`, el backend ignora cualquier lista de gateways recibida y persiste
   la lista vacía — no se valida como error.

Ref: `docs/screen-payments-filters-2.png` · `docs/distribution-engine-onboarding.html` STEP 2.

## User additions (this session)

- Reemplaza el placeholder `JsonNode gatewayFilters` de `DistributablePaymentsConfig` (VPR-9631) con
  un tipo concreto.
- Endpoint existente: `PUT /configs/{id}/payment-filters` (VPR-9631) — se extiende
  `UpdatePaymentFiltersRequest` con un nuevo campo `gatewayFilters`, sin duplicar el endpoint (mismo
  patrón que VPR-9629 extendiendo `UpdatePoolConfigRequest`).
