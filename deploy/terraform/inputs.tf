# =============================================================================
# Infraestructura
# =============================================================================
variable "environment" {
  type        = string
  description = "Environment name: dev, stg, prod"
}

variable "aws_region" {
  type        = string
  description = "AWS region"
}

variable "vpc_id" {
  type        = string
  description = "VPC ID"
}

# =============================================================================
# Naming (usado en tags y nombres de recursos)
# =============================================================================
variable "project_name_camel_case" {
  type        = string
  description = "Project name in CamelCase, e.g. DistriEngine"
}

variable "project_name_snake_case" {
  type        = string
  description = "Project name in snake_case, e.g. distri_engine"
}

variable "project_name_acronym_snake_case" {
  type        = string
  description = "Project name acronym in snake_case, e.g. distri_engine"
}

variable "component_name_camel_case" {
  type        = string
  description = "Component name in CamelCase, e.g. Api"
}

variable "component_name_snake_case" {
  type        = string
  description = "Component name in snake_case, e.g. api"
}

variable "project_name_kebab_case" {
  type        = string
  description = "Project name in KebabCase, e.g. distri-engine"
}

# =============================================================================
# Metadata (usado en tags)
# =============================================================================
variable "author" {
  type        = string
  description = "Author email"
}

variable "cost_center" {
  type        = string
  description = "Cost center for billing"
}

variable "github_repository" {
  type        = string
  description = "GitHub repository name"
}

# =============================================================================
# ALB y ECS
# =============================================================================
variable "listener_https_arn" {
  type        = string
  description = "ARN of the ALB HTTPS listener"
}

variable "capacity_provider" {
  type        = string
  description = "ECS capacity provider name"
}

variable "listener_priority" {
  type        = string
  description = "AWS Load Balancer Listener priority"
}

# =============================================================================
# Alarms
# =============================================================================
variable "sns_alarms_topic_arn" {
  type        = string
  description = "ARN of the SNS topic for CloudWatch alarms"
}

# =============================================================================
# Environment variables (específicas por ambiente)
# =============================================================================
variable "environment_variables" {
  type        = map(any)
  description = "Environment variables for the container"
  default     = {}
}

# =============================================================================
# AWS Profile (solo para ejecución local)
# =============================================================================
variable "profile" {
  type        = string
  description = "AWS CLI profile name (empty for CI/CD)"
  default     = ""
}
