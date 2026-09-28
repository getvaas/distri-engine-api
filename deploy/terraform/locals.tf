locals {
  name            = "${var.environment}-${var.project_name_snake_case}"
  name_kebab_case = "${var.environment}-${var.project_name_kebab_case}"

  # ==========================================================================
  # ECS Configuration
  # ==========================================================================
  ecs_cluster_configuration = {
    memory  = 3072
    cpu     = "2048"
    version = "v1.0.0"
  }

  # ==========================================================================
  # Target Group Configuration
  # ==========================================================================
  target_group_configuration = {
    port              = "8080"
    prefix_path       = "/api/distribution-engine"
    health_check_path = "/api/distribution-engine/actuator/health"
  }

  # ==========================================================================
  # Policy actions
  # ==========================================================================
  # Acotado a lo que el código realmente usa hoy: pull de imagen (ECR), logs
  # (CloudWatch), y GetSecretValue (DB credentials + auth0 compartido). No
  # incluye S3/SQS/SNS/SSM — el bean SnsClient (SnsConfig.java) y el bloque
  # company-api de infra-config.yml existen en el código pero no los usa
  # ninguna clase todavía (scaffold sin wirear); agregar esas actions cuando
  # se implemente algo real que las necesite (ver vaas.infra.add).
  iam_configuration = {
    ecs_policy = {
      name = "${local.name_kebab_case}-ecs-policy"
      role = "${local.name_kebab_case}-ecs-role"
      actions = [
        "ecr:GetAuthorizationToken",
        "ecr:GetDownloadUrlForLayer",
        "ecr:BatchGetImage",
        "ecr:BatchCheckLayerAvailability",
        "ecr:InitiateLayerUpload",
        "ecr:UploadLayerPart",
        "ecr:CompleteLayerUpload",
        "logs:CreateLogStream",
        "logs:PutLogEvents",
        "logs:CreateLogGroup",
        "secretsmanager:GetSecretValue"
      ]
    }
  }

  # ==========================================================================
  # Secrets
  # ==========================================================================
  # Bundle propio del servicio (secrets_manager module, ./secrets_manager) —
  # las 2 conexiones MySQL que usa distri-engine-api (payments_db y
  # master_trust_servicer, ver src/main/resources/infra-config.yml).
  secrets = [
    "DATASOURCE___PAYMENTS_DB___URL",
    "DATASOURCE___PAYMENTS_DB___USERNAME",
    "DATASOURCE___PAYMENTS_DB___PASSWORD",
    "DATASOURCE___MASTER_SERVICER_DB___URL",
    "DATASOURCE___MASTER_SERVICER_DB___USERNAME",
    "DATASOURCE___MASTER_SERVICER_DB___PASSWORD",
  ]

  # Secret compartido "<env>-auth0" (mismas keys que conciliation-engine-api)
  # — vaas_header queda incluido por paridad aunque el bloque company-api de
  # infra-config.yml todavía no lo usa ningún cliente real.
  auth0_secrets = [
    "url",
    "client_id",
    "client_secret",
    "vaas_header"
  ]
}
