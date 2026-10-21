output "instance_profile_name" { value = aws_iam_instance_profile.ec2.name }
output "instance_role_arn" { value = aws_iam_role.ec2.arn }
output "github_deploy_role_arn" { value = try(aws_iam_role.github_deploy[0].arn, null) }
