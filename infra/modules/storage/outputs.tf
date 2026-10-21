output "files_bucket_name" { value = aws_s3_bucket.files.bucket }
output "files_bucket_arn" { value = aws_s3_bucket.files.arn }
output "backups_bucket_name" { value = aws_s3_bucket.backups.bucket }
output "backups_bucket_arn" { value = aws_s3_bucket.backups.arn }
output "backend_repository_url" { value = aws_ecr_repository.backend.repository_url }
output "frontend_repository_url" { value = aws_ecr_repository.frontend.repository_url }
output "backend_repository_arn" { value = aws_ecr_repository.backend.arn }
output "frontend_repository_arn" { value = aws_ecr_repository.frontend.arn }
