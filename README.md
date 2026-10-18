# CH&D RENTAL PRO — Servicios Web REST

Proyecto de investigación aplicada del curso **Desarrollo de Servicios Web II (4698)**, CIBERTEC, sexto ciclo, Grupo 10.

Es la API REST para la gestión de alquiler de camionetas 4x4 de **GRUPO CH&D CONTRATISTAS S.A.C.**: clientes, flota, contratos, pagos, mantenimientos y usuarios.
Esta versión 2.0 reemplaza la aplicación JSP/Servlets + MySQL del ciclo anterior por **Spring Boot 3, Spring Data JPA, Lombok, Spring MVC y Spring Security**, con **PostgreSQL** y despliegue en la nube.

| Requisito del curso | Implementación |
|---|---|
| Servicio REST de login con usuario y password en BD, password cifrado con `BCryptPasswordEncoder` | `POST /api/auth/login` → valida contra la tabla `usuarios` (hash `$2a$10$…`) y devuelve un token JWT |
| Servicios REST GET, POST, PUT y DELETE con datos en BD | 6 recursos × 5 operaciones (`/api/clientes`, `/vehiculos`, `/contratos`, `/pagos`, `/mantenimientos`, `/usuarios`) |
| Exponer la aplicación en la nube con PostgreSQL | `Procfile` + `system.properties` + `app.json` (Heroku) · `Dockerfile` + `render.yaml` (Render) |
| Pruebas de la capa de acceso a datos (insertar, actualizar, eliminar, listar) | 26 pruebas `@DataJpaTest` + 14 pruebas de integración de los servicios + 2 unitarias = **42 pruebas en verde** |

## Tecnologías

Java 17 · Spring Boot 3.3.5 · Spring Web (MVC) · Spring Data JPA (Hibernate 6) · Spring Security 6 + JWT (jjwt 0.12) · BCrypt · Bean Validation · Lombok · springdoc-openapi (Swagger UI) · PostgreSQL 16 · H2 (pruebas) · JUnit 5 + MockMvc · Maven · Docker.

## Arquitectura

```
Cliente REST (Swagger / Postman / Front)
        │  JSON + Authorization: Bearer <JWT>
        ▼
 JwtAuthenticationFilter ─► SecurityConfig (roles)        [security]
        ▼
 @RestController  (Auth, Cliente, Vehiculo, Contrato, Pago, Mantenimiento, Usuario)   [controller]
        ▼  DTO validados (@Valid)                                                    [dto]
 @Service  (reglas de negocio, @Transactional)                                       [service]
        ▼
 JpaRepository (Spring Data)                                                          [repository]
        ▼
 @Entity con Lombok  ──►  PostgreSQL                                                  [entity]
 GlobalExceptionHandler → respuestas de error JSON homogéneas                         [exception]
```

Patrones y buenas prácticas: arquitectura por capas, inyección de dependencias por constructor (`@RequiredArgsConstructor`), *Repository*, *DTO*, *Service* con interfaz e implementación, *Builder* (Lombok), manejo centralizado de excepciones, API sin estado (JWT), principio de mínimo privilegio por rol, configuración externa con variables de entorno (12-factor).

## Servicios

| Método | Ruta | Descripción | Rol |
|---|---|---|---|
| POST | `/api/auth/login` | Login → JWT | público |
| GET | `/api/auth/me` | Usuario autenticado | autenticado |
| GET | `/api/{recurso}` · `/api/{recurso}/{id}` | Listar / obtener | autenticado |
| POST | `/api/{recurso}` | Registrar (201 + `Location`) | ADMIN, OPERADOR, PAGOS* |
| PUT | `/api/{recurso}/{id}` | Actualizar | ADMIN, OPERADOR, PAGOS* |
| DELETE | `/api/{recurso}/{id}` | Eliminar (204) | ADMIN |

`{recurso}`: `clientes`, `vehiculos`, `contratos`, `pagos`, `mantenimientos`, `usuarios` (este último solo ADMIN).
\* En `/api/pagos` solo escriben ADMIN y PAGOS. GERENCIA tiene acceso de solo lectura.

Filtros: `GET /api/clientes?estado=ACTIVO&buscar=minera`, `GET /api/vehiculos?estado=DISPONIBLE`, `GET /api/contratos?estado=ACTIVO`, `GET /api/pagos?idContrato=1`, `GET /api/mantenimientos?idVehiculo=1`.

Reglas de negocio principales:
- Contrato: el servidor calcula días, subtotal, IGV (18 %) y total con la tarifa del vehículo. Rechaza fechas cruzadas con otro contrato vigente del mismo vehículo (409), clientes inactivos y vehículos en mantenimiento. El vehículo pasa a `ALQUILADO` y vuelve a `DISPONIBLE` al finalizar o cancelar el contrato; al finalizar también se actualiza el kilometraje.
- Vehículo: placa, marca y modelo no se modifican después del registro. El kilometraje no puede disminuir.
- Mantenimiento: si está `PROGRAMADO` o `EN_PROCESO`, el vehículo pasa a `MANTENIMIENTO`; al `COMPLETADO` se libera y se actualiza el kilometraje.
- No se eliminan registros con historial (cliente con contratos, contrato con pagos, etc.); en esos casos se cambia el estado.
- Errores: 400 validación (detalle por campo), 401 sin token o credenciales incorrectas, 403 rol sin permiso, 404 no existe, 409 regla de negocio o duplicado.

## Ejecución local

Requisitos: JDK 17 y Maven 3.9 (o Docker).

```bash
# Opción 1: todo con Docker (PostgreSQL + API)
docker compose up -d --build
# http://localhost:8080/swagger-ui.html

# Opción 2: PostgreSQL propio (BD chd_rental) + Maven
mvn spring-boot:run

# Opción 3: sin PostgreSQL (H2 en memoria)
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

Usuarios iniciales (se crean con BCrypt si la tabla está vacía):

| Email | Contraseña | Rol |
|---|---|---|
| admin@chdrental.com | `Admin123*` (o la variable `ADMIN_PASSWORD`) | ADMIN |
| operador@chdrental.com | `Chd12345*` | OPERADOR |
| pagos@chdrental.com | `Chd12345*` | PAGOS |
| gerencia@chdrental.com | `Chd12345*` | GERENCIA |

Para probar los servicios: abre `/swagger-ui.html`, ejecuta `POST /api/auth/login`, copia el `token`, presiona **Authorize** y ejecuta el resto. También puedes importar `docs/postman/CHD-Rental-Pro-API.postman_collection.json`; el login guarda el token automáticamente.

## Pruebas

```bash
mvn test
```

- `repository/*RepositoryTest` (`@DataJpaTest`): insertar, actualizar, eliminar y listar para Cliente, Vehículo, Usuario, Contrato, Pago y Mantenimiento; restricción UNIQUE; consulta de cruce de fechas.
- `controller/AuthControllerTest`: login correcto/incorrecto, validaciones, password cifrado, acceso sin token o con token inválido.
- `controller/ApiIntegrationTest`: flujo GET/POST/PUT/DELETE de punta a punta, reglas de contrato, pagos, mantenimientos y permisos por rol.

## Despliegue en la nube

Variables de entorno:

| Variable | Uso |
|---|---|
| `JDBC_DATABASE_URL`, `JDBC_DATABASE_USERNAME`, `JDBC_DATABASE_PASSWORD` | Conexión JDBC (Heroku las define automáticamente) |
| `DATABASE_URL` | Alternativa `postgres://user:pass@host:5432/db` (Render, Railway, Neon) |
| `JWT_SECRET` | Clave para firmar los JWT (obligatoria en producción) |
| `ADMIN_PASSWORD` | Contraseña inicial de `admin@chdrental.com` |
| `PORT` | Puerto HTTP (lo asigna la plataforma) |

### Heroku (recomendado por el curso)

```bash
heroku login
heroku create chd-rental-api-g10
heroku addons:create heroku-postgresql:essential-0
heroku config:set JWT_SECRET=$(openssl rand -base64 48) ADMIN_PASSWORD=<clave-segura>
git push heroku main
heroku open /swagger-ui.html
```

El buildpack de Java compila con Maven (`system.properties` → Java 17) y arranca con el `Procfile`.

### Render (alternativa con plan gratuito)

1. En Render: **New → Blueprint**, selecciona este repositorio. `render.yaml` crea la base PostgreSQL y el Web Service Docker.
2. Ingresa `ADMIN_PASSWORD` cuando lo solicite. `DATABASE_URL` y `JWT_SECRET` se configuran solos.
3. Verifica `https://<servicio>.onrender.com/actuator/health` → `{"status":"UP"}`.

## Estructura

```
src/main/java/com/chd/rental
├── config/       OpenAPI, datos iniciales, DATABASE_URL, registro de filtros
├── controller/   7 controladores REST
├── dto/          Request/Response con Bean Validation + Lombok
├── entity/       6 entidades JPA + enums de estado
├── exception/    GlobalExceptionHandler y excepciones de negocio
├── repository/   Spring Data JPA
├── security/     SecurityConfig, JWT, UserDetailsService (BCrypt)
└── service/      interfaces + impl (reglas de negocio, transacciones)
database/         script PostgreSQL reproducible con datos de ejemplo
docs/             informe, colección Postman y evidencias
```

## Integrantes — Grupo 10

- Barreda Puma Fernando (coordinador)
- Mesta Wong Jennyfer
- Roman Quispe César
- Sihuincha Angelino Vladimir
