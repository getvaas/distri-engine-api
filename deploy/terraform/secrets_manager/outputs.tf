output "secret_arn" {
  value = aws_secretsmanager_secret.ecs_secrets_managers.arn
}
