variable "aws_region" {
  type        = string
  description = "AWS region for the Terraform state bucket."
  default     = "eu-south-2"
}

variable "project_name" {
  type    = string
  default = "AELB"
}

variable "environment" {
  type    = string
  default = "bootstrap"
}

variable "state_bucket_name" {
  type        = string
  description = "Optional globally unique state bucket name. Leave empty to derive it."
  default     = ""
}
