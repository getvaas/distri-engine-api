# Original Request

**Sources**:
- Jira ticket [VPR-9639](https://pmvaas1.atlassian.net/browse/VPR-9639) — "Notifications: Events & Channels"
- Jira ticket [VPR-9640](https://pmvaas1.atlassian.net/browse/VPR-9640) — "Notifications: Templates (subject, recipients, cuerpo de texto, adjuntos)"

**Cloud ID**: fd15f28a-875e-4a3c-89f3-fb3c51ed1ba1

## VPR-9639 — Events & Channels (extracto)
Qué evento dispara qué canal. Hoy el motor real solo reconoce `email` hardcodeado. Escenarios
reales fuera del wizard: Slack (estado interno/error), SNS (escalación), Roam (alerta interna
BIA), SFTP (instrucción, Liquitech/Skandia, E7b). Preguntaba si esta etapa cubre solo instrucción
al cliente o también alerta interna, y si SFTP tiene lugar aquí.

## VPR-9640 — Templates (extracto)
Contenido del mensaje: asunto (✅ ya es config hoy), adjuntos (infra lista, falta selector),
cuerpo (⬛ bloqueado por notifications-api, pregunta sin responder — no estimar), recipients (hoy
hardcodeados). Menciona el caso de Inklusiva con 2 audiencias distintas (cliente/lender) por
corrida.

## User additions (this session)

- El usuario preguntó "qué dudas hay" sobre estos 2 tickets antes de implementar.
- Se resolvieron 2 decisiones de scope basadas en el texto de los tickets (alcance
  cliente-only, avanzar sin `body`), pero al revisar el mockup real
  (`docs/distribution-engine-onboarding.html` STEP 7) se encontró que **contradecía** la primera
  decisión: el mockup mezcla canales internos y de cliente en un solo selector, sin SFTP, y usa
  un solo campo de recipients (no dos audiencias). Se corrigió el modelo para seguir el mockup
  literal en ambos puntos, en vez de la interpretación basada solo en el texto de Jira.
- Endpoint nuevo `PUT /configs/{id}/notifications`, mismo patrón que Payment Filters (un endpoint
  para toda la etapa, dos sub-secciones independientes).
