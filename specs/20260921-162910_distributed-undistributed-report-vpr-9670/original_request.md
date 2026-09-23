Jira ticket: VPR-9670 (https://pmvaas1.atlassian.net/browse/VPR-9670) — "Ejecución: generación del reporte distribuido/no-distribuido".

## Descripción original del ticket

Bloque 5 — Se ejecuta y se avisa. Generar el Excel de 3 tabs (distribuido / tape sin payment / payments sin tape) después de persistir la distribución. Hoy (verificado): `DistributedAndUndistributedReporter.produceReport(config, result)` — ya funciona y ya se adjunta a la instrucción vía `distribution.documents`. Alimentado por: Notifications: Templates (VPR-9640) — este reporte es uno de los templates/documentos que el deal puede elegir adjuntar. Decisión a tomar: confirmar que este reporte ya cubre el detalle que el gate de conciliación con tolerancia necesita mostrar cuando la corrida se frena por exceder el umbral — o si necesita un tab/sección nueva.

## Investigación real realizada antes de escribir la historia

Investigando `master-trust-servicer-api` (Kotlin, `DistributedAndUndistributedReporter.kt`) se encontraron varias discrepancias entre el ticket y el código real:

- El reporte real tiene **2 sheets**, no 3: "Distributed Payments" y "Undistributed Payments" (`payment_tape.distribution_id IS NULL`, sin filtro de fecha). El tercer tab ("payments sin tape") no existe en ningún lado del código real — es aspiracional.
- **Sin conexión real con el gate de conciliación con tolerancia**: cero menciones de "tolerance"/"threshold" en todo el repo real.
- La frase "Alimentado por: Notifications: Templates (VPR-9640)" es engañosa — se leyó VPR-9640 completo y es enteramente sobre selección de adjuntos/subject/recipients para email, no menciona generar ningún Excel en ningún lado.
- El reporte real SÍ está conectado a la instrucción de transferencia (paso 10e, VPR-9639, no VPR-9640): `produceReport()` agrega el documento generado a `distribution.documents`, que luego `notifyTransferInstruction()` lee para adjuntarlo al mismo email.
- Confirmado con un archivo real compartido por el usuario (PayJoy, `distributed-and-undistributed-payments-2026-09-18-210336964.xlsx`, 108MB): 2 sheets exactos ("Distributed Payments" ~23,825 filas, "Undistributed Payments" ~652,652 filas), 22 columnas base (`Id, GatewayCode, GatewayPaymentId, PaymentMethod, ContractType, BorrowerContractId, BorrowerContractDebtorId, BorrowerPaymentId, PaymentDate, TotalPayment, NetAmount, FeeAmount, BorrowerAmount, Currency, PaymentId, DistributionId, OwnerName, CreationDate, PayerLegalId, LoanDebtorLegalId, BorrowerPaymentReference, AtomContractId`) más columnas dinámicas extra que varían por sheet (confirmado: "Undistributed Payments" tenía 2 columnas extra — `Full_name`/`Payer_legal_id_type` — que "Distributed Payments" no tenía, provenientes de un JSON `extraData` variable por borrower/gateway).
- Librería real: `org.apache.poi:poi-ooxml:5.4.0`, usa `SXSSFWorkbook` streaming — justificado por el volumen real confirmado (650k+ filas en un solo borrower).
- Existe un config-gate real (flag global `reporting.distributed-and-undistributed.default-enabled` + override por borrower) — confirmado que WIMO lo tiene explícitamente apagado en prod.
- `DistributionRunner.createDistribution()` es el único funnel compartido por todos los borrowers reales de este motor (cron cada 15 min, consumer SQS, o endpoint manual) — sin ramificación por borrower antes de eso.

## Decisiones de alcance tomadas con el usuario

1. Sin tercer tab (no existe en el sistema real).
2. **Sin persistencia del archivo generado** — decisión técnica, no solo de simplicidad: el reporte tiene 2 mitades de naturaleza distinta. "Distributed Payments" es inmutable una vez persistida la distribución, pero también es la mitad barata de consultar (filtro acotado por un `distributionId` puntual). "Undistributed Payments" es la mitad potencialmente cara pero nunca es estable — cambia todos los días a medida que entran tapes nuevos o se distribuyen otros; cachearla serviría un snapshot desactualizado. Cachear el archivo completo no resuelve el problema de performance real, así que no se agrega esa complejidad. Se recalcula siempre on-demand.
3. Sin disparo automático desde `RunDistributionUseCase`/`/run` — sin ningún consumidor real (notificación/adjunto) en este repo todavía, generarlo automáticamente solo pagaría el costo sin beneficio observable.
4. Sin config-gate (flag global + override por deal) — al ser 100% on-demand, nadie lo pide sin querer.
5. `XSSFWorkbook` simple, no streaming — documentado como límite conocido (no escala al volumen real de 650k+ filas), aceptable porque distri-engine-api no tiene volumen real todavía.
6. Columnas: solo las ya mapeadas en `PaymentTapeEntity` hoy (`id, companyId, gatewayCode, paymentDate, netAmount, totalPayment, ownerName, distributionId, paymentId, fundTransferId`). Sin columnas dinámicas de `extraData`, sin campos no mapeados (`feeAmount`, `borrowerAmount`, `contractType`, etc.).
7. **Sin endpoint HTTP todavía** — el usuario pidió explícitamente construir el use case ahora, listo para exponerse por un endpoint más adelante, pero sin wirear el endpoint en esta historia.
