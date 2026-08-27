**Created at**: 2026-08-27
**Status**: Done
**Original input**: @https://pmvaas1.atlassian.net/browse/VPR-9713
**Plan implemented**: @specs/20260827-173307_transfer-instructions-owner-assignment-vpr-9713/plan.md

# Story: Asignar templates de instrucción de transferencia por owner

### Description
Algunos owners de un deal (lenders, prestamistas) reciben su instrucción de transferencia con una
plantilla específica en vez de la genérica. Hoy esa asociación owner-plantilla vive en un archivo
externo (`owner_dictionary.json`, en S3) que resuelve el detalle completo de cada owner
(cuentas, montos de reserva, regla de balance). El deal necesita poder declarar, dentro de su
propia config, cuáles de esas plantillas (`template_code`) están habilitadas/asignadas para él —
sin duplicar el resto de los datos del owner, que siguen viviendo únicamente en el diccionario
externo.

### Acceptance Criteria
- [x] **Given** una lista de `templateOwnerCodes` sin duplicados, **When** se guarda la config,
  **Then** persiste tal cual.
- [x] **Given** una lista de `templateOwnerCodes` con al menos un valor repetido **dentro del
  mismo registro** (misma `DistributionConfig`), **When** se intenta guardar, **Then** se rechaza
  — un mismo código asignado dos veces en el mismo deal no aporta información nueva y es indicio
  de error de carga.
- [x] **Given** un `templateOwnerCode` que ya está asignado en **otro** registro
  (`DistributionConfig` distinto), **When** se guarda, **Then** no hay conflicto — la unicidad es
  por registro, no global: el mismo owner/plantilla puede estar asignado en varios deals distintos
  al mismo tiempo.
- [x] **Given** una lista vacía o no enviada, **When** se guarda, **Then** persiste sin error —
  significa que este deal no tiene plantillas de owner especiales asignadas todavía.

### Additional Context
Verificado contra el texto real del ticket: el S3 path de `owner_dictionary.json` es una variable
de entorno de infraestructura global, no algo que el wizard configure por deal — eso no cambia con
esta historia. Ese diccionario ya trae, por cada entrada, `owner_company_id`, `owner_name`,
`from_account_id`, `to_account_id`, `reserve_amount`, `balance_rule` y `template_code`.

La unicidad de `templateOwnerCode` aplica solo dentro de un mismo registro de
`distribution_engine_config` — no es una restricción global. El mismo código puede repetirse entre
distintos deals sin problema; lo que no puede pasar es que el mismo deal asigne el mismo código dos
veces.

Se evaluó (y se descartó, en conversación con negocio) incluir `ownerCompanyId` en cada asignación
de esta config: es redundante, porque `companyId` ya identifica al deal completo en la raíz de
`DistributionConfig` — una distribución es siempre de una company particular, y una company puede
tener varias configs. Esta historia agrega un único campo de config (qué plantillas están
asignadas), no una copia de la fila completa del diccionario, que además va a terminar siendo su
propia tabla más adelante. Por el mismo motivo tampoco se agrega un identificador de
`master_servicer_id`: ya es implícito vía `masterTrustId` en la raíz de `DistributionConfig`, y un
mismo `master_servicer_id` puede tener múltiples `templateOwnerCode` asignados sin problema.

El campo se llama `templateOwnerCode` (no `templateCode` a secas) para no confundirlo con otros
identificadores de plantilla del sistema — en particular, el `templateId` de documentos que se
va a guardar por separado al cargar templates (ticket relacionado, fuera de este alcance).

Relacionado, explícitamente fuera de esta historia:
- **VPR-9714** (Creación de owner metadatos): agrega un campo `namespace` a cada asignación, para
  matching de metadata (ej. `metadata.amount` concatenado con el `template_code`) — se construye
  sobre lo que esta historia deja armado, no se adelanta acá.
- **VPR-9715** (Carga de templates): alta de documentos y persistencia de su `templateId` — no
  toca esta lista de asignaciones.

No se encontró el mockup del wizard (`docs/distribution-engine-onboarding.html`) en este
repositorio para verificar la UI real de este paso — esta historia se basa en el texto del ticket
ya refinado más las decisiones de modelado acordadas explícitamente en conversación.
