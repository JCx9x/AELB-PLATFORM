# Emails transaccionales

El backend utiliza una transactional outbox en MySQL. Los casos de uso crean el registro de negocio y `email_outbox` en la misma transacción; `EmailWorker` reclama lotes limitados, aplica el rate limit global, reintentos con backoff/jitter y recupera jobs `PROCESSING` abandonados.

## Local

```bash
cp .env.example .env
docker compose up -d
```

Mailpit queda disponible en [http://localhost:8025](http://localhost:8025) y SMTP en `localhost:1025` desde el host (`mailpit:1025` desde el contenedor backend). El profile local fuerza SMTP y el backend rechaza SES fuera de producción.

Flujos verificables: registro y verificación, `/forgot-password`, `/reset-password`, inscripción y webhook Stripe en test mode. Ninguno necesita credenciales AWS ni envía correos reales.

## Configuración de producción

Para producción no uses el Compose local. Utiliza `docker-compose.prod.yml`, que fuerza `SPRING_PROFILES_ACTIVE=prod` y `EMAIL_PROVIDER=ses`:

```bash
cp .env.prod.example .env.prod
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build
```

Variables relevantes:

```env
APP_ENV=prod
EMAIL_PROVIDER=ses
EMAIL_FROM=noreply@midominio.es
EMAIL_FROM_NAME=AELB
AWS_REGION=eu-west-1
EMAIL_MAX_PER_SECOND=2
EMAIL_WORKER_CONCURRENCY=1
EMAIL_BATCH_SIZE=10
EMAIL_MAX_RETRIES=5
EMAIL_WORKER_ENABLED=true
FRONTEND_PUBLIC_URL=https://midominio.es
EMAIL_TOKEN_ENCRYPTION_KEY=<secreto fuerte gestionado fuera del repositorio>
```

La aplicación usa la cadena de credenciales por defecto del SDK de AWS. En EC2 debe utilizarse un Instance Profile/IAM Role; no se necesitan `AWS_ACCESS_KEY_ID` ni `AWS_SECRET_ACCESS_KEY` en `.env`.

Permiso mínimo recomendado para el role:

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Action": ["ses:SendEmail", "ses:SendRawEmail"],
    "Resource": "arn:aws:ses:eu-west-1:123456789012:identity/midominio.es"
  }]
}
```

SES requiere verificar el dominio/remitente, habilitar DKIM, publicar SPF y DMARC, salir del sandbox y usar la región configurada. Mailpit no se incluye en producción.

## Operación del outbox

```sql
SELECT status, type, COUNT(*) FROM email_outbox GROUP BY status, type;
SELECT COUNT(*) FROM email_outbox WHERE status = 'PENDING';
SELECT id, type, attempts, next_attempt_at, last_error FROM email_outbox WHERE status = 'FAILED' ORDER BY created_at DESC;
```

Para detener envíos sin apagar el backend: `EMAIL_WORKER_ENABLED=false`. Los jobs pendientes permanecen en la base de datos. Los `SENT` se limpian después de la retención configurada (30 días por defecto); los `FAILED` se conservan para diagnóstico.

La limitación por IP actual es en memoria y válida para una instancia. Si se despliegan varias réplicas, mover los rate limits de endpoints a un almacén compartido/WAF; el claim del outbox ya utiliza lock pesimista para evitar doble procesamiento.
