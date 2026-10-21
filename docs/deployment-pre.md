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

`STRIPE_WEBHOOK_ENABLED=false` sigue definido por `deploy-pre.sh`. El workflow deja `EMAIL_ENABLED` y `EMAIL_WORKER_ENABLED` en `false` por defecto mediante las variables opcionales del Environment `pre`; actívalas solo después de verificar SES y crear la clave de cifrado indicada abajo. Los SecureStrings nunca se imprimen ni pasan por GitHub.

## Habilitar SES en PRE

PRE sigue ejecutándose en `eu-south-2`, pero el `SesClient` usa la propiedad configurable `EMAIL_SES_REGION`, que en PRE debe ser `eu-south-1`. El SDK usa las credenciales temporales del Instance Profile de la EC2; no se configuran access keys en Docker ni en GitHub. El remitente temporal es `javiercanoramirez@gmail.com` y se envía mediante SES, no mediante Gmail SMTP.

La arquitectura se activa en dos fases:

1. Ejecuta `terraform plan`, revisa y aplica la identidad SES/IAM. Terraform crea una identidad de email en `eu-south-1`; AWS enviará un mensaje de verificación a `javiercanoramirez@gmail.com`. Abre ese mensaje y pulsa el enlace. Comprueba manualmente que la identidad aparece como `Verified` en SES Milán. Terraform no puede completar ese paso por ti.
2. En el Environment `pre`, crea o cambia estas variables:

```text
EMAIL_ENABLED=true
EMAIL_WORKER_ENABLED=true
```

El workflow ya aporta `EMAIL_PROVIDER=ses`, `EMAIL_FROM=javiercanoramirez@gmail.com`, `EMAIL_FROM_NAME=AELB PRE` y `EMAIL_SES_REGION=eu-south-1`.

El cifrado AES/GCM de los payloads de la outbox necesita una clave independiente. Créala manualmente como SecureString (no la guardes en Git ni Terraform):

```bash
aws ssm put-parameter \
  --profile aelb-tf \
  --region eu-south-2 \
  --name /aelb/pre/email/token-encryption-key \
  --type SecureString \
  --value "$(openssl rand -base64 48)" \
  --overwrite
```

`deploy-pre.sh` solo lee esa clave cuando `EMAIL_ENABLED=true`; el Instance Role ya tiene lectura del prefijo `/aelb/pre/`. No se necesita una clave SMTP, password de Gmail ni OAuth.

## SES Sandbox

La cuenta SES puede estar inicialmente en sandbox. En ese caso deben estar verificadas tanto la identidad remitente como las direcciones destinatarias de prueba. Verifica también manualmente una o más cuentas de prueba en SES `eu-south-1`. No se solicita Production Access automáticamente. Más adelante puede solicitarse ese acceso para enviar a destinatarios no verificados.

Casos de correo existentes: verificación tras registro, reenvío de verificación, recuperación de contraseña, confirmación de inscripción y confirmaciones de pago de cuota/inscripción. Todos pasan por la outbox; no se envían dentro de la petición HTTP.

El worker se ejecuta cada 100 ms, reclama como máximo `min(batch-size, worker-concurrency)` trabajos (por defecto 1), limita el envío a `max-per-second` (por defecto 10), recupera trabajos abandonados tras 10 minutos y reintenta fallos temporales hasta 5 intentos con backoff y jitter. Los fallos permanentes pasan a `FAILED`. Además existen límites específicos para registro, forgot-password y reenvío de verificación.

En sandbox, el registro solo debe probarse con un destinatario verificado. Producción usará posteriormente una identidad de dominio propia con DKIM/SPF/DMARC; Gmail es solo una identidad temporal de PRE y no crea ninguna dependencia de producción.

## Acceso a MySQL PRE con DBeaver

Desde la raíz del proyecto, ejecuta:

```bash
./scripts/db-pre-tunnel.sh
```

El script comprueba las credenciales AWS, que la instancia esté `Online` en SSM, y que el contenedor `aelb-pre-mysql-1` esté `running` y `healthy`. Resuelve la IP interna actual del contenedor mediante `AWS-RunShellScript`; no usa SSH ni fija ninguna IP Docker. Mantén la terminal abierta mientras uses DBeaver. Al pulsar `Ctrl+C`, se cierra la sesión SSM y se corta la conexión de DBeaver.

Para usar otro puerto local:

```bash
LOCAL_PORT=3310 ./scripts/db-pre-tunnel.sh
```

Si el puerto ya está ocupado, el script muestra información del listener cuando el sistema la permite y no mata ningún proceso.

Configuración de DBeaver:

```text
Driver:   MySQL
Host:     127.0.0.1
Port:     3308
Username: aelb_user
Database: aelb_db
```

`aelb_db` es la base configurada por `scripts/deploy-pre.sh` y `docker-compose.pre.yml`. La contraseña es el valor de Parameter Store `/aelb/pre/mysql/password`; el script del túnel no lo recupera. Para obtenerla manualmente, usando el perfil habitual y sin guardarla en el repositorio:

```bash
aws ssm get-parameter \
  --profile aelb-tf \
  --region eu-south-2 \
  --name /aelb/pre/mysql/password \
  --with-decryption \
  --query 'Parameter.Value' \
  --output text
```

Propiedades del driver:

```text
allowPublicKeyRetrieval=true
sslMode=DISABLED
```

`sslMode=DISABLED` es aceptable en este uso porque MySQL no está expuesto a Internet y el trayecto entre el ordenador y la instancia va dentro de la sesión AWS SSM. Docker puede asignar otra IP si recrea el contenedor; hay que volver a ejecutar el script para resolver la IP nueva.
