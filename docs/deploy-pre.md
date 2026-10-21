# Despliegue de PRE

## Resumen

`Deploy PRE` es el workflow de GitHub Actions que valida, construye y despliega la aplicación en el entorno de preproducción de AELB.

El despliegue se realiza sobre la instancia EC2 PRE mediante AWS Systems Manager (SSM). La base de datos MySQL permanece en la instancia y utiliza su volumen persistente; el workflow no elimina ni recrea ese volumen.

Componentes principales:

- Repositorio: `JCx9x/AELB-PLATFORM`
- Rama de PRE: `pre`
- Región AWS: `eu-south-2`
- Instancia EC2: `i-073035077dc228e14`
- URL PRE: `http://ec2-35-43-130-103.eu-south-2.compute.amazonaws.com`
- Registro ECR: `537735703239.dkr.ecr.eu-south-2.amazonaws.com`
- Environment de GitHub: `pre`

## Cuándo se ejecuta

El workflow está definido en `.github/workflows/deploy-pre.yml` y se ejecuta en estos casos:

1. Automáticamente al hacer `push` a la rama `pre`.
2. Manualmente mediante `workflow_dispatch` desde GitHub Actions.

El environment de GitHub `pre` está restringido a la rama `pre`. Para un despliegue manual se debe seleccionar esa rama.

La concurrencia utiliza el grupo `aelb-pre-deploy` y no cancela una ejecución que ya esté en curso. Esto evita dos despliegues simultáneos sobre la misma instancia.

## Qué hace paso a paso

### 1. Selecciona el commit

GitHub Actions hace checkout del commit exacto que disparó la ejecución. En una ejecución manual, es el commit de la rama seleccionada en el formulario `Run workflow`.

Antes de desplegar se muestra el valor de `GITHUB_SHA` en los logs.

### 2. Valida el backend

En `backendweb` ejecuta:

```bash
./mvnw -B clean verify
```

Esto compila y ejecuta las pruebas del backend. Las pruebas usan una base de datos H2 en memoria definida en `backendweb/src/test/resources/application.properties`; no deben conectarse a MySQL de PRE.

### 3. Valida el frontend

En `aelb-frontend` ejecuta:

```bash
npm ci
npm test
npx tsc --noEmit
NEXT_PUBLIC_API_URL=/api npm run build
```

Si cualquiera de estas validaciones falla, el workflow se detiene antes de acceder a AWS.

### 4. Obtiene credenciales AWS mediante OIDC

GitHub no almacena claves permanentes de AWS. El job solicita un token OIDC y asume este rol:

```text
arn:aws:iam::537735703239:role/aelb-pre-github-deploy
```

El rol confía únicamente en el repositorio `JCx9x/AELB-PLATFORM` usando el environment `pre` o la rama `pre`.

El workflow necesita estos permisos globales de job:

```yaml
permissions:
  contents: read
  id-token: write
```

La variable `AWS_DEPLOY_ROLE_ARN` se configura en el environment de GitHub `pre`.

### 5. Selecciona el tag de las imágenes

Si `image_tag` está vacío, usa:

```text
pre-<GITHUB_SHA>
```

Ese tag identifica de forma inmutable el commit desplegado.

Si `image_tag` contiene un valor, el workflow reutiliza las imágenes existentes con ese tag y no las vuelve a construir.

Los nombres de imagen son:

```text
537735703239.dkr.ecr.eu-south-2.amazonaws.com/aelb-pre-backend:<tag>
537735703239.dkr.ecr.eu-south-2.amazonaws.com/aelb-pre-frontend:<tag>
```

El tag se valida para impedir valores arbitrarios o inválidos.

### 6. Construye y publica las imágenes

Cuando se necesita construir, `scripts/build-pre.sh`:

- Inicia sesión en ECR.
- Construye las imágenes para arquitectura `linux/arm64`, compatible con la instancia `t4g`.
- Publica backend y frontend en ECR.
- Usa la caché de GitHub Actions para acelerar las compilaciones.

Este paso no se ejecuta cuando se proporciona un `image_tag` existente.

### 7. Sincroniza el runtime mediante SSM

`scripts/sync-pre-runtime.sh` envía a la instancia mediante SSM los archivos y configuración necesarios para PRE, entre ellos:

- `docker-compose.pre.yml`
- `Caddyfile.pre`
- `database/schema.sql`
- `scripts/deploy-pre.sh`
- Scripts y unidades del backup de MySQL
- `/opt/aelb/config/deploy.env`

El script no abre SSH ni necesita una clave privada. La instancia debe estar registrada y conectada a SSM.

### 8. Ejecuta el despliegue en la instancia

El workflow ejecuta mediante SSM:

```bash
/opt/aelb/deploy-pre.sh
```

El script de la instancia descarga/usa las imágenes indicadas y levanta los servicios PRE.

### 9. Comprueba los contenedores y el volumen MySQL

Se comprueban estos cuatro contenedores:

```text
aelb-pre-mysql-1
aelb-pre-backend-1
aelb-pre-frontend-1
aelb-pre-caddy-1
```

Todos deben estar `running` y `healthy`. También se verifica que `/var/lib/aelb/mysql` esté montado sobre el volumen esperado:

```text
3911d963-0a55-4ea7-9898-855e58e67ef2
```

Si el volumen no coincide, el workflow falla para evitar desplegar sobre un almacenamiento incorrecto.

### 10. Ejecuta smoke tests HTTP

Finalmente comprueba:

```text
GET /
GET /api/championships
```

La ejecución solo termina correctamente si ambas respuestas HTTP son satisfactorias.

## Ejecución manual

En GitHub:

1. Abrir `Actions`.
2. Seleccionar `Deploy PRE`.
3. Pulsar `Run workflow`.
4. Seleccionar la rama `pre`.
5. Dejar `image_tag` vacío para construir el commit seleccionado, o indicar un tag existente para reutilizar imágenes.
6. Pulsar `Run workflow`.

Para desplegar el código actual de PRE se recomienda dejar `image_tag` vacío.

Para redeplegar exactamente una imagen anterior se puede indicar, por ejemplo:

```text
pre-b9a0a032ed3c6c1e61dce18bf82edf526df0a4ef
```

El tag debe existir en ambos repositorios ECR.

## Ejecución con GitHub CLI

Consultar las ejecuciones:

```bash
gh run list --repo JCx9x/AELB-PLATFORM --workflow 'Deploy PRE'
```

Lanzar el despliegue del commit actual de `pre`:

```bash
gh workflow run 'Deploy PRE' \
  --repo JCx9x/AELB-PLATFORM \
  --ref pre
```

Redeplegar un tag existente:

```bash
gh workflow run 'Deploy PRE' \
  --repo JCx9x/AELB-PLATFORM \
  --ref pre \
  -f image_tag=pre-<tag>
```

No se debe ejecutar el workflow desde `main` para desplegar PRE.

## IAM y OIDC

La cuenta AWS utiliza el proveedor:

```text
arn:aws:iam::537735703239:oidc-provider/token.actions.githubusercontent.com
```

El rol `aelb-pre-github-deploy` tiene permisos limitados a:

- Publicar imágenes en `aelb-pre-backend` y `aelb-pre-frontend`.
- Enviar comandos SSM a `i-073035077dc228e14` usando `AWS-RunShellScript`.
- Leer el resultado de los comandos SSM.

No utiliza claves AWS permanentes dentro de GitHub.

La política de confianza debe permitir el subject OIDC del environment:

```text
repo:JCx9x@82141312/AELB-PLATFORM@1324905612:environment:pre
```

GitHub puede incluir los identificadores internos de la organización y del repositorio en el subject OIDC. El valor anterior es el observado en CloudTrail para este repositorio.

El environment de GitHub está configurado para aceptar únicamente despliegues desde `pre`.

## Errores conocidos y diagnóstico

### `Missing GitHub Environment variable AWS_DEPLOY_ROLE_ARN`

La variable no existe en el environment `pre`. Debe contener:

```text
arn:aws:iam::537735703239:role/aelb-pre-github-deploy
```

### `Could not assume role with OIDC: token ... could not be validated`

Normalmente indica que falta el proveedor OIDC de GitHub en IAM o que el rol referenciado no existe.

Comprobación:

```bash
aws iam list-open-id-connect-providers --profile aelb
aws iam get-role --profile aelb --role-name aelb-pre-github-deploy
```

### `Not authorized to perform sts:AssumeRoleWithWebIdentity`

La política de confianza del rol no coincide con el `sub` emitido por GitHub. Si el job utiliza `environment: pre`, debe permitirse el subject `environment:pre`, no solo `ref:refs/heads/pre`.

### `Failed to determine a suitable driver class`

El contexto de pruebas Spring no encontraba una configuración de datasource. Las pruebas del backend deben tener el driver H2 y una configuración de base de datos en memoria en `src/test/resources`.

### Falla en SSM

Comprobar:

- Que la instancia `i-073035077dc228e14` esté registrada como managed instance.
- Que el agente SSM esté conectado.
- Que el rol de la instancia tenga permisos de SSM y acceso a ECR/SSM Parameter Store si los scripts lo necesitan.
- El resultado de `StandardOutputContent` y `StandardErrorContent` del comando.

### Falla en la comprobación MySQL

No se debe eliminar ni recrear el volumen. Primero hay que verificar el montaje de `/var/lib/aelb/mysql` y el estado de `aelb-pre-mysql-1`.

## Qué no hace este workflow

- No ejecuta migraciones destructivas de base de datos por sí mismo.
- No elimina el volumen persistente de MySQL.
- No despliega imágenes de otra rama si se ejecuta correctamente desde `pre`.
- No usa acceso SSH directo.
- No almacena claves permanentes de AWS en el repositorio.

## Archivos relacionados

- `.github/workflows/deploy-pre.yml`
- `scripts/build-pre.sh`
- `scripts/sync-pre-runtime.sh`
- `scripts/deploy-pre.sh`
- `docker-compose.pre.yml`
- `backendweb/src/test/resources/application.properties`
- `infra/environments/pre/terraform.tfvars`
