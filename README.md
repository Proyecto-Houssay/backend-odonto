# Novadent — Backend

API REST para la gestión de un consultorio odontológico. El backend utiliza Java 17, Spring Boot 3.5.6, Spring Web, Bean Validation y Spring Data JPA. MySQL es la base predeterminada de la aplicación; H2 en memoria se reserva para pruebas automatizadas.

## Stack tecnológico

- **Java:** 17
- **Framework:** Spring Boot 3.5.6
- **Capa Web:** Spring Web (REST)
- **Seguridad:** Spring Security OAuth2 Resource Server y JWT HS256
- **Persistencia:** Spring Data JPA / Hibernate
- **Validaciones:** Jakarta Bean Validation
- **Base predeterminada de la aplicación:** MySQL 8.4 con Connector/J. Docker Compose guarda sus datos en un volumen local nombrado.
- **Base de pruebas:** H2 en memoria (`jdbc:h2:mem:odontodb`), aislada del MySQL local.
- **Build tool:** Maven Wrapper (`mvnw` / `mvnw.cmd`)

## Ejecución y pruebas

Se requiere JDK 17 configurado en el entorno. El Maven Wrapper evita instalar Maven globalmente.

### En Windows (PowerShell)

```powershell
# Ejecutar todas las pruebas unitarias y de integración
.\mvnw.cmd clean test
```

### En macOS / Linux (Bash)

```bash
# Ejecutar todas las pruebas unitarias y de integración
./mvnw clean test
```

El servidor queda disponible en `http://localhost:8080`.
- Verificación técnica: `GET http://localhost:8080/api/health`

### Iniciar la aplicación (MySQL predeterminada)

La aplicación usa MySQL de manera predeterminada y necesita Docker para iniciar la base local. Las pruebas automatizadas usan H2 y no requieren un servidor MySQL.

#### Windows (PowerShell)

```powershell
Copy-Item .env.example .env
docker compose up -d
docker compose ps
cmd /c mvnw.cmd spring-boot:run
```

#### macOS / Linux (Bash)

```bash
cp .env.example .env
docker compose up -d
docker compose ps
./mvnw spring-boot:run
```

Ejecutá los comandos desde la raíz del repositorio. Esperá a que el servicio MySQL figure como `healthy` antes de iniciar Spring Boot. El archivo `.env` contiene credenciales locales y se ignora en Git; `.env.example` solo tiene valores de demostración, reemplazalos si otras personas pueden acceder a tu equipo. Spring Boot importa el `.env` local al iniciar y también acepta las mismas variables desde el entorno.

El usuario `novadent_app` se limita a la base configurada en `MYSQL_DATABASE`; la aplicación no se conecta como `root`. MySQL escucha únicamente en `127.0.0.1`. El esquema se actualiza con Hibernate (`ddl-auto=update`), sin migraciones versionadas, por lo que esta configuración es para desarrollo/demo y no para producción.

Para detener el contenedor sin borrar los datos, ejecutá `docker compose down`. **`docker compose down -v` elimina el volumen y borra permanentemente los datos de demo.** Para iniciar otra vez, repetí `docker compose up -d` y luego inicia la aplicación con el comando indicado arriba.

## API disponible

| Módulo | Tipo / Rutas | Descripción |
|---|---|---|
| **Salud** | `GET /api/health` | Healthcheck técnico de la aplicación. |
| **Pacientes** | `POST /api/pacientes` | Registro y validación de pacientes con DNI único. |
| **Historias Clínicas** | `HistoriaClinica`, `Diagnostico`, `Tratamiento` | Entidades, enum `EstadoTratamiento` y repositorios JPA para ficha clínica y evolución odontológica. |
| **Odontólogos** | `GET /api/odontologos`, `GET /api/odontologos/{id}`, `GET /api/odontologos/matricula/{matricula}`, `POST /api/odontologos`, `PUT /api/odontologos/{id}`, `DELETE /api/odontologos/{id}` | Gestión de staff profesional y matrícula única. |
| **Especialidades** | `GET /api/especialidades`, `GET /api/especialidades/{id}`, `POST /api/especialidades`, `DELETE /api/especialidades/{id}` | Catálogo de especialidades odontológicas. |
| **Turnos** | `GET /api/turnos`, `GET /api/turnos/{id}`, `GET /api/turnos/fecha/{fecha}`, `GET /api/turnos/odontologo/{odontologoId}`, `GET /api/turnos/paciente/{pacienteId}`, `GET /api/turnos/disponibilidad`, `POST /api/turnos`, `PUT /api/turnos/{id}`, `DELETE /api/turnos/{id}` | Agenda, asignación de turnos y verificación de disponibilidad. |
| **Usuarios** | `GET /api/usuarios`, `GET /api/usuarios/{id}`, `POST /api/usuarios`, `PUT /api/usuarios/{id}`, `DELETE /api/usuarios/{id}` | Gestión de cuentas de usuario. |
| **Autenticación** | `POST /api/auth/login` | Autentica credenciales persistidas y devuelve un JWT Bearer de 15 minutos. |
| **Informes e inventario** | `GET /api/reports`, `GET /api/reports/inventario`, `POST /api/reports/inventario`, `PUT /api/reports/inventario/{id}`, `GET /api/reports/atenciones`, `GET /api/reports/ganancias`, `GET /api/reports/cobros`, `GET /api/reports/anual` | Resumen y reportes con datos persistidos. Todas las rutas requieren `ADMINISTRADOR`. |
| **Pagos** | `POST /api/pagos` | Registro mínimo de un pago para alimentar informes; requiere `ADMINISTRADOR`. No implementa edición, eliminación ni relaciones con pacientes/tratamientos. |

> `DELETE /api/turnos/{id}` cancela el turno modificando su estado a `CANCELADO`; no elimina el registro de la base de datos.

### Usuarios y autenticación

- `POST /api/usuarios` crea cuentas y almacena la contraseña con BCrypt. Las respuestas REST de usuarios no incluyen la contraseña ni su hash. El alta requiere rol `ADMINISTRADOR`; no existe registro público.
- `PUT /api/usuarios/{id}` actualiza los datos de perfil y estado, pero no permite cambiar ni borrar la contraseña. El cambio de contraseña requiere un flujo explícito que todavía no está implementado.
- `POST /api/auth/login` recibe `usernameOrEmail` y `password`, busca el usuario persistido, compara con BCrypt y rechaza credenciales incorrectas, cuentas inexistentes o inactivas con el mismo error genérico. En caso de éxito responde con `token`, `tokenType: Bearer` y `expiresIn: 900`.
- Las rutas protegidas reciben el JWT en `Authorization: Bearer <token>`. La API es stateless: tokens inválidos o expirados reciben `401`; un usuario autenticado sin el rol requerido recibe `403`.

#### Matriz de roles

| Rol | Permisos |
|---|---|
| `ADMINISTRADOR` | Acceso administrativo completo, incluida la gestión de usuarios, roles, informes e insumos. |
| `ODONTOLOGO` | Acceso a pacientes y turnos, lectura de odontólogos/especialidades y acceso a rutas clínicas de historias, diagnósticos, tratamientos y recetas. |
| `RECEPCIONISTA` | Acceso a pacientes y turnos; lectura de odontólogos y especialidades. Sin acceso a historias clínicas, usuarios, roles, informes o administración. |

`POST /api/auth/login` y `GET /api/health` son públicos. Todos los demás endpoints requieren autenticación y se autorizan según la matriz. Las rutas clínicas todavía no tienen controladores REST implementados; sus patrones ya están reservados para el rol odontólogo y administrador.

### Informes, inventario y pagos

Todas las rutas `/api/reports/**` y `POST /api/pagos` requieren un JWT válido con rol `ADMINISTRADOR`. Sin token (o con token inválido/expirado), la respuesta es HTTP `401`; con otro rol autenticado, HTTP `403`.

| Método y ruta | Parámetros / request | DTO de respuesta y comportamiento |
|---|---|---|
| `GET /api/reports` | Ninguno | Resumen `Map`: `modulo`, `estado`, `totalAtencionesRegistradas`, `totalItemsInventario` y `reportesDisponibles`. Solo enumera reportes con endpoint implementado. |
| `GET /api/reports/inventario` | Ninguno | Lista `InventarioItemDto`: `id`, `nombre`, `categoria`, `cantidadDisponible`, `unidadMedida`, `estado`. |
| `POST /api/reports/inventario` | `InsumoRequest`: `nombre`, `categoria`, `cantidadDisponible`, `unidadMedida`, `stockMinimo` | HTTP `201` y un `InventarioItemDto`. |
| `PUT /api/reports/inventario/{id}` | `id` en la ruta y `InsumoRequest` en el body | HTTP `200` y un `InventarioItemDto`; si el insumo no existe, HTTP `404`. |
| `GET /api/reports/atenciones?desde=YYYY-MM-DD&hasta=YYYY-MM-DD` | Fechas obligatorias en formato ISO; límites inclusivos | `AtencionesReportDto`: `tipo`, `desde`, `hasta`, `totalAtenciones`, `detalle`. El detalle contiene turno, fecha/hora, paciente, odontólogo, motivo y estado. Solo cuenta turnos `ATENDIDO` de hoy o anteriores; nunca cuenta fechas futuras, aunque el turno esté marcado como atendido. |
| `GET /api/reports/ganancias?desde=YYYY-MM-DD&hasta=YYYY-MM-DD` | Fechas obligatorias en formato ISO; límites inclusivos | `GananciasReportDto`: `desde`, `hasta`, `totalIngresosBrutos`, `cantidadCobros`. “Ganancias” significa **ingresos brutos cobrados**, no utilidad neta: no hay datos de costos. Solo suma pagos `COBRADO` con fecha no futura. |
| `GET /api/reports/cobros?desde=YYYY-MM-DD&hasta=YYYY-MM-DD` | Fechas obligatorias en formato ISO; límites inclusivos | `CobrosReportDto`: `desde`, `hasta`, `totalCobrado`, `cantidadCobros`, `cobros`. Cada elemento es `PagoResponse` (`id`, `monto`, `fecha`, `estado`, `metodo`). Solo devuelve pagos `COBRADO` no futuros. |
| `GET /api/reports/anual?anio=YYYY` | Año calendario obligatorio (entre 1 y 9999) | `AnualReportDto`: `anio`, `totalAtenciones`, `totalCobrado`, `cantidadCobros`, `meses`. Incluye los 12 meses calendario; cada `MesAnualReportDto` contiene `mes`, `nombre`, `totalAtenciones`, `totalCobrado`, `cantidadCobros`. Atenciones/pagos futuros no se suman, incluso si existieran registros inconsistentes. |
| `POST /api/pagos` | `PagoRequest`: `monto`, `fecha` (`YYYY-MM-DD`), `estado` (`COBRADO`, `PENDIENTE` o `ANULADO`), `metodo` | HTTP `201` y `PagoResponse`. `monto` debe ser positivo, con hasta 10 dígitos enteros y 2 decimales; `metodo` es obligatorio y admite hasta 40 caracteres. Un pago `COBRADO` no puede tener fecha futura; `PENDIENTE` puede tenerla. |

Para los informes por rango, `desde` debe ser anterior o igual a `hasta`. Fechas faltantes, fechas ISO inválidas, rangos invertidos, año fuera del rango, payloads que no cumplen las validaciones y un pago cobrado futuro producen HTTP `400`. La aplicación no define aquí un esquema estable propio para el body de error; se conserva el manejo HTTP/Spring actual. Una escritura exitosa retorna `201`; lecturas y actualización de inventario exitosa retornan `200`.

Ejemplo de registro de pago:

```http
POST /api/pagos
Authorization: Bearer <token-de-administrador>
Content-Type: application/json
```

```json
{
  "monto": 1250.50,
  "fecha": "2025-06-10",
  "estado": "COBRADO",
  "metodo": "EFECTIVO"
}
```

La entidad `Pago` es la fuente persistida para los informes de cobros/ingresos. La aplicación usa MySQL y conserva los pagos al reiniciar gracias al volumen Docker `mysql_data`. H2 **en memoria** se usa solo en pruebas y no comparte ni modifica los datos de la aplicación local.

#### Configuración de JWT

La aplicación requiere la variable `JWT_SECRET_BASE64`. El `.env.example` trae una clave conocida únicamente para la demo local; reemplazala por una clave privada antes de compartir el entorno. La clave debe ser Base64 válido que decodifique a **32 bytes como mínimo**. El `.env` real se ignora en Git: no guardes ahí una clave de producción ni la subas al repositorio. Conservá la misma clave entre reinicios para no invalidar los tokens emitidos.

PowerShell (genera una clave para la sesión; guardá el valor en un gestor seguro si necesitás persistirlo):

```powershell
$secretBytes = New-Object byte[] 32
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($secretBytes)
$env:JWT_SECRET_BASE64 = [Convert]::ToBase64String($secretBytes)
$rng.Dispose()
Remove-Variable secretBytes
```

Git Bash (guardá la clave generada en un gestor seguro para reutilizarla):

```bash
export JWT_SECRET_BASE64="$(openssl rand -base64 32)"
```

Si regenerás la clave, los tokens ya emitidos dejan de ser válidos. Los tokens duran 15 minutos; no se implementan refresh tokens ni revocación, y un cambio de rol tiene efecto cuando vence el token existente. Para producción se requieren, además, HTTPS, almacenamiento persistente, gestión segura de secretos, auditoría y políticas de rotación/revocación.

#### Alta inicial del administrador

El primer administrador se crea solo al habilitar explícitamente el perfil `bootstrap-admin`, y únicamente si todavía no existe ningún `ADMINISTRADOR`. El perfil exige `BOOTSTRAP_ADMIN_USERNAME`, `BOOTSTRAP_ADMIN_EMAIL` y `BOOTSTRAP_ADMIN_PASSWORD`; nunca se habilita registro anónimo. Después de iniciar la aplicación y comprobar el alta, desactivá el perfil y quitá esas tres variables. No guardes la contraseña de bootstrap en el repositorio ni en el historial de comandos.

PowerShell (ejecutá desde la raíz del repositorio, con `.env` preparado y MySQL iniciado):

```powershell
$env:SPRING_PROFILES_ACTIVE = "bootstrap-admin"
$env:BOOTSTRAP_ADMIN_USERNAME = "admin"
$env:BOOTSTRAP_ADMIN_EMAIL = "admin@example.com"
$securePassword = Read-Host "Contraseña inicial del administrador" -AsSecureString
$passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
try {
    $env:BOOTSTRAP_ADMIN_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
    .\mvnw.cmd spring-boot:run
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
    Remove-Item Env:SPRING_PROFILES_ACTIVE, Env:BOOTSTRAP_ADMIN_USERNAME, Env:BOOTSTRAP_ADMIN_EMAIL, Env:BOOTSTRAP_ADMIN_PASSWORD -ErrorAction SilentlyContinue
}
```

Git Bash (ingresá la contraseña sin eco en pantalla):

```bash
export SPRING_PROFILES_ACTIVE=bootstrap-admin
export BOOTSTRAP_ADMIN_USERNAME=admin
export BOOTSTRAP_ADMIN_EMAIL=admin@example.com
read -rsp 'Contraseña inicial del administrador: ' BOOTSTRAP_ADMIN_PASSWORD; echo
export BOOTSTRAP_ADMIN_PASSWORD
./mvnw spring-boot:run
unset SPRING_PROFILES_ACTIVE BOOTSTRAP_ADMIN_USERNAME BOOTSTRAP_ADMIN_EMAIL BOOTSTRAP_ADMIN_PASSWORD
```

En ambos casos, al terminar el bootstrap el perfil debe quedar deshabilitado; para el uso normal mantené configurado solo `JWT_SECRET_BASE64`.

### Payload para registrar o actualizar turnos

Para `POST /api/turnos` y `PUT /api/turnos/{id}`, el paciente y el odontólogo deben existir previamente (`estado` es opcional):

```json
{
  "fecha": "2026-10-15",
  "hora": "10:30:00",
  "motivo": "Limpieza dental",
  "pacienteId": 1,
  "odontologoId": 1,
  "estado": "PENDIENTE"
}
```

## Estructura del proyecto

```text
src/main/java/com/proyectohoussay/odonto/
├── BackendOdontoApplication.java
├── auth/              # Controladores, servicios y DTOs de login
├── clinicalhistory/   # Historia clínica, diagnósticos y tratamientos (entidades y repositorios)
├── controller/        # Controladores REST de turnos, odontólogos, especialidades y usuarios
├── dto/               # Objetos de transferencia de datos (DTOs)
├── exception/         # Manejo centralizado de excepciones
├── health/            # Controladores de verificación de estado
├── model/             # Entidades JPA de turnos, odontólogos, especialidades y usuarios
├── patient/           # Modelo, repositorio, servicio y controlador de pacientes
├── repository/        # Repositorios JPA
└── service/           # Servicios con lógica de negocio
docs/
├── backend-scaffold.md
└── braian-dashboard-integration.md
```

## Flujo de integración

- `develop` es la rama de integración continua.
- Las ramas de trabajo individuales son `Mateo`, `Kevin`, `Braian`, `Josue` e `Iris`.
- Todo Pull Request debe dirigirse a `develop`, compilar limpiamente y aprobar `mvn test` antes de su integración.
- Mantener commits descriptivos y no publicar secretos ni archivos de entorno.

## Documentación

- [Wiki del proyecto](https://github.com/Proyecto-Houssay/backend-odonto/wiki)
- [Sprint 0](https://github.com/Proyecto-Houssay/backend-odonto/wiki/Sprint-0)
- [Stack tecnológico](https://github.com/Proyecto-Houssay/backend-odonto/wiki/Stack-tecnologico)
- [`docs/backend-scaffold.md`](docs/backend-scaffold.md)
- [`docs/braian-dashboard-integration.md`](docs/braian-dashboard-integration.md)
