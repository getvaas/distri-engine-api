# Original Request

Jira: VPR-9667 — "Ejecución: partición de payment tapes ownerless (fix del NPE)" (Bloque 3, paso 6).

Descripción: en `master-trust-servicer-api`, `p.ownerName!! !in listOf(Owner.UNKNOWN, Owner.UNDEFINED)`
— el `!!` es un riesgo real: un tape con `ownerName` null tira NPE y tumba la corrida entera en vez
de caer en la partición de ownerless. Bug documentado en `epica-distri-engine.md` E1. Alimentado por
Ownership (VPR-9635/9636) + E1 de la épica.

**Qué implica (según el ticket):**
1. Reemplazar el `!!` por un fallback a la partición de ownerless (fix de una línea, ya identificado).
2. Confirmar si la notificación de ownerless existente es suficiente, o si hace falta distinguir
   "ownerless por dato faltante" vs. "ownerless por mismatch no resuelto" (VPR-9636).

**Estado ya resuelto de raíz**: el punto 1 (el NPE) ya no puede ocurrir en este repo — `ResolveOwnershipUseCase`
(VPR-9665) nunca devuelve `null`, siempre cae a `"UNDEFINED"`. Lo que faltaba construir era la
partición en sí.

**Decisión del usuario**: el marcador `"UNKNOWN"` (distinto de `"UNDEFINED"`) solo aplica al lado de
Ownership API (diferido, VPR-9665) — con `PAYMENT_TAPE_FIELD` alcanza con chequear el marcador
propio `"UNDEFINED"`. El punto 2 (notificaciones diferenciadas) queda fuera de alcance, igual que en
VPR-9665, porque depende de notificaciones (no existen) y del cross-check de Ownership API (diferido).

Ref: `docs/proceso-distribucion-unificado.md`, Sección 1 paso 6 · `epica-distri-engine.md` E1.
