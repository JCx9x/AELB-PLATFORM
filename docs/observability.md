# Observabilidad de producción

La aplicación utiliza una capa deliberadamente pequeña:

```text
HTTP -> CorrelationIdFilter/MDC -> logs de aplicación
                         |-> Actuator health/info/metrics
                         |-> Micrometer
stdout del contenedor -> CloudWatch Logs -> filtros y alarmas
```

No se envían logs desde el código a CloudWatch. El contenedor escribe en stdout y el runtime AWS (ECS/Fargate, EC2 con agente o el mecanismo equivalente) debe recogerlo.

## Diagnóstico local

Cada petición responde con `X-Correlation-ID`. Si el cliente no lo proporciona, el backend genera un UUID. El valor se incorpora al MDC y aparece en los logs. Solo se aceptan identificadores alfanuméricos, `.`, `_` y `-`, con un máximo de 64 caracteres.

No se registran cuerpos HTTP, `Authorization`, cookies, contraseñas, tokens, DNI, documentos ni payloads completos de Stripe.

Ejemplos de eventos buscables:

```text
event=HTTP_REQUEST_COMPLETED method=POST path=/api/... status=201 durationMs=84
event=STRIPE_WEBHOOK_RECEIVED stripeEventId=evt_... eventType=checkout.session.completed
event=STRIPE_WEBHOOK_PROCESSED stripeEventId=evt_... stripeSessionId=cs_...
event=STRIPE_WEBHOOK_FAILED exceptionType=...
event=REGISTRATION_CREATED registrationId=... championshipId=...
event=PAYMENT_CONFIRMED paymentId=... stripeSessionId=...
event=EMAIL_SENT emailJobId=... type=...
```

Los identificadores técnicos se mantienen en logs, no en tags de métricas.

## Actuator

Endpoints expuestos:

* `GET /actuator/health/liveness`: solo estado del proceso/JVM; no depende de MySQL, Stripe ni S3.
* `GET /actuator/health/readiness`: incluye el estado de la conexión MySQL; una instancia no preparada no debe recibir tráfico.
* `GET /actuator/health`: resumen sin detalles.
* `GET /actuator/info`: versión, commit y entorno no sensibles.
* `GET /actuator/metrics/**`: solo usuarios `GESTOR`/`ADMIN` autenticados.

La caída de Stripe/S3 no marca liveness ni readiness como DOWN porque el backend todavía puede servir operaciones que no dependen de ellos. Los endpoints de Actuator no incluidos se deniegan.

## Métricas

Spring Boot/Micrometer proporciona las métricas HTTP/JVM/pool de conexiones. La aplicación añade únicamente:

* `aelb.stripe.webhooks{result}`: `signature_invalid`, `unsupported`, `not_paid`, `invalid_metadata`, `processed`, `failed`.
* `aelb.payments{result}`: confirmados y repetidos.
* `aelb.emails{result}`: `sent`, `retried`, `failed`.
* `aelb.email.outbox.pending` y `aelb.email.outbox.failed`.

No se usan `userId`, `championshipId`, `stripeSessionId` ni `correlationId` como tags.

## CloudWatch

Crear un único grupo por entorno, por ejemplo `/aelb/prod/backend`, con retención inicial de 30 días. Mantener el contenedor en stdout y configurar el runtime para recogerlo; no hace falta el SDK de CloudWatch en la aplicación.

Consultas de CloudWatch Logs Insights (para logs key/value):

```sql
fields @timestamp, @message
| filter @message like /correlationId=01H/
| sort @timestamp asc
```

```sql
fields @timestamp, @message
| filter @message like /event=STRIPE_WEBHOOK_FAILED/
| sort @timestamp desc
```

```sql
fields @timestamp, @message
| filter @message like /event=HTTP_REQUEST_COMPLETED/ and @message like /status=5/
| sort @timestamp desc
```

```sql
fields @timestamp, @message
| filter @message like /event=REGISTRATION_FAILED/ or @message like /event=STRIPE_WEBHOOK_FAILED/
| stats count() by bin(5m)
```

## Alarmas iniciales

Crear las alarmas fuera de la aplicación, preferentemente con CloudWatch + SNS y una suscripción de email para administradores. Estos valores son un punto de partida para el volumen actual:

| Alarma | Condición | Ventana | Severidad | Acción |
| --- | --- | --- | --- | --- |
| BackendUnavailable | target group sin targets healthy o readiness DOWN | 2 periodos de 1 min | CRITICAL | Revisar despliegue, logs y DB |
| High5xxRate | `HTTPCode_Target_5XX_Count >= 5` y al menos 10 requests | 5 min | HIGH | Buscar correlation IDs y dependencia fallida |
| StripeWebhookFailure | filtro `event=STRIPE_WEBHOOK_FAILED` >= 1 | 5 min | HIGH | Revisar evento, sesión y reintento Stripe |
| RegistrationFailureSpike | filtro `event=REGISTRATION_FAILED` >= 3 | 10 min | HIGH | Revisar disponibilidad, validación y DB |
| EmailOutboxFailure | `aelb.email.outbox.failed >= 3` o varios `EMAIL_FAILED` | 15 min | MEDIUM/HIGH | Detener worker si hay loop y revisar outbox |
| EmailOutboxBacklog | `aelb.email.outbox.pending > 100` | 15 min | MEDIUM | Inspeccionar worker, SES/SMTP y rate limit |

No crear alarmas para 400/401/403/404 aislados ni para cada error de validación. Los thresholds deben configurarse en IaC o consola CloudWatch, no hardcodearse en Java.

## Runbook

### “Me han cobrado pero no estoy inscrito”

1. Buscar el usuario y el campeonato en la base de datos.
2. Buscar `PAYMENT_CONFIRMED`, `PAYMENT_ALREADY_CONFIRMED` y `stripeSessionId`.
3. Buscar `STRIPE_WEBHOOK_RECEIVED` usando `stripeEventId`.
4. Comprobar si existe `STRIPE_WEBHOOK_FAILED` para esa ventana.
5. Revisar el estado del checkout/payment y la inscripción.
6. Si hay error de proveedor, comprobar el webhook y sus reintentos en Stripe.

### “Me da error al inscribirme”

Pedir el `X-Correlation-ID` de la respuesta, buscarlo en CloudWatch y seguir `HTTP_REQUEST_COMPLETED`, la excepción y el `championshipId`/`registrationId` técnico.

### Backend caído

Comprobar target group/readiness, logs de arranque, `GET /actuator/health/liveness`, estado de MySQL y último commit de `GET /actuator/info`.

### Email outbox creciendo

Desactivar `EMAIL_WORKER_ENABLED=false` sin apagar el backend, contar `PENDING`/`FAILED` en la tabla, revisar `EMAIL_FAILED`/`EMAIL_RETRIED` y reactivar solo después de identificar la causa.

## Entornos y limitaciones

Local y test no necesitan AWS ni CloudWatch. Local usa logs legibles y Mailpit; los tests usan fakes. Producción usa `INFO`, stdout y SES para el sistema transaccional de email.

Los errores JavaScript puramente del navegador no se capturan de forma global sin una herramienta cliente como Sentry/RUM. Los errores de servidor de Next.js deben recogerse desde stdout del runtime. Para el tamaño actual no se introduce una plataforma adicional.

## AWS

Configurar manualmente:

1. Log group `/aelb/prod/backend` y retención de 30 días.
2. Recolección de stdout del runtime.
3. Métricas HTTP del ALB y filtros de métricas para los eventos críticos si no se exportan las métricas Micrometer.
4. Alarmas anteriores.
5. Topic SNS y suscripción de email confirmada.
6. `APP_VERSION` y `GIT_COMMIT` durante el build/deploy.

No se requieren credenciales AWS en la aplicación para observabilidad. El mismo principio de IAM role usado por SES/S3 debe mantenerse.
