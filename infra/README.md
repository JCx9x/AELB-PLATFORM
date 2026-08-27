# AELB PRE en AWS

Esta IaC crea un PRE pequeño en `eu-south-2`: una VPC con una única subnet pública, una EC2 Graviton Ubuntu 24.04 ARM64, Caddy, Next.js, Spring Boot y MySQL 8.4 en Docker Compose. La instancia no tiene SSH ni puertos de aplicación públicos: solo Caddy publica 80/443. S3, ECR, Parameter Store, SSM, IAM y un Budget son servicios externos.

## Hallazgos del repositorio

- El Compose local usa MySQL, MinIO y Mailpit; no se modifica.
- `docker-compose.prod.yml` ya usa perfil Spring `prod`, S3 por IAM, SES y MySQL externo. PRE reutiliza ese perfil, pero fuerza `sslMode=REQUIRED` para el MySQL privado del mismo host; no se expone 3306.
- El frontend compila `NEXT_PUBLIC_API_URL`. Las imágenes PRE deben construirse con `NEXT_PUBLIC_API_URL=/api`; Caddy conserva `/api/*` y el body original del webhook al hacer `reverse_proxy`.
- El esquema base es `database/schema.sql` y Hibernate valida (`ddl-auto=validate`). Las migraciones existentes no se cambian.
- El backend ya implementa S3/presigned URLs, Stripe y credenciales por variables; el rol EC2 proporciona la identidad AWS. No se usan access keys.
- El perfil `prod` usa SES para correo transaccional. Es opcional en PRE: si se configura `ses_identity_arn`, el rol obtiene solo `ses:SendEmail`/`ses:SendRawEmail` para esa identidad verificada; si se deja vacío, el despliegue no concede permisos SES.
- El remoto actual es `JCx9x/AELB-PLATFORM`; OIDC de GitHub está desactivado por defecto y se habilita explícitamente con variables.

## Estructura

```text
infra/
  bootstrap/                 # bucket S3 del state, sin backend remoto
  modules/network/            # VPC, subnet, IGW, rutas, SG
  modules/storage/            # S3 de ficheros/backups y ECR
  modules/iam/                # rol EC2, SSM, S3/ECR/Parameter Store y OIDC opcional
  modules/compute/             # AMI Ubuntu ARM64, EC2, EBS MySQL y EIP opcional
  environments/pre/            # composición PRE y outputs
docker-compose.pre.yml
Caddyfile.pre
scripts/put-pre-secrets.sh
scripts/sync-pre-runtime.sh
scripts/deploy-pre.sh
scripts/backup-mysql-pre.sh
```

## Requisitos

- Terraform `>= 1.10`.
- AWS CLI configurado mediante SSO, role asumido o credenciales temporales; no se guardan claves en el repositorio.
- Una identidad AWS con permisos para el bootstrap y para crear la infraestructura indicada.
- Docker solo es necesario para probar localmente o construir las imágenes; la EC2 instala Docker durante el bootstrap.
- `terraform fmt`, `init`, `validate` y `plan` no crean recursos. `apply` sí crea recursos AWS y requiere autorización explícita.

## Primera instalación

El bootstrap debe ejecutarse primero porque el backend S3 no puede configurarse antes de conocer el bucket.

```bash
cd infra/bootstrap
cp terraform.tfvars.example terraform.tfvars
terraform init
terraform plan
terraform apply
terraform output -raw state_bucket_name
```

El resultado de `apply` crea únicamente el bucket de state. Copia `backend.hcl.example` a `backend.hcl`, reemplaza el bucket y luego inicializa PRE:

```bash
cd ../environments/pre
cp terraform.tfvars.example terraform.tfvars
cp backend.hcl.example backend.hcl
# Edita backend.hcl y pon el bucket real del bootstrap
terraform init -backend-config=backend.hcl
terraform plan
terraform apply
```

Si ya existía un state local, Terraform preguntará si deseas migrarlo al backend S3 durante `init`; revísalo antes de confirmar. El backend usa `use_lockfile = true`, el locking nativo de S3. No se crea DynamoDB.

Para probar `t4g.micro`, cambia `instance_type`. PRE usa una Elastic IP (`enable_eip = true`) y expone sus valores estables mediante `terraform output -raw public_ip` y `terraform output -raw public_dns`. Tras `apply`, el hostname AWS puede suministrarse al despliegue como `DOMAIN_NAME="$(terraform output -raw public_dns)"`; `domain_name` permanece vacío porque no se usa Route 53 ni un dominio propio en PRE. Con `DOMAIN_NAME=:80` se sirve HTTP; con un hostname público, Caddy intentará HTTPS automático si los puertos 80/443 son accesibles y la CA acepta emitir para ese nombre.

## Secretos en Parameter Store

Terraform nunca recibe ni gestiona valores secretos. El rol EC2 solo puede leer `/aelb/pre/*`; los parámetros son `SecureString` STANDARD y usan la clave administrada `alias/aws/ssm`.

Después de `apply` y con AWS CLI autenticado:

```bash
cd ../..
chmod 700 scripts/put-pre-secrets.sh
AWS_REGION=eu-south-2 ./scripts/put-pre-secrets.sh
```

Para el primer PRE HTTP, el script solicita sin eco: `mysql/root-password`, `mysql/password`, `jwt/secret` y `stripe/secret-key`. El email está deshabilitado mediante `EMAIL_ENABLED=false` y el webhook Stripe mediante `STRIPE_WEBHOOK_ENABLED=false`; por tanto no se necesitan SES, `EMAIL_FROM`, `email/token-encryption-key` ni `stripe/webhook-secret` todavía. No uses `terraform.tfvars`, outputs, user-data ni Git para estos valores.

Los valores no secretos necesarios para el despliegue son: buckets y repositorios obtenidos por outputs, URLs PRE, `CORS_ALLOWED_ORIGINS`, `EMAIL_FROM`, `EMAIL_FROM_NAME` y el tag SHA de las imágenes.

## Imágenes y despliegue por SSM

Construye ambas imágenes para ARM64. El frontend debe usar `/api` como `NEXT_PUBLIC_API_URL`; usa el SHA del commit como tag, no `latest`:

```bash
docker buildx build --platform linux/arm64 -t BACKEND_REPOSITORY_URL:COMMIT_SHA --push ./backendweb
docker buildx build --platform linux/arm64 --build-arg NEXT_PUBLIC_API_URL=/api -t FRONTEND_REPOSITORY_URL:COMMIT_SHA --push ./aelb-frontend
```

Después de sustituir los valores, sincroniza los ficheros no sensibles y despliega mediante SSM:

```bash
chmod 700 scripts/sync-pre-runtime.sh
INSTANCE_ID="$(cd infra/environments/pre && terraform output -raw instance_id)" \
FILES_BUCKET="$(cd infra/environments/pre && terraform output -raw files_bucket)" \
BACKUPS_BUCKET="$(cd infra/environments/pre && terraform output -raw backups_bucket)" \
AWS_REGION=eu-south-2 \
BACKEND_IMAGE="BACKEND_REPOSITORY_URL:COMMIT_SHA" \
FRONTEND_IMAGE="FRONTEND_REPOSITORY_URL:COMMIT_SHA" \
CORS_ALLOWED_ORIGINS="http://ec2-35-43-130-103.eu-south-2.compute.amazonaws.com" \
FRONTEND_PUBLIC_URL="http://ec2-35-43-130-103.eu-south-2.compute.amazonaws.com" \
STRIPE_SUCCESS_URL="http://ec2-35-43-130-103.eu-south-2.compute.amazonaws.com/perfil?stripe=success&session_id={CHECKOUT_SESSION_ID}" \
STRIPE_CANCEL_URL="http://ec2-35-43-130-103.eu-south-2.compute.amazonaws.com/perfil?stripe=cancelled" \
DOMAIN_NAME="ec2-35-43-130-103.eu-south-2.compute.amazonaws.com" \
PRE_URL="http://ec2-35-43-130-103.eu-south-2.compute.amazonaws.com" \
STRIPE_WEBHOOK_ENABLED=false ./scripts/sync-pre-runtime.sh
```

El script coloca temporalmente solo configuración no sensible, `schema.sql` y scripts en `_runtime/` del bucket de ficheros, los descarga por SSM y ejecuta `docker compose pull`/`up`. Los secretos se recuperan dentro de la EC2 desde Parameter Store. Para el hostname AWS de PRE, usa `DOMAIN_NAME="$(terraform output -raw public_dns)"`; si no se quiere intentar HTTPS, usa `DOMAIN_NAME=:80`.

El script sincroniza además un `systemd timer` diario a las 03:15 UTC y lo activa después de crear `pre.env`. La subida usa el Instance Role; el fichero temporal se elimina siempre con `trap`. S3 Lifecycle elimina backups tras `backup_retention_days` (21 por defecto).

## Session Manager

No se crea key pair y el SG no permite 22:

```bash
aws ssm start-session --target INSTANCE_ID --region eu-south-2
```

El rol incluye `AmazonSSMManagedInstanceCore`, que contiene algunos recursos `*` porque la integración administrada de SSM lo requiere. Las acciones específicas de S3, ECR y Parameter Store del rol inline están restringidas a sus buckets, repositorios y prefijo.

## CI/CD futuro

El módulo IAM puede crear, con `enable_github_oidc = true`, un proveedor OIDC y un rol restringido a `github_repository` y `github_branch`. Ese rol tiene push sobre los dos ECR y, mediante una policy del entorno, `ssm:SendCommand` solo al documento `AWS-RunShellScript` y a la instancia PRE. El despliegue seguirá separado de Terraform: GitHub Actions ejecutará tests, build ARM64, push ECR y el comando SSM. No se guardan AWS access keys permanentes. Si la cuenta ya tiene el proveedor GitHub OIDC, revisa antes de activar la variable para no duplicarlo.

## Destrucción y persistencia

```bash
cd infra/environments/pre
terraform destroy
```

Esto elimina VPC, EC2, EBS MySQL, buckets PRE, repositorios ECR, IAM y Budget gestionados por PRE. S3 versioning no convierte los buckets en inborrables: para destruirlos deben estar vacíos, incluidos objetos/versiones. El bucket de Terraform state está en `infra/bootstrap` y sobrevive hasta destruirlo explícitamente. Los backups solo sobreviven mientras exista su bucket; exporta o copia los que deban conservarse antes de destruir PRE. No hay `prevent_destroy` para mantener la recreabilidad explícita.

## Costes y advertencias

El coste principal es la EC2 `t4g.small` (y sus horas aunque no haya tráfico), EBS gp3, S3/ECR por almacenamiento y operaciones, y la transferencia de datos. Una IPv4 pública asignada tiene coste según la tarificación vigente; `enable_eip=false` usa la IP pública automática, que también puede generar coste. Una Elastic IP puede generar coste incluso cuando la EC2 está detenida o no presta servicio, por lo que solo debe habilitarse si se necesita IP estable.

No se crean RDS, ECS/Fargate, ALB, NAT Gateway, ElastiCache, CloudFront, WAF, Kubernetes, Secrets Manager ni CloudWatch Logs Agent. Se usan métricas estándar de EC2 y logs locales Docker/journald con rotación; no se envía un volumen de logs a CloudWatch. El Budget es opcional (`billing_alert_email`) y su email debe confirmarse según AWS.

## Validación local

Desde el entorno con Terraform instalado:

```bash
terraform fmt -recursive infra
terraform -chdir=infra/bootstrap init
terraform -chdir=infra/bootstrap validate
terraform -chdir=infra/environments/pre init -backend-config=backend.hcl
terraform -chdir=infra/environments/pre validate
terraform -chdir=infra/environments/pre plan
```

En este checkout no están instalados `terraform` ni `tflint`, por lo que no se ejecutó `apply`, `destroy` ni se tocó AWS.
