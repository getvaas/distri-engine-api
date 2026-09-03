# Database Migrations

Migration tool configuration and naming conventions.

## Migration Tool

**Flyway** is configured as a dependency but **currently disabled** in `infra-config.yml`:
```yaml
spring:
  flyway:
    enabled: false
```
Migrations are applied manually/externally against `payments_db`, not on app startup — same
convention as `payment-data-extractor`.

## Migration Location

```
src/main/resources/db/scripts/migration/
```

## Naming Convention

Follows Flyway standard versioned migration format:

```
V{major}.{minor}.{patch}__{description}.sql
```

Example:
```
V1.0.0__baseline_migration.sql
```

## Baseline Migration

The baseline migration (`V1.0.0__baseline_migration.sql`) creates the tables this service owns:
- `distribution_engine_config`

## Scope

Only tables **owned by this service** get migrations here. `payment_tape` is mapped read-only
(`PaymentTapeEntity`) but written by `payment-data-extractor`, which already owns its migration
(`V1.0.0__baseline_migration.sql` in that repo) — never duplicate or modify it from here. Any
future entity that maps a table owned by another service follows the same rule: no migration in
this repo.

## Rules

- Migration files are versioned and immutable once applied
- Use double underscore `__` between version and description
- Description uses `snake_case`
- Each migration file should be idempotent where possible
- DDL changes only — no DML in migration files
