resource "aws_secretsmanager_secret" "ecs_secrets_managers" {
  name = var.name
}

data "aws_secretsmanager_secret" "existing_secret" {
  arn = aws_secretsmanager_secret.ecs_secrets_managers.arn
}

data "aws_secretsmanager_secret_version" "existing_secret_version" {
  secret_id  = aws_secretsmanager_secret.ecs_secrets_managers.id
  depends_on = [data.aws_secretsmanager_secret.existing_secret]
}

resource "aws_secretsmanager_secret_version" "ecs_secrets_managers_versions" {
  secret_id     = aws_secretsmanager_secret.ecs_secrets_managers.id
  secret_string = jsonencode(local.all_secrets)
}
