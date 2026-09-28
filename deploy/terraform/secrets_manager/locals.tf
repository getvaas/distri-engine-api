locals {
  existing_secrets = try(jsondecode(data.aws_secretsmanager_secret_version.existing_secret_version.secret_string), {})
  all_secrets = {
    for secret_key in var.secrets :
    secret_key => lookup(local.existing_secrets, secret_key, "")
  }
}
