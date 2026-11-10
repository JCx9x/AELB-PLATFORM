# Runbook de observabilidad — PRE

Documento operativo del estado **realmente implementado** en la rama `pre-observability` (Fase 1: centralización de logs en CloudWatch + logging de autenticación, Stripe, SES y rate limiting). `docs/observability.md` describe el diseño pensado para un futuro entorno de producción con ALB/target groups; este documento es específico de PRE (una única instancia EC2, Docker Compose, sin balanceador).

## 0. Qué existe hoy

- 4 CloudWatch Log Groups, uno por contenedor, retención 14 días (`var.log_retention_days`, con validación de los valores que acepta CloudWatch):
  `/aelb/pre/caddy`, `/aelb/pre/mysql`, `/aelb/pre/backend`, `/aelb/pre/frontend`.
- Los 4 contenedores de `docker-compose.pre.yml` usan el driver `awslogs` en `mode: non-blocking` (un fallo de CloudWatch nunca bloquea el contenedor).
- El rol `aelb-pre-ec2` tiene dos statements IAM, uno por tipo de recurso real (ver §7).
- Backend: logging de eventos de autenticación (`AUTH_TOKEN_REJECTED`, `AUTH_REQUIRED`, `AUTHZ_DENIED`, `AUTH_FAILED`), de Stripe (`STRIPE_CHECKOUT_*`), de SES (`SES_SEND_FAILED`) y de rate limiting (`RATE_LIMIT_EXCEEDED`).
- Caddy: logging de acceso en JSON a stdout (cabeceras `Cookie`/`Authorization` redactadas automáticamente por Caddy).
- `server.forward-headers-strategy=framework` en el backend: `request.getRemoteAddr()` devuelve la IP real del cliente (ver §3), no la IP interna de Caddy.

## 1. Localizar logs del backend en CloudWatch

```bash
aws logs tail /aelb/pre/backend --since 30m --follow --region eu-south-2
```

Consola: CloudWatch → Log groups → `/aelb/pre/backend` → el stream más reciente es `backend-<container-id>` (uno nuevo por cada arranque del contenedor, no por petición).

## 2. Consultar eventos por `correlationId`

El patrón de log (`application.properties`, `logging.pattern.console`) añade `correlationId=%X{correlationId:-}` a **toda** línea emitida en el hilo de una petición HTTP — no hace falta que el código la mencione explícitamente.

```sql
fields @timestamp, @message
| filter @message like /correlationId=<ID>/
| sort @timestamp asc
```

**Límite conocido:** el `correlationId` de la petición original **no llega** al procesamiento asíncrono de `EmailWorker` (corre en el hilo del `@Scheduled`, sin MDC). Para seguir un email, usa `emailJobId` (ver §5) en vez de `correlationId`.

## 3. Investigar un fallo de autenticación

```sql
fields @timestamp, @message
| filter @message like /event=AUTH_/
| sort @timestamp desc
```

Motivos posibles y su significado exacto:

| `reason` | Dónde se emite | Significado |
|---|---|---|
| `TOKEN_EXPIRED` | `JwtAuthenticationFilter` | El JWT caducó (normal tras ~15 min de inactividad; el `Max-Age` de la cookie coincide con la expiración del JWT, así que el navegador deja de enviarla casi al mismo tiempo — esto **no** debería generar ruido constante) |
| `TOKEN_INVALID` | `JwtAuthenticationFilter` | Firma inválida o token manipulado. Si aparece en ráfaga para muchos usuarios a la vez, lo más probable es una rotación de `JWT_SECRET` |
| `SESSION_REVOKED` | `JwtAuthenticationFilter` | La versión de sesión en BD cambió (bloqueo, cambio de rol o de contraseña) después de emitirse el token |
| `USER_NOT_FOUND` | `JwtAuthenticationFilter` | El usuario del token ya no existe en BD |
| `USER_BLOCKED` | `JwtAuthenticationFilter` + `GlobalExceptionHandler` | Cuenta bloqueada — al validar una sesión existente o al intentar iniciar sesión |
| `INVALID_CREDENTIALS` | `GlobalExceptionHandler` | Contraseña incorrecta en `/api/auth/login` (sin email en el log; cuenta los eventos, no los identifiques por usuario desde aquí — para eso, ver `RATE_LIMIT_EXCEEDED` por IP) |
| `REFRESH_TOKEN_REUSED` | `GlobalExceptionHandler` | **Señal de seguridad fuerte**: alguien reutilizó un refresh token ya rotado — posible robo de sesión. Todas las sesiones del usuario quedan revocadas |
| (sin reason, `event=AUTH_REQUIRED`/`AUTHZ_DENIED`) | `SecurityConfig` | Resultado final 401/403 de la petición — llega acompañado de `path=`/`method=` |

Para un intento de login controlado (prueba de aceptación D.6 de la auditoría): hacer login con contraseña incorrecta desde fuera y comprobar que aparece exactamente `event=AUTH_FAILED reason=INVALID_CREDENTIALS`, y que un `GET` anónimo a una ruta pública **no** genera ninguna línea `AUTH_*`.

## 4. Investigar un error de Stripe

```sql
fields @timestamp, @message
| filter @message like /event=STRIPE_CHECKOUT/
| sort @timestamp desc
```

Cada línea incluye `idempotencyKey` (p. ej. `registration-checkout-<id>` o `annual-quota-<id>`) — es el identificador que relaciona el log con la operación de negocio concreta sin tocar la base de datos. También incluye `attempt` (1-indexado), `statusCode`, `detail` (código/tipo/mensaje de error de Stripe, nunca la cabecera `Authorization`) y `retryable` (si ese fallo concreto se reintentaría o no).

```sql
fields @timestamp, @message
| filter @message like /event=STRIPE_CIRCUIT_OPEN/
| sort @timestamp desc
```

`STRIPE_CIRCUIT_OPEN` solo aparece en la transición cerrado→abierto (no se repite en cada fallo mientras el circuito ya está abierto) — cada aparición es un incidente nuevo, no ruido repetido.

## 5. Investigar un correo no enviado

```sql
fields @timestamp, @message
| filter @message like /emailJobId=<ID>/
| sort @timestamp asc
```

Secuencia esperada: `EMAIL_SENT` (éxito) **o** una o más `EMAIL_RETRIED` seguidas de `EMAIL_FAILED` (agotados los reintentos). Desde esta fase, ambas incluyen `error=` con el mensaje de la excepción.

**Límite conocido (no corregido en esta fase, requiere cambiar la interfaz `EmailSender`):** `SES_SEND_FAILED` (emitido en `SesEmailSender`, con `statusCode`/`errorCode`/`requestId` de AWS) **no** incluye `emailJobId` — no aparece en la búsqueda anterior. Si `EMAIL_FAILED` no basta para diagnosticar (p. ej. necesitas el `requestId` de AWS para abrir un caso de soporte), busca por franja horaria en el mismo log group:

```sql
fields @timestamp, @message
| filter @message like /event=SES_SEND_FAILED/
| sort @timestamp desc
```

## 6. Diagnosticar un backend que no arranca / en bucle de reinicio

1. `docker logs aelb-pre-backend-1` (SSM) — **sigue funcionando**: Docker activa automáticamente *dual logging* cuando el driver configurado no soporta lectura local (es el caso de `awslogs`), y mantiene una caché local de hasta 5 archivos × 20 MB por contenedor salvo que se desactive explícitamente con `cache-disabled` (no se ha desactivado aquí).
2. Si lo anterior no es suficiente o el contenedor ya no existe: `/aelb/pre/backend` en CloudWatch, buscando `event=APPLICATION_READY` (arranque correcto) o la ausencia de cualquier línea tras el intento de arranque.
3. **Riesgo a tener en cuenta:** en un *crash* muy rápido tras arrancar, el envío asíncrono a CloudWatch (`mode: non-blocking`) podría no llegar a completarse antes de que el proceso del contenedor termine. La caché local de dual-logging (punto 1) es la vía más fiable en ese caso concreto porque no depende de la entrega a AWS.
4. Si ni `docker logs` ni CloudWatch tienen nada (p. ej. el contenedor nunca llegó a iniciar Docker): `/var/log/aelb-bootstrap.log` y `journalctl -u docker --since "15 min ago"` en el host, vía SSM — no dependen del driver `awslogs` en absoluto.

## 7. Consultar logs del host vía SSM (sin SSH)

```bash
aws ssm start-session --target <instance-id> --region eu-south-2
# dentro de la sesión:
docker ps -a
docker logs --tail 200 aelb-pre-backend-1
journalctl -u docker --since "15 min ago"
cat /var/log/aelb-bootstrap.log
```

## 8. Detectar problemas del driver `awslogs`

No hay alarma automática para esto en esta fase (deliberado: priorizar que el backend nunca se bloquee por CloudWatch, antes que alertar sobre ello). Para comprobarlo a mano:

```bash
journalctl -u docker --since "10 min ago" | grep -i awslogs
```

Si el rol IAM no tuviera permiso (p. ej. tras revertir la corrección de H-01 por error), aquí aparecería `AccessDenied` repetido — los contenedores seguirían `healthy` igualmente, por eso esta comprobación debe hacerse de forma proactiva, no solo cuando algo "se nota" roto.

## 9. Verificar permisos y retención

```bash
aws iam get-role-policy --role-name aelb-pre-ec2 --policy-name aelb-pre-app-access \
  --query 'PolicyDocument.Statement[?Sid==`AppLogsCreateStream` || Sid==`AppLogsPutEvents`]'

aws logs describe-log-groups --log-group-name-prefix /aelb/pre/ \
  --query 'logGroups[].{name:logGroupName,retention:retentionInDays}'
```

Esperado: `AppLogsCreateStream` con `Resource` = 4 ARN de log-group sin sufijo; `AppLogsPutEvents` con `Resource` = los mismos 4 ARN **con** `:log-stream:*` al final; retención = 14 en los 4 grupos.

## 10. Validar la centralización de logs tras el despliegue

```bash
aws logs tail /aelb/pre/backend --since 5m
aws logs tail /aelb/pre/caddy --since 5m
```

Si ambos devuelven líneas recientes, la Fase 1 está funcionando de extremo a extremo. Si `describe-log-groups` muestra los grupos pero `tail` no devuelve nada tras varios minutos de tráfico real, sospechar primero de IAM (§9) antes de nada del lado de la aplicación.

## 11. Coste y volumen

```bash
aws cloudwatch get-metric-statistics --namespace AWS/Logs --metric-name IncomingBytes \
  --dimensions Name=LogGroupName,Value=/aelb/pre/backend \
  --start-time $(date -u -d '2 days ago' +%Y-%m-%dT%H:%M:%S) --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
  --period 86400 --statistics Sum --region eu-south-2
```

Estimación previa (ver auditoría): tráfico actual, retención 14 días → por debajo de unos pocos USD/mes, dentro del presupuesto de 20 USD/mes ya configurado (`aws_budgets_budget.monthly`).
