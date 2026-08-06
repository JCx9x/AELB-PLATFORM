# Stripe en producción: datos, configuración y procedimiento

## Objetivo

Esta guía explica cómo activar el cobro real de cuotas anuales con Stripe Checkout en AELB. El proceso de pago es:

```text
Usuario pulsa «Pagar cuota»
    → AELB crea una Checkout Session en Stripe
    → el navegador se redirige a Stripe
    → Stripe cobra el pago
    → Stripe envía un webhook firmado a AELB
    → AELB verifica la firma y guarda la cuota como PAID en MySQL
```

El webhook, y no el regreso del usuario a la web, es la fuente de verdad del pago.

## Información que debes preparar

Antes del despliegue reúne estos datos:

| Dato | Ejemplo | Dónde se obtiene |
|---|---|---|
| Dominio público de la plataforma | `https://app.aelb.es` | DNS / infraestructura AWS. |
| URL pública de la API | `https://api.aelb.es` o `https://app.aelb.es/api` | Infraestructura AWS / proxy. |
| Clave secreta live | `sk_live_...` | Stripe Dashboard → Developers → API keys. |
| Secreto de firma del webhook | `whsec_...` | Stripe Dashboard → Developers → Webhooks, después de crear el endpoint. |
| Moneda | `eur` | Decisión de negocio; la aplicación usa EUR por defecto. |
| Cuenta bancaria y datos legales | — | Stripe Dashboard → Settings / Payments. |

No se necesita clave pública (`pk_live_...`) para la integración actual. Se usa Stripe Checkout alojado por Stripe, por lo que el navegador recibe solo una URL de checkout. La clave secreta nunca debe llegar al frontend.

## Configuración de Stripe

### 1. Activar pagos reales

1. Completa la activación de la cuenta Stripe, incluyendo la información fiscal, legal y bancaria solicitada.
2. Cambia el Dashboard de **modo de pruebas** a **modo live**.
3. En **Developers → API keys**, revela y copia la clave secreta `sk_live_...`.

No copies esta clave en documentación, repositorios Git, tickets ni frontend.

### 2. Crear el endpoint de webhook

En **Developers → Webhooks**, crea un endpoint para:

```text
https://API_PUBLICA/api/webhooks/stripe
```

Ejemplos válidos:

```text
https://api.aelb.es/api/webhooks/stripe
https://app.aelb.es/api/webhooks/stripe
```

Selecciona al menos estos eventos:

```text
checkout.session.completed
checkout.session.async_payment_succeeded
```

Una vez creado, abre el endpoint y copia su **Signing secret** (`whsec_...`). Este secreto es distinto de la clave API `sk_live_...`.

Stripe recomienda usar el webhook `checkout.session.completed` para completar el proceso de negocio después del pago. Consulta la [guía oficial de Checkout](https://docs.stripe.com/payments/checkout/how-checkout-works) y la [guía de webhooks](https://docs.stripe.com/webhooks).

## Configuración de la plataforma

### Variables de entorno

Configura estos valores en el gestor de secretos de producción. Si se usa Docker Compose temporalmente, pueden estar en el fichero `.env` del servidor, que no debe subirse a Git.

```env
STRIPE_SECRET_KEY=sk_live_...
STRIPE_WEBHOOK_SECRET=whsec_...
STRIPE_CURRENCY=eur

STRIPE_SUCCESS_URL=https://app.aelb.es/perfil?stripe=success&session_id={CHECKOUT_SESSION_ID}
STRIPE_CANCEL_URL=https://app.aelb.es/perfil?stripe=cancelled
```

`{CHECKOUT_SESSION_ID}` debe conservarse literalmente: Stripe lo sustituye al redirigir al usuario. La URL de éxito sirve para informar al usuario; no debe marcar el pago como realizado.

### AWS recomendado

En producción, guarda `STRIPE_SECRET_KEY` y `STRIPE_WEBHOOK_SECRET` en AWS Secrets Manager o Parameter Store con cifrado KMS. Otorga al servicio backend únicamente permiso de lectura para esos dos secretos.

No añadas ninguna clave secreta como:

- `NEXT_PUBLIC_*`.
- Variable de entorno del frontend.
- Valor hardcodeado en JavaScript o Java.
- Archivo incluido en la imagen Docker.

Tras actualizar los secretos, despliega o reinicia el contenedor backend para que cargue los nuevos valores.

## Requisitos de red

1. La URL de webhook debe ser pública y accesible por HTTPS desde Internet.
2. El proxy, balanceador o WAF debe permitir `POST /api/webhooks/stripe` sin JWT.
3. No transformes ni reconstruyas el cuerpo de la petición antes de que llegue al backend: la firma de Stripe se verifica contra el cuerpo bruto.
4. El backend debe poder realizar conexiones HTTPS salientes a `https://api.stripe.com` para crear las Checkout Sessions.
5. Configura certificados TLS válidos para el dominio público.

## Preparación funcional en AELB

Antes de abrir pagos reales:

1. Entra como `GESTOR` o `ADMIN` en **Gestión → Cuotas anuales**.
2. Configura los siete precios del año vigente.
3. Verifica que los usuarios tienen fecha de nacimiento. Sin ella no se puede determinar el tramo de cuota.
4. Comprueba que el usuario ve en Perfil su cuota y el botón **Pagar cuota**.

Al pulsar el botón, AELB crea una sesión Stripe con el importe de la cuota ya persistida. La cuota conserva el tramo y precio que tenía cuando fue generada, aunque posteriormente cambien las tarifas anuales.

## Confirmación y persistencia del pago

El endpoint de AELB recibe el evento firmado en:

```text
POST /api/webhooks/stripe
```

La aplicación realiza estas comprobaciones:

1. Valida `Stripe-Signature` con `STRIPE_WEBHOOK_SECRET`.
2. Acepta solo eventos de pago completado.
3. Lee el usuario y año de los metadatos internos enviados al crear la sesión.
4. Busca la cuota correspondiente en MySQL.
5. Cambia `quotas.payment_status` de `PENDING` a `PAID` y registra `payment_date`.

El manejo es idempotente: si Stripe reintenta el mismo evento, una cuota ya pagada permanece pagada sin duplicar cobros ni registros.

## Plan de pruebas antes de producción

Realiza las pruebas en modo test antes de usar claves `sk_live_...`:

1. Configura `sk_test_...` y el `whsec_...` de pruebas.
2. Reenvía eventos a local mediante Stripe CLI:

   ```bash
   stripe login
   stripe listen --forward-to localhost:8080/api/webhooks/stripe
   ```

3. Configura una cuota de prueba y realiza un pago con una tarjeta de prueba de Stripe.
4. Comprueba en Stripe Dashboard que la sesión está pagada.
5. Comprueba en AELB que el perfil muestra **Pagada**.
6. Comprueba en MySQL que la fila en `quotas` tiene `payment_status = 'PAID'` y `payment_date` informada.
7. Reenvía el mismo evento desde Stripe Dashboard y verifica que no se modifica ni duplica la cuota.
8. Prueba cancelar el checkout y confirma que la cuota sigue `PENDING`.

Las tarjetas de prueba y el procedimiento actualizado están en la [documentación oficial de pruebas de Stripe](https://docs.stripe.com/testing).

## Lista de salida a producción

- [ ] Precios anuales configurados y revisados.
- [ ] Usuarios con fecha de nacimiento válida.
- [ ] Dominio y HTTPS operativos.
- [ ] Endpoint de webhook creado en modo live.
- [ ] Eventos `checkout.session.completed` y `checkout.session.async_payment_succeeded` seleccionados.
- [ ] `sk_live_...` y `whsec_...` guardados en Secrets Manager o equivalente.
- [ ] Variables de URL de éxito y cancelación apuntando al dominio real.
- [ ] Backend desplegado con los secretos live.
- [ ] Se ha realizado un pago real controlado de importe mínimo o una prueba final autorizada por el responsable financiero.
- [ ] Se han comprobado Dashboard de Stripe, Perfil AELB y registro MySQL.

## Operación y soporte

### Un usuario dice que ha pagado pero su cuota sigue pendiente

1. Busca el pago o Checkout Session en Stripe Dashboard usando el email y la hora aproximada.
2. Comprueba que Stripe entregó el webhook al endpoint y que la respuesta fue HTTP 200.
3. Revisa los logs del backend para errores de firma, URL o base de datos.
4. Si el pago está confirmado en Stripe, usa la opción de reenvío del evento desde Stripe Dashboard. El proceso es seguro por idempotencia.
5. Como medida excepcional, un gestor/admin puede marcar la cuota como pagada desde la API administrativa, dejando constancia operativa de por qué se hizo manualmente.

### Rotación de secretos

1. Crea o revela la nueva clave/secreto en Stripe.
2. Actualiza el secreto en AWS Secrets Manager.
3. Despliega o reinicia el backend.
4. Verifica un pago de prueba o la recepción de un evento.
5. Revoca la clave anterior solo cuando se haya confirmado el funcionamiento.

## Límites actuales y mejoras futuras

- La aplicación persiste el estado y fecha de pago de la cuota. Si se requiere conciliación financiera más detallada, se recomienda almacenar también el ID de Checkout Session, Payment Intent, importe y evento de Stripe en una tabla de pagos.
- El filtro de usuarios permite ver quién ha pagado la cuota vigente.
- Las devoluciones, facturas, impuestos y suscripciones no forman parte de esta primera integración; deben diseñarse antes de activarlas.
# Stripe para inscripciones a campeonatos

Además de las cuotas anuales, las inscripciones pendientes pueden pagar con Stripe Checkout.

1. El usuario crea la inscripción; queda en `PENDING` con su importe calculado.
2. `POST /api/registrations/{registrationId}/checkout` crea una sesión Stripe hospedada.
3. El navegador se redirige a `checkoutUrl`.
4. Stripe llama a `POST /api/webhooks/stripe` con un evento firmado.
5. El backend verifica la firma, registra la referencia en `registration_checkouts` y marca la inscripción como `PAID`.

No se debe marcar una inscripción como pagada desde la URL de éxito ni desde el navegador. El endpoint de webhook y los secretos son los mismos que para las cuotas: `STRIPE_SECRET_KEY` y `STRIPE_WEBHOOK_SECRET`.
