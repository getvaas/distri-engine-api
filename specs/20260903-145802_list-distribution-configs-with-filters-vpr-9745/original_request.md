Jira: VPR-9745 — https://pmvaas1.atlassian.net/browse/VPR-9745

**Resumen (Historia, En curso, Medium):**
Crear endpoint para obtener la lista de distribution_engine_config, que se pueda filtrar:
- Nombre
- Master Servicer Id
- Company Id

## Respuestas de clarificación

1. "Master Servicer Id" del ticket mapea al campo `masterTrustId` que ya existe en la entity.
2. Filtro por nombre: búsqueda parcial (LIKE %texto%), no match exacto.
3. El listado necesita paginación (page/size).
