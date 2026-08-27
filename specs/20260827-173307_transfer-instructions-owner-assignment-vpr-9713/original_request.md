# Original Request

**Source**: Jira ticket [VPR-9713](https://pmvaas1.atlassian.net/browse/VPR-9713) — "Transfer Instructions: Asignación de owners"
**Cloud ID**: fd15f28a-875e-4a3c-89f3-fb3c51ed1ba1

## Extracted Jira description (as of 2026-08-27)

Definir un S3 path configurable, variable de entorno. Para registrar owners y los mismos servirán
para la asignación de owners en el template de instrucciones. El archivo será
`owner_dictionary.json` y tendrá una estructura json:

```json
{
      "owner_company_id": 5,
      "owner_name": "PAYJOY",
      "from_account_id": 97,
      "to_account_id": 1166,
      "reserve_amount": null,
      "balance_rule": null,
      "template_code": "PAYJOY"
}
```

## Context from related tickets (fetched this session, not this ticket's scope)

- **VPR-9714** ("Transfer Instructions: Creación de owner metadatos"): confirma que en
  `distribution_engine_config`, en la sección de transfer instruction, ya existe una lista de
  "assignments", y agrega un campo `namespace` por assignment para matching de metadata (ej.
  `metadata.amount` + `template_code`). Este dato fue clave para entender que VPR-9713 sí modela
  algo en `config_json` (una nota previa de otra sesión decía lo contrario — quedó superada).
- **VPR-9715** ("Transfer Instructions: Carga de templates"): alta de documentos vía documents
  API, guardando `templateId`. No relacionado a esta lista de assignments.

## User additions (this session)

- Confirmado: el modelo de assignment es una referencia liviana, no una copia completa de los
  campos del `owner_dictionary.json` — solo un campo por asignación.
- Descartado explícitamente `ownerCompanyId` en el assignment: es redundante con `companyId`, que
  ya vive en la raíz de `DistributionConfig` (una distribución siempre es de una company
  particular; una company puede tener varias configs).
- Descartado también un campo `master_servicer_id` nuevo: ya es implícito vía `masterTrustId` en
  la raíz de `DistributionConfig`. Un mismo `master_servicer_id` puede tener múltiples
  `templateOwnerCode` en la lista.
- El campo se llama `templateOwnerCode` (no `templateCode`), para distinguirlo de otros
  identificadores de plantilla del sistema (ej. `templateId` de documentos en VPR-9715).
- Regla de validación explícita del usuario: no se permiten duplicados de `templateOwnerCode`
  dentro de la lista ("no se puede repetir").
- Modelo acordado:
  ```java
  public record TransferInstructionsConfig(
          List<String> templateOwnerCodes
  ) {}
  ```
- Esto agrega un 9º nodo al payload de `DistributionConfigPayload` — hoy no existe ningún campo ni
  placeholder para esto (verificado leyendo el archivo real).
