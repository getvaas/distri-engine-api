# Environment Variables

## Datasource

| Variable | Required | Description |
|---|---|---|
| `DATASOURCE___PAYMENTS_DB___URL` | Yes | JDBC URL de `payments_db` (MySQL) |
| `DATASOURCE___PAYMENTS_DB___USERNAME` | Yes | Usuario |
| `DATASOURCE___PAYMENTS_DB___PASSWORD` | Yes | Password |

## Redis / ShedLock

| Variable | Required | Description |
|---|---|---|
| `REDIS___HOST` | No | Default `dev.smr.app.getvaas.com` |
| `REDIS___PORT` | No | Default `6379` |
| `REDIS___SSL___ENABLED` | No | Default `false` |
| `REDIS___SSL___DISABLE_PEER_VERIFICATION` | No | Default `false` |

## Vaas Security

| Variable | Required | Description |
|---|---|---|
| `VAAS_SECURITY_ENABLED` | No | Default `false` — activar cuando se definan los endpoints reales |
| `VAAS_SECURITY_JWK_DOMAIN` | Yes (si `enabled=true`) | Dominio Auth0/Keycloak |
| `INFRA_SECURITY_INTERNAL_SERVICE_HEADER` | Yes (si aplica) | Header interno de servicio a servicio |

## AWS / SNS

| Variable | Required | Description |
|---|---|---|
| `AWS_REGION` | No | Default `us-east-1` |
| `AWS_ENDPOINT` | No | Para apuntar a LocalStack en local |
| `AWS_ACCESS_KEY` / `AWS_SECRET_ACCESS_KEY` | No | Si no se setean, usa `DefaultCredentialsProvider` |
| `SNS_TOPIC_ARN` | No | Tópico de eventos de distribución |
