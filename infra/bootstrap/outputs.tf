output "state_bucket_name" {
  value       = aws_s3_bucket.state.bucket
  description = "Use this bucket in backend.hcl for the PRE state."
}

output "state_bucket_region" {
  value = var.aws_region
}
