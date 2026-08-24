# Original Request

**Source**: Jira ticket [VPR-9631](https://pmvaas1.atlassian.net/browse/VPR-9631) — "Payment filters: Accounting Payments (revisar impacto en la tabla payment tape)"
**Cloud ID**: fd15f28a-875e-4a3c-89f3-fb3c51ed1ba1

## Extracted Jira description (as of 2026-08-21, post-refinamiento)

**Etapa 3 — Payment Filters: Accounting Payments.** Identifica pagos contables (sin respaldo de
caja) y decide si se distribuyen o no. Es una sola card en el mockup
(`docs/screen-payments-filters.png`), con 2 toggles:

```
Toggle 1: "This deal has accounting payments" → activa el builder de condiciones:
  [campo del PT] [= != IN NOT IN IS NULL IS NOT NULL CONTAINS > <] [valor]
  soporta AND entre condiciones + OR groups (2 niveles: grupos OR que contienen ANDs adentro)
  hint: "Values: string, number, null, true/false. For IN use commas: Siniestro, write-off"

Toggle 2: "Distribute accounting payments" → si está desactivado, se excluyen los pagos que
  matchean las condiciones de arriba
```

**Resuelto (2026-08-19/20/21):**
1. No hace falta ninguna columna nueva — la identificación es 100% una condición configurable sobre
   columnas que ya existen en el payment tape (`payment_type: enum "cash/accounting"` es el campo
   típico, no exclusivo).
2. El toggle "Distribute accounting payments" (antes VPR-9632) queda fusionado en esta historia —
   misma card del mockup. VPR-9632 pasa a ser exclusivamente Gateway Filters.
3. Builder AND/OR: solo 2 niveles, sin anidamiento arbitrario.
4. El matiz E5.3b (owner lender/borrower) se resuelve dentro de este mismo builder, como condición
   sobre un campo `owner` — no requiere mecanismo separado.

**Pendiente de confirmar (no bloqueante):** si el hint `"Siniestro, write-off"` es el caso real de
Rapicredit o un ejemplo genérico — no afecta el modelo de datos, solo el copy del wizard.

Ref: `docs/screen-payments-filters.png` · `docs/distribution-engine-onboarding.html` STEP 2 ·
`docs/epica-distri-engine.md` E5.

## User additions (this session)

- Sigue el mismo patrón arquitectónico ya usado en el proyecto: records inmutables Java,
  `@JsonIgnoreProperties(ignoreUnknown = true)`, persistidos dentro de `config_json` vía
  `DistributionConfigPayload`.
- Esta sección tipará una parte de `distributablePayments` (hoy `JsonNode` placeholder en
  `DistributionConfigPayload.java`) — específicamente Accounting Payments. Gateway Filters
  (VPR-9632) tipará otra parte del mismo placeholder, en un ticket separado.
- Endpoint nuevo en `DistributionConfigRouter`, mismo patrón que `PUT /configs/{id}/pool`.
