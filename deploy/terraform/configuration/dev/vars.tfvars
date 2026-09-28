environment           = "dev"
aws_region            = "us-east-1"
vpc_id                = "vpc-0fb53b76f02cbe650"
listener_https_arn    = "arn:aws:elasticloadbalancing:us-east-1:052650215423:listener/app/LB-Dev-Pub-AppLoadBalancer/7bac3213d92ada71/49839fb562ea4760"
capacity_provider     = "dev_arm_one_cluster_capacity_provider"
sns_alarms_topic_arn  = "arn:aws:sns:us-east-1:052650215423:dev-borbotones-alarms-topic"
listener_priority     = 9110
environment_variables = {
  SERVER_PORT                             = "8080"
  SERVER_SERVLET_CONTEXT_PATH             = "/api/distribution-engine"
  VAAS_SECURITY_ENABLED                   = "true"
  BASE_URL                                = "https://dev.app.getvaas.com/api"
  NOTIFICATIONS_API_URL                   = "https://dev.app.getvaas.com/api/notifications-bot"
  REDIS___HOST                            = "dev.smr.app.getvaas.com"
  REDIS___PORT                            = "6379"
  REDIS___SSL___ENABLED                   = "true"
  REDIS___SSL___DISABLE_PEER_VERIFICATION = "true"
}
