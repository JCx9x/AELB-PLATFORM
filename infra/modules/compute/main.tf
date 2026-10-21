data "aws_ssm_parameter" "ubuntu_arm64" {
  name = "/aws/service/canonical/ubuntu/server/24.04/stable/current/arm64/hvm/ebs-gp3/ami-id"
}

locals {
  name = "${lower(var.project_name)}-${var.environment}"
}

resource "aws_ebs_volume" "mysql" {
  availability_zone = data.aws_subnet.selected.availability_zone
  size              = var.mysql_volume_size
  type              = "gp3"
  encrypted         = true
  tags              = { Name = "${local.name}-mysql" }
}

data "aws_subnet" "selected" { id = var.subnet_id }

resource "aws_instance" "this" {
  ami                         = data.aws_ssm_parameter.ubuntu_arm64.value
  instance_type               = var.instance_type
  subnet_id                   = var.subnet_id
  vpc_security_group_ids      = [var.security_group_id]
  iam_instance_profile        = var.instance_profile_name
  associate_public_ip_address = true
  user_data = templatefile("${path.module}/user-data.sh.tftpl", {
    mysql_volume_id = aws_ebs_volume.mysql.id
  })
  user_data_replace_on_change = false

  root_block_device {
    volume_type           = "gp3"
    volume_size           = var.root_volume_size
    encrypted             = true
    delete_on_termination = true
  }

  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required"
    http_put_response_hop_limit = 1
  }

  tags = { Name = "${local.name}-ec2" }
}

resource "aws_volume_attachment" "mysql" {
  device_name = "/dev/sdf"
  volume_id   = aws_ebs_volume.mysql.id
  instance_id = aws_instance.this.id
}

resource "aws_eip" "this" {
  count  = var.enable_eip ? 1 : 0
  domain = "vpc"
  tags   = { Name = "${local.name}-eip" }
}

resource "aws_eip_association" "this" {
  count         = var.enable_eip ? 1 : 0
  instance_id   = aws_instance.this.id
  allocation_id = aws_eip.this[0].id
}
