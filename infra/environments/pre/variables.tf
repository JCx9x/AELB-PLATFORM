variable "project_name" {
  type    = string
  default = "AELB"
}
variable "environment" {
  type    = string
  default = "pre"
}
variable "aws_region" {
  type    = string
  default = "eu-south-2"
}
variable "vpc_cidr" {
  type    = string
  default = "10.20.0.0/16"
}
variable "public_subnet_cidr" {
  type    = string
  default = "10.20.1.0/24"
}
variable "instance_type" {
  type    = string
  default = "t4g.small"
}
variable "root_volume_size" {
  type    = number
  default = 20
}
variable "mysql_volume_size" {
  type    = number
  default = 20
}
variable "enable_eip" {
  type    = bool
  default = false
}
variable "domain_name" {
  type    = string
  default = ""
}
variable "backup_retention_days" {
  type    = number
  default = 21
}
variable "ecr_image_retention_count" {
  type    = number
  default = 8
}
variable "billing_alert_email" {
  type    = string
  default = ""
}
variable "budget_limit_usd" {
  type    = number
  default = 20
}
variable "enable_github_oidc" {
  type    = bool
  default = false
}
variable "github_repository" {
  type    = string
  default = "JCx9x/AELB-PLATFORM"
}
variable "github_environment" {
  type    = string
  default = "pre"
}

variable "github_branch" {
  type        = string
  default     = "pre"
  description = "Deprecated compatibility variable; OIDC trust is restricted to github_environment."
}
variable "ses_identity_arn" {
  type        = string
  default     = ""
  description = "Optional verified SES identity ARN for transactional email."
}
variable "ses_domain" {
  type        = string
  default     = "aelb.es"
  description = "Domain verified in SES (eu-south-1) used as sender, e.g. noreply@aelb.es."
}
variable "ses_identity_email" {
  type        = string
  default     = "javiercanoramirez@gmail.com"
  description = "PRE SES email identity to verify manually in eu-south-1."
}

variable "parameter_prefix" {
  type        = string
  description = "Prefix for SecureString parameters read by the instance."
  default     = "/aelb/pre/"
  validation {
    condition     = startswith(var.parameter_prefix, "/") && endswith(var.parameter_prefix, "/")
    error_message = "parameter_prefix must start and end with '/'."
  }
}

variable "log_retention_days" {
  type        = number
  description = "Retention for the application CloudWatch Log Groups (one per container)."
  default     = 14
  validation {
    # https://docs.aws.amazon.com/AmazonCloudWatchLogs/latest/APIReference/API_PutRetentionPolicy.html
    condition = contains([
      1, 3, 5, 7, 14, 30, 60, 90, 120, 150, 180, 365, 400, 545, 731, 1096,
      1827, 2192, 2557, 2922, 3288, 3653, 0
    ], var.log_retention_days)
    error_message = "log_retention_days must be one of the values CloudWatch Logs accepts for retentionInDays (0 = never expire)."
  }
}
