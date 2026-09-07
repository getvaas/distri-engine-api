# Original Request

Jira: VPR-9662 — "Ejecución: ventana de búsqueda de payment tapes (days back + días hábiles por país)".

El ticket, tal como está escrito, describe únicamente el cálculo de la ventana de fechas (Pool
Strategy → Days back + Deal Info → país) — eso ya estaba construido en una iteración anterior
(`specs/20260821-155208_payment-tape-search-window-working-days-vpr-9662/`, Status: Done) como
`FetchCandidatePaymentTapesUseCase`, expuesto solo por un endpoint de diagnóstico.

Al retomar el ticket, el usuario aclaró que el alcance real es más amplio: "este ticket trata de
tomar esa configuración para usarlo en su implementación" — es decir, VPR-9662 es sobre **usar**
Pool Strategy para alimentar el procesamiento real de una distribución, no solo calcular un valor
aislado. Para eso, el usuario compartió el trace completo (11 pasos) de cómo corre hoy una
distribución real en `master-trust-servicer-api` para un borrower real (payjoy/ADELANTOS,
`master_trust_id 12`), como referencia de "proceso base" contra el cual evaluar qué le falta a la
config de distri-engine-api.

De esa conversación surgieron, en orden:
1. El endpoint de diagnóstico no tiene sentido como pieza pública — se eliminó (mismo criterio que
   el endpoint de readiness checks en VPR-9661).
2. El nombre "candidate" generaba confusión — se renombró a "eligible" (calca "payment tapes
   elegibles" del paso 3 del trace real).
3. Los 3 Pool Strategies (`PAYMENT_TAPE`, `ACCOUNT_BALANCE`, `DATA_SOURCE_AGGREGATION`) necesitan
   evaluarse explícitamente antes de resolver el pool — no alcanza con una guarda puntual en un solo
   use case.
4. `amountField` (VPR-9628) determina qué monto usar por payment tape — se resolvió el monto real
   (sin el fallback de 3 niveles de producción, que es alcance de VPR-9628).
5. Con las piezas (readiness checks de VPR-9661 + resolución de pool de VPR-9662) ya construidas
   por separado, el usuario preguntó si la "distribución base" estaba resuelta para empezar a
   integrar — no lo estaba: faltaba el orquestador que las conecta. Se construyó como el paso final
   de esta iteración.
