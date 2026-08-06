# Arquitectura y despliegue

## Propósito

AELB es una aplicación de gestión de campeonatos de lucha de brazos. Tiene un frontend web, una API REST y servicios de datos. Esta guía describe el montaje actual y la decisión temporal de servir el frontend con Next.js en lugar de una exportación estática en Nginx.

## Componentes

```text
Navegador
    │
    ├── Frontend Next.js (puerto 3000)
    │       │
    │       └── API Spring Boot (puerto 8080)
    │               ├── MySQL 8 (datos de negocio)
    │               └── MinIO / S3 (imágenes y objetos)
    │
    └── MinIO Console (puerto 9001, desarrollo local)
```

| Componente | Tecnología | Responsabilidad |
|---|---|---|
| Frontend | Next.js 16, React 19, TypeScript | Interfaz pública, perfil y backoffice. |
| Backend | Spring Boot 4, Java 21 | API REST, autenticación JWT y reglas de negocio. |
| Base de datos | MySQL 8.4 | Usuarios, campeonatos, inscripciones, cuotas y contenidos. |
| Almacenamiento | MinIO en desarrollo / S3 en producción | Imágenes cargadas mediante URLs firmadas. |

La orquestación local está en [docker-compose.yml](../docker-compose.yml). Las variables necesarias están en `.env` y no deben incluirse en repositorios públicos.

## Frontend: situación actual

El frontend se ejecuta como servidor de Next.js dentro de su contenedor (`node server.js`) y se expone por el puerto `3000`.

Esta configuración usa `output: 'standalone'` en `aelb-frontend/next.config.ts`. El Dockerfile copia únicamente la salida standalone y los recursos estáticos necesarios, por lo que la imagen de ejecución no incluye el código fuente ni las dependencias de desarrollo.

La razón es la ruta dinámica de detalle de campeonato:

```text
/campeonatos/[id]
```

Los campeonatos se crean y modifican desde el backoffice. Sus identificadores no se conocen cuando se construye la imagen. Servir Next.js permite que esa ruta se resuelva bajo demanda y que un campeonato nuevo esté disponible sin reconstruir el frontend.

También se mantiene el enlace directo a un campeonato y el retorno al login con la misma URL, por ejemplo:

```text
/campeonatos/{id}?scroll=inscripcion
```

## Exportación estática y Nginx: planteamiento original

Inicialmente el frontend estaba configurado con `output: 'export'` y se servía desde Nginx. El resultado era un conjunto de HTML, JavaScript y CSS que podía alojarse sin servidor Node, por ejemplo en S3 + CloudFront o en un Nginx pequeño.

Es una opción muy buena para reducir costes en AWS cuando todas las URLs se conocen durante el build. Sin embargo, una exportación estática no puede generar rutas dinámicas de Next.js cuyos IDs vienen de la API después del despliegue, salvo que se reconstruya la web cada vez que cambian los campeonatos.

El archivo `aelb-frontend/nginx.conf` se conserva como referencia de ese planteamiento, pero no participa en el contenedor actual.

## Alternativas para volver a un frontend estático

Cuando se priorice el coste de infraestructura, hay dos caminos razonables:

1. Cambiar la ficha dinámica por una única página estática con parámetro de consulta, por ejemplo `/campeonato?id={id}`. La página lee el ID en cliente y obtiene los datos de la API. Así podría volver `output: 'export'` y Nginx/S3/CloudFront.
2. Mantener `/campeonatos/[id]`, pero publicar una nueva exportación estática cada vez que se crea, edita o elimina un campeonato. Es más complejo y deja de ser inmediato para el usuario.

La primera opción es la recomendada para un despliegue estático económico. Antes de hacer ese cambio conviene revisar todos los enlaces a campeonatos, el retorno a login y las reglas de Nginx/CloudFront para mantener compatibilidad con URLs existentes.

## Backend y seguridad

El backend aplica arquitectura por dominio, casos de uso e infraestructura:

```text
domain/          Entidades, value objects y puertos de repositorio
application/     Casos de uso
infrastructure/  JPA, controladores REST, JWT, almacenamiento S3/MinIO
```

La API utiliza JWT. El frontend lo envía como `Authorization: Bearer ...`. Las rutas públicas incluyen campeonatos visibles, noticias y resultados; las operaciones de gestión requieren los roles `GESTOR` o `ADMIN`.

## Base de datos y migraciones

Hibernate está configurado en modo `validate`: comprueba que la estructura de MySQL coincide con las entidades, pero no crea ni altera tablas automáticamente.

En una base nueva, MySQL ejecuta [database/schema.sql](../database/schema.sql) al inicializar su volumen. En una base que ya existe, los cambios deben aplicarse con una migración explícita. Por ejemplo, las cuotas anuales requieren ejecutar una vez:

```text
database/migrations/20260727_annual_quotas.sql
```

No se debe borrar el volumen de MySQL como mecanismo de migración, ya que eliminaría los datos existentes.

## Operación local

```bash
docker compose up --build
```

Servicios resultantes:

| Servicio | Dirección local |
|---|---|
| Frontend | http://localhost:3000 |
| Backend | http://localhost:8080 |
| MinIO API | http://localhost:9000 |
| MinIO Console | http://localhost:9001 |
| MySQL | localhost:3307 |

Para modificar la aplicación se recomienda reconstruir el servicio afectado:

```bash
docker compose build frontend
docker compose up -d frontend
```

o bien el backend:

```bash
docker compose build backend
docker compose up -d backend
```
