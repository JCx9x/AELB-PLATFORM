module "network" {
  source               = "../../modules/network"
  project_name         = var.project_name
  environment          = var.environment
  vpc_cidr             = var.vpc_cidr
  public_subnet_cidr   = var.public_subnet_cidr
  enable_https_ingress = true
}

# One log group per container. Docker's awslogs driver only needs to write
# into an existing stream/group (see docker-compose.pre.yml); it never gets
# logs:CreateLogGroup, so these must exist before the containers start.
locals {
  log_group_prefix = "/${lower(var.project_name)}/${var.environment}"
  log_services     = ["caddy", "mysql", "backend", "frontend"]
}

resource "aws_cloudwatch_log_group" "app" {
  for_each          = toset(local.log_services)
  name              = "${local.log_group_prefix}/${each.value}"
  retention_in_days = var.log_retention_days
}

module "storage" {
  source                    = "../../modules/storage"
  project_name              = var.project_name
  environment               = var.environment
  aws_region                = var.aws_region
  backup_retention_days     = var.backup_retention_days
  ecr_image_retention_count = var.ecr_image_retention_count
  files_allowed_origins     = var.files_allowed_origins
}

module "iam" {
  source                     = "../../modules/iam"
  project_name               = var.project_name
  environment                = var.environment
  aws_region                 = var.aws_region
  files_bucket_arn           = module.storage.files_bucket_arn
  backups_bucket_arn         = module.storage.backups_bucket_arn
  backend_repository_arn     = module.storage.backend_repository_arn
  frontend_repository_arn    = module.storage.frontend_repository_arn
  parameter_prefix           = var.parameter_prefix
  enable_github_oidc         = var.enable_github_oidc
  github_repository          = var.github_repository
  github_environment         = var.github_environment
  ses_identity_arn           = aws_ses_email_identity.pre.arn
  ses_recipient_identity_arn = "arn:aws:ses:eu-south-1:${data.aws_caller_identity.current.account_id}:identity/javicanoramirez@gmail.com"
  ses_domain_identity_arn    = aws_sesv2_email_identity.domain.arn
  # aws_cloudwatch_log_group.arn already includes a trailing ":*" (AWS API behavior) — do not append it again.
  cloudwatch_log_group_arns = [for g in aws_cloudwatch_log_group.app : g.arn]
}

resource "aws_ses_email_identity" "pre" {
  provider = aws.ses
  email    = var.ses_identity_email
}

# Dominio remitente (noreply@aelb.es). El DNS vive en LucusHost, junto a la web antigua:
# los 3 CNAME DKIM deben apuntar a "<token>.dkim.eu-south-1.amazonses.com" (eu-south-1 es
# región opt-in; el destino global dkim.amazonses.com no existe) y "envios" lleva el MX/SPF de SES.
# Creado inicialmente con la CLI; los bloques import lo incorporan al estado.
resource "aws_sesv2_email_identity" "domain" {
  provider       = aws.ses
  email_identity = var.ses_domain

  dkim_signing_attributes {
    next_signing_key_length = "RSA_2048_BIT"
  }
}

resource "aws_sesv2_email_identity_mail_from_attributes" "domain" {
  provider               = aws.ses
  email_identity         = aws_sesv2_email_identity.domain.email_identity
  mail_from_domain       = "envios.${var.ses_domain}"
  behavior_on_mx_failure = "USE_DEFAULT_VALUE"
}

import {
  provider = aws.ses
  to       = aws_sesv2_email_identity.domain
  id       = "aelb.es"
}

import {
  provider = aws.ses
  to       = aws_sesv2_email_identity_mail_from_attributes.domain
  id       = "aelb.es"
}

module "compute" {
  source                  = "../../modules/compute"
  project_name            = var.project_name
  environment             = var.environment
  aws_region              = var.aws_region
  subnet_id               = module.network.public_subnet_id
  security_group_id       = module.network.security_group_id
  instance_profile_name   = module.iam.instance_profile_name
  instance_type           = var.instance_type
  root_volume_size        = var.root_volume_size
  mysql_volume_size       = var.mysql_volume_size
  enable_eip              = var.enable_eip
  domain_name             = var.domain_name
  files_bucket_name       = module.storage.files_bucket_name
  backups_bucket_name     = module.storage.backups_bucket_name
  backend_repository_url  = module.storage.backend_repository_url
  frontend_repository_url = module.storage.frontend_repository_url
  parameter_prefix        = var.parameter_prefix
}

data "aws_caller_identity" "current" {}

resource "aws_iam_role_policy" "github_ssm_deploy" {
  count = var.enable_github_oidc ? 1 : 0
  name  = "aelb-${var.environment}-github-ssm-deploy"
  role  = try(element(split("/", module.iam.github_deploy_role_arn), 1), "")
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "SendCommandToPreInstance"
        Effect = "Allow"
        Action = "ssm:SendCommand"
        Resource = [
          "arn:aws:ssm:${var.aws_region}:${data.aws_caller_identity.current.account_id}:document/AWS-RunShellScript",
          "arn:aws:ec2:${var.aws_region}:${data.aws_caller_identity.current.account_id}:instance/${module.compute.instance_id}"
        ]
      },
      {
        Sid      = "ReadCommandResult"
        Effect   = "Allow"
        Action   = ["ssm:GetCommandInvocation", "ssm:ListCommands", "ssm:ListCommandInvocations"]
        Resource = "*"
      }
    ]
  })
}

resource "aws_budgets_budget" "monthly" {
  count        = var.billing_alert_email == "" ? 0 : 1
  name         = "${lower(var.project_name)}-${var.environment}-monthly"
  budget_type  = "COST"
  limit_amount = tostring(var.budget_limit_usd)
  limit_unit   = "USD"
  time_unit    = "MONTHLY"

  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                  = 50
    threshold_type             = "PERCENTAGE"
    notification_type          = "FORECASTED"
    subscriber_email_addresses = [var.billing_alert_email]
  }
  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                  = 80
    threshold_type             = "PERCENTAGE"
    notification_type          = "ACTUAL"
    subscriber_email_addresses = [var.billing_alert_email]
  }
  notification {
    comparison_operator        = "GREATER_THAN"
    threshold                  = 100
    threshold_type             = "PERCENTAGE"
    notification_type          = "ACTUAL"
    subscriber_email_addresses = [var.billing_alert_email]
  }
}
