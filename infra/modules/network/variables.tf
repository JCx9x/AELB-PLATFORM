variable "project_name" { type = string }
variable "environment" { type = string }
variable "vpc_cidr" { type = string }
variable "public_subnet_cidr" { type = string }
variable "enable_https_ingress" {
  type    = bool
  default = true
}
