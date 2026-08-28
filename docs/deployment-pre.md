# CI/CD de PRE

El workflow `.github/workflows/deploy-pre.yml` automatiza el flujo existente: checkout del commit exacto, validaciones, build ARM64 y push a ECR, `scripts/sync-pre-runtime.sh`, `/opt/aelb/deploy-pre.sh` mediante SSM, healthchecks y smoke tests HTTP.

No usa SSH, no descarga secretos en GitHub y no ejecuta Terraform. La EC2 obtiene los secretos desde Parameter Store mediante su Instance Role. El workflow nunca ejecuta `docker compose down -v`, `mkfs`, `wipefs`, Terraform destroy ni recrea la instancia.

## Ejecución

Se ejecuta en cada push a `pre` y mediante `workflow_dispatch`. La concurrencia `aelb-pre-deploy` impide dos despliegues simultáneos y no cancela el que está en curso.

El workflow manual admite `image_tag`. Si se proporciona, reutiliza imágenes existentes de ECR; si se deja vacío, usa `pre-<SHA completo del commit>`.

## GitHub Environment

Crea un Environment llamado `pre` y configura esta variable:

```text
AWS_DEPLOY_ROLE_ARN=arn:aws:iam::537735703239:role/aelb-pre-github-deploy
```

Es un ARN, no un secreto de aplicación. No configures en GitHub contraseñas MySQL, JWT ni Stripe. Puedes añadir aprobación requerida al Environment para introducir una barrera manual.

## OIDC e IAM

Terraform prepara el proveedor `token.actions.githubusercontent.com` y el rol `aelb-pre-github-deploy`. El trust policy acepta únicamente:

```text
repo:JCx9x/AELB-PLATFORM:environment:pre
```

El rol solo puede publicar/leer capas en los dos ECR PRE, enviar comandos SSM a `i-073035077dc228e14` usando `AWS-RunShellScript` y consultar sus resultados. No tiene permisos de Terraform, IAM administrativo, EC2 create/delete, S3, RDS, Route53 o billing.

Si ya existe un proveedor OIDC de GitHub en la cuenta, revisa el plan para no crear un duplicado antes de aplicar.

## Terraform

Desde `infra/environments/pre`, copia `terraform.tfvars.example` a `terraform.tfvars` y establece:

```hcl
enable_github_oidc = true
github_repository = "JCx9x/AELB-PLATFORM"
github_environment = "pre"
```

Después:

```bash
terraform init -backend-config=backend.hcl
terraform validate
terraform plan -out=pre-github-oidc.tfplan
```

Antes de aplicar, confirma que el plan solo crea/actualiza OIDC, el rol y su policy; no debe recrear EC2, EBS MySQL, buckets ni ECR. El comando posterior sería:

```bash
terraform apply pre-github-oidc.tfplan
```

No lo ejecuta este workflow.

## Logs y rollback

La ejecución muestra commit, tag, imágenes, CommandId de sync/deploy, salidas SSM, estado de contenedores y URL PRE. Para depurar, consulta esos CommandId en Systems Manager Run Command y comprueba que la instancia está online y que `/var/lib/aelb/mysql` sigue siendo un filesystem separado.

El actuator solo está expuesto internamente en `/actuator/health/liveness`; Caddy no publica `/actuator`. Por eso la salud del backend se comprueba por SSM y `/api/championships` sirve como smoke test HTTP del backend detrás de Caddy.

No hay rollback automático destructivo. Para repetir una versión, ejecuta manualmente el workflow con `image_tag=pre-<SHA anterior>` existente en ECR. Nunca borres el volumen MySQL para resolver un despliegue fallido.

`EMAIL_ENABLED=false`, `EMAIL_WORKER_ENABLED=false` y `STRIPE_WEBHOOK_ENABLED=false` siguen definidos por `deploy-pre.sh`; los SecureStrings nunca se imprimen ni pasan por GitHub.
