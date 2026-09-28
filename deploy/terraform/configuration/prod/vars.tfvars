environment           = "prod"
aws_region            = "us-east-1"
vpc_id                = "vpc-0a487bca114ad676a"
listener_https_arn    = "arn:aws:elasticloadbalancing:us-east-1:387023001980:listener/app/LB-Prod-Pub-AppLoadBalancer/209f5e16fc607827/499f69d69fc261db"
capacity_provider     = "prod_arm_large_one_cluster_capacity_provider"
sns_alarms_topic_arn  = "arn:aws:sns:us-east-1:387023001980:prod-borbotones-alarms-topic"
listener_priority     = 9120
environment_variables = {
  SERVER_PORT                             = "8080"
  SERVER_SERVLET_CONTEXT_PATH             = "/api/distribution-engine"
  VAAS_SECURITY_ENABLED                   = "true"
  BASE_URL                                = "https://app.getvaas.com/api"
  NOTIFICATIONS_API_URL                   = "https://app.getvaas.com/api/notifications-bot"
  REDIS___HOST                            = "smr.app.getvaas.com"
  REDIS___PORT                            = "6379"
  REDIS___SSL___ENABLED                   = "true"
  REDIS___SSL___DISABLE_PEER_VERIFICATION = "true"
}
