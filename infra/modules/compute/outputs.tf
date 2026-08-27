output "instance_id" { value = aws_instance.this.id }
output "public_ip" { value = var.enable_eip ? aws_eip.this[0].public_ip : aws_instance.this.public_ip }
output "public_dns" { value = var.enable_eip ? aws_eip.this[0].public_dns : aws_instance.this.public_dns }
output "mysql_volume_id" { value = aws_ebs_volume.mysql.id }
