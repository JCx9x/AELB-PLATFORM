data "aws_caller_identity" "current" {}
data "aws_kms_alias" "ssm" { name = "alias/aws/ssm" }

resource "aws_iam_role" "ec2" {
  name = "${lower(var.project_name)}-${var.environment}-ec2"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ec2.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy_attachment" "ssm_core" {
  role       = aws_iam_role.ec2.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_role_policy" "ec2_app" {
  name = "${lower(var.project_name)}-${var.environment}-app-access"
  role = aws_iam_role.ec2.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = concat([
      {
        Sid      = "FilesObjects"
        Effect   = "Allow"
        Action   = ["s3:GetObject", "s3:PutObject", "s3:DeleteObject"]
        Resource = ["${var.files_bucket_arn}/*"]
      },
      {
        Sid      = "BackupObjects"
        Effect   = "Allow"
        Action   = ["s3:PutObject"]
        Resource = ["${var.backups_bucket_arn}/*"]
      },
      {
        Sid      = "EcrPull"
        Effect   = "Allow"
        Action   = ["ecr:BatchCheckLayerAvailability", "ecr:BatchGetImage", "ecr:GetDownloadUrlForLayer"]
        Resource = [var.backend_repository_arn, var.frontend_repository_arn]
      },
      {
        Sid      = "EcrAuthToken"
        Effect   = "Allow"
        Action   = ["ecr:GetAuthorizationToken"]
        Resource = "*"
      },
      {
        Sid      = "ReadPreParameters"
        Effect   = "Allow"
        Action   = ["ssm:GetParameter"]
        Resource = "arn:aws:ssm:${var.aws_region}:${data.aws_caller_identity.current.account_id}:parameter${var.parameter_prefix}*"
      },
      {
        Sid      = "DecryptPreParameters"
        Effect   = "Allow"
        Action   = ["kms:Decrypt"]
        Resource = data.aws_kms_alias.ssm.target_key_arn
      },
      {
        # logs:CreateLogStream authorizes on the "log-group" resource type
        # (ARN with no suffix) — see AWS CloudWatch Logs service authorization
        # reference. aws_cloudwatch_log_group.arn is already a bare log-group
        # ARN (the provider strips any ":*" the API returns), so it's used as-is.
        Sid      = "AppLogsCreateStream"
        Effect   = "Allow"
        Action   = ["logs:CreateLogStream"]
        Resource = var.cloudwatch_log_group_arns
      },
      {
        # logs:PutLogEvents authorizes on the "log-stream" resource type
        # (ARN ".../log-group:NAME:log-stream:STREAM"). Docker generates
        # stream names at runtime ("backend-{{.ID}}"), so the stream segment
        # must be wildcarded — a bare log-group ARN does not match it.
        Sid      = "AppLogsPutEvents"
        Effect   = "Allow"
        Action   = ["logs:PutLogEvents"]
        Resource = [for arn in var.cloudwatch_log_group_arns : "${arn}:log-stream:*"]
      }
      ], var.ses_identity_arn == "" ? [] : [{
        Sid      = "SendEmailFromVerifiedIdentity"
        Effect   = "Allow"
        Action   = ["ses:SendEmail"]
        Resource = compact([var.ses_identity_arn, var.ses_recipient_identity_arn])
    }])
  })
}

resource "aws_iam_instance_profile" "ec2" {
  name = "${lower(var.project_name)}-${var.environment}-ec2"
  role = aws_iam_role.ec2.name
}

resource "aws_iam_openid_connect_provider" "github" {
  count           = var.enable_github_oidc ? 1 : 0
  url             = "https://token.actions.githubusercontent.com"
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = ["6938fd4d98bab03faadb97b34396831e3780aea1"]
}

resource "aws_iam_role" "github_deploy" {
  count = var.enable_github_oidc ? 1 : 0
  name  = "${lower(var.project_name)}-${var.environment}-github-deploy"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Federated = aws_iam_openid_connect_provider.github[0].arn }
      Action    = "sts:AssumeRoleWithWebIdentity"
      Condition = {
        StringEquals = {
          "token.actions.githubusercontent.com:aud" = "sts.amazonaws.com"
          "token.actions.githubusercontent.com:sub" = "repo:${var.github_repository}:environment:${var.github_environment}"
        }
      }
    }]
  })
}

resource "aws_iam_role_policy" "github_deploy" {
  count = var.enable_github_oidc ? 1 : 0
  name  = "${lower(var.project_name)}-${var.environment}-github-deploy-policy"
  role  = aws_iam_role.github_deploy[0].id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid      = "PushImages"
        Effect   = "Allow"
        Action   = ["ecr:BatchCheckLayerAvailability", "ecr:CompleteLayerUpload", "ecr:InitiateLayerUpload", "ecr:PutImage", "ecr:UploadLayerPart", "ecr:GetDownloadUrlForLayer", "ecr:BatchGetImage"]
        Resource = [var.backend_repository_arn, var.frontend_repository_arn]
      },
      {
        Sid      = "EcrAuthToken"
        Effect   = "Allow"
        Action   = ["ecr:GetAuthorizationToken"]
        Resource = "*"
      }
    ]
  })
}
