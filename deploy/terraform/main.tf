# =============================================================================
# Backend y Provider
# =============================================================================
terraform {
  backend "s3" {}
}

provider "aws" {
  region  = var.aws_region
  profile = var.profile
  default_tags {
    tags = {
      Environment   = var.environment
      Author        = var.author
      CostCenter    = var.cost_center
      ProjectName   = var.project_name_camel_case
      ComponentName = var.component_name_camel_case
      GithubRepo    = var.github_repository
    }
  }
}


# =============================================================================
# Data Sources — Secrets Manager
# =============================================================================
module "secrets_manager" {
  source  = "./secrets_manager"
  name    = "${var.environment}-${var.project_name_kebab_case}-secrets-manager"
  secrets = local.secrets
}

data "aws_secretsmanager_secret" "auth0" {
  name = "${var.environment}-auth0"
}

# =============================================================================
# ECS Service
# =============================================================================
module "ecs_service" {
  source                         = "git@github.com:getvaas/tf_modules.git//ecs_service"
  ecs_memory_min                 = local.ecs_cluster_configuration.memory
  ecs_cpu_millis_min             = local.ecs_cluster_configuration.cpu
  ecs_service_name               = "${var.environment}-distri-engine-api"
  container_port                 = local.target_group_configuration.port
  environment                    = var.environment
  health_check_path              = local.target_group_configuration.health_check_path
  lb_internal_target_group_name  = "${var.environment}-${var.project_name_kebab_case}-lb"
  secrets_manager_arn            = module.secrets_manager.secret_arn
  secrets_manager_keys           = local.secrets
  secrets = { for key in local.auth0_secrets : key => "${data.aws_secretsmanager_secret.auth0.arn}:${key}::" }
  ecs_policy_actions   = local.iam_configuration.ecs_policy.actions
  environment_variables = merge(var.environment_variables, {
    "AWS_REGION"             = var.aws_region
    "SPRING_PROFILES_ACTIVE" = var.environment
    "ENVIRONMENT"            = var.environment
  })
  add_telemetry             = true
  sns_alarms_topic_arn      = var.sns_alarms_topic_arn
  capacity_provider         = var.capacity_provider
  lb_listener_rules = [{
    priority    = var.listener_priority
    action_type = "forward"
    conditions  = [{ field = "path_pattern", values = ["/api/distribution-engine/*"] }]
  }]
  enable_internal_routing = true
  enable_public_routing   = false
}
