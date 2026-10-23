# Novadent — Backend

API REST para un proyecto académico de gestión de un consultorio odontológico (IEFI). Pagos y facturación están fuera de alcance; receta se reserva para pruebas y no es una función REST de uso habitual. El backend utiliza Java 17, Spring Boot 3.5.6, Spring Web, Bean Validation y Spring Data JPA. MySQL es la base predeterminada de la aplicación; H2 en memoria se reserva para pruebas automatizadas.

## Stack tecnológico

- **Java:** 17
- **Framework:** Spring Boot 3.5.6
- **Capa Web:** Spring Web (REST)
- **Documentación interactiva:** Swagger UI (SpringDoc OpenAPI)
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

Al iniciar la aplicación, el servidor queda disponible en `http://localhost:8080`.
- Verificación técnica: `GET http://localhost:8080/api/health`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Especificación OpenAPI (JSON): `http://localhost:8080/v3/api-docs`

### Iniciar la aplicación (MySQL predeterminada)

La aplicación usa MySQL de manera predeterminada. Podés usar el contenedor de desarrollo **o** una instalación local de MySQL; Docker no es obligatorio si MySQL ya está funcionando. Las pruebas automatizadas usan H2 y no requieren MySQL.

#### Windows (PowerShell)

```powershell
docker compose up -d
docker compose ps
cmd /c mvnw.cmd spring-boot:run
```

#### macOS / Linux (Bash)

```bash
docker compose up -d
docker compose ps
./mvnw spring-boot:run
```

Ejecutá los comandos desde la raíz de `backend-odonto`, donde está `mvnw.cmd`. Solo si todavía no existe `.env`, copiá `.env.example` a `.env` y configurá tus credenciales locales; **no sobrescribas** un `.env` existente. Si usás Docker, esperá a que MySQL figure como `healthy`; si ya usás MySQL instalado en Windows, omití `docker compose up -d` y `docker compose ps`, pero comprobá que el servidor, puerto, base y usuario coincidan con tu `.env`. El archivo `.env` contiene credenciales locales y se ignora en Git; `.env.example` solo tiene valores de demostración. Spring Boot importa el `.env` local al iniciar y también acepta las mismas variables desde el entorno.

En el contenedor de desarrollo, el usuario `novadent_app` se limita a la base configurada en `MYSQL_DATABASE`, y MySQL escucha únicamente en `127.0.0.1`. Configurá la aplicación con un usuario dedicado, no con `root`. El esquema se actualiza con Hibernate (`ddl-auto=update`), sin migraciones versionadas, por lo que esta configuración es para desarrollo/demo y no para producción.

Si usás el contenedor, para detenerlo sin borrar datos ejecutá `docker compose down`. **`docker compose down -v` elimina el volumen y borra permanentemente sus datos.** Para iniciar otra vez, repetí `docker compose up -d` y luego iniciá la aplicación con el comando indicado arriba.

### Probar Swagger y comprobar MySQL

Swagger UI es una página para explorar y enviar solicitudes a la API; no es una base de datos. Las operaciones protegidas requieren un JWT válido:

1. Abrí `http://localhost:8080/swagger-ui/index.html` y ejecutá `POST /api/auth/login` con las credenciales de un usuario existente. Para crear usuarios necesitás un `ADMINISTRADOR`; para crear pacientes también sirven `ODONTOLOGO` y `RECEPCIONISTA`.
2. Copiá **solo el valor** `token` de la respuesta. Pulsá **Authorize** en Swagger, pegalo sin comillas ni el prefijo `Bearer`, y confirmá. El token vence a los 15 minutos; si recibís `401` por vencimiento, volvé a iniciar sesión y reemplazalo.
3. Ejecutá `POST /api/pacientes` con datos **ficticios**, un DNI único y sin campo `id` (lo genera la base). Una respuesta `200` confirma que el endpoint aceptó la creación.
4. En MySQL Workbench, conectate al mismo servidor y seleccioná como esquema predeterminado el valor de `MYSQL_DATABASE` de tu `.env`. Ejecutá `SELECT id, nombre, apellido, dni FROM paciente WHERE dni = 'DNI_FICTICIO';`, sustituyendo el DNI por el que enviaste. La tabla es `paciente` (singular), no `pacientes`.

La guía [Swagger → MySQL](docs/swagger-mysql-workflow.md) incluye un ejemplo completo y la matriz de permisos. No compartas contraseñas, JWT ni datos personales en capturas o chats.

## API disponible

| Módulo | Tipo / Rutas | Descripción |
|---|---|---|
| **Salud** | `GET /api/health` | Healthcheck técnico de la aplicación. |
| **Pacientes** | `POST /api/pacientes` | Registro y validación de pacientes con DNI único. |
| **Historias Clínicas** | `POST /api/historias-clinicas`, `GET /api/historias-clinicas/{id}`, `GET /api/historias-clinicas/paciente/{pacienteId}`, `POST /api/historias-clinicas/{id}/diagnosticos` | Gestión de historias clínicas y fichas odontológicas con evolución clínica. |
| **Diagnósticos y Tratamientos** | `POST /api/diagnosticos`, `GET /api/diagnosticos/{id}`, `GET /api/diagnosticos/historia/{historiaClinicaId}`, `POST /api/diagnosticos/{id}/tratamientos`, `PATCH /api/diagnosticos/tratamientos/{id}/estado` | Registro de diagnósticos odontológicos y actualización de planes de tratamiento. |
| **Diagnóstico por Imagen con IA** | `POST /api/ia/diagnostico-imagen/analizar`, `POST /api/ia/diagnostico-imagen/upload`, `GET /api/ia/diagnostico-imagen/paciente/{pacienteId}`, `GET /api/ia/diagnostico-imagen/{id}`, `GET /api/ia/diagnostico-imagen/catalogo` | Análisis asistido por IA de radiografías (panorámicas, periapicales, bite-wing, CBCT 3D) y fotos intraorales, detección de patologías, cálculo de severidad/urgencia e integración automática en historia clínica. |
| **Odontólogos** | `GET /api/odontologos`, `GET /api/odontologos/{id}`, `GET /api/odontologos/matricula/{matricula}`, `POST /api/odontologos`, `PUT /api/odontologos/{id}`, `DELETE /api/odontologos/{id}` | Gestión de staff profesional y matrícula única. |
| **Especialidades** | `GET /api/especialidades`, `GET /api/especialidades/{id}`, `POST /api/especialidades`, `DELETE /api/especialidades/{id}` | Catálogo de especialidades odontológicas. |
| **Turnos** | `GET /api/turnos`, `GET /api/turnos/{id}`, `GET /api/turnos/fecha/{fecha}`, `GET /api/turnos/odontologo/{odontologoId}`, `GET /api/turnos/paciente/{pacienteId}`, `GET /api/turnos/disponibilidad`, `POST /api/turnos`, `PUT /api/turnos/{id}`, `DELETE /api/turnos/{id}` | Agenda, asignación de turnos y verificación de disponibilidad. |
| **Usuarios** | `GET /api/usuarios`, `GET /api/usuarios/{id}`, `POST /api/usuarios`, `PUT /api/usuarios/{id}`, `DELETE /api/usuarios/{id}` | Gestión de cuentas de usuario. |
| **Autenticación** | `POST /api/auth/login` | Autentica credenciales persistidas y devuelve un JWT Bearer de 15 minutos. |
| **Informes e inventario** | `GET /api/reports`, `GET /api/reports/inventario`, `POST /api/reports/inventario`, `PUT /api/reports/inventario/{id}`, `GET /api/reports/atenciones`, `GET /api/reports/anual` | Resumen y reportes con datos persistidos. Todas las rutas requieren `ADMINISTRADOR`. |


> `DELETE /api/turnos/{id}` cancela el turno modificando su estado a `CANCELADO`; no elimina el registro de la base de datos.

### Usuarios y autenticación

- `POST /api/usuarios` crea cuentas y almacena la contraseña con BCrypt. Las respuestas REST de usuarios no incluyen la contraseña ni su hash. El alta requiere rol `ADMINISTRADOR`; no existe registro público.
- `PUT /api/usuarios/{id}` actualiza los datos de perfil y estado, pero no permite cambiar ni borrar la contraseña. El cambio de contraseña requiere un flujo explícito que todavía no está implementado.
- `POST /api/auth/login` recibe `usernameOrEmail` y `password`, busca el usuario persistido, compara con BCrypt y rechaza credenciales incorrectas, cuentas inexistentes o inactivas con el mismo error genérico. En caso de éxito responde con `token`, `tokenType: Bearer` y `expiresIn: 900`.
- Las rutas protegidas reciben el JWT en `Authorization: Bearer <token>`. La API es stateless: tokens inválidos o expirados reciben `401`; un usuario autenticado sin el rol requerido recibe `403`.

#### Matriz de roles

| Rol | Permisos |
|---|---|
| `ADMINISTRADOR` | Acceso administrativo completo, incluida la gestión de usuarios, roles, informes, historias clínicas, diagnósticos e IA. |
| `ODONTOLOGO` | Acceso a pacientes y turnos, lectura de odontólogos/especialidades y acceso completo a historias clínicas, diagnósticos, tratamientos y al módulo de Diagnóstico por Imagen con IA. |
| `RECEPCIONISTA` | Acceso a pacientes y turnos; lectura de odontólogos y especialidades. Sin acceso a historias clínicas, usuarios, roles, informes o administración. |

`POST /api/auth/login` y `GET /api/health` son públicos. Todos los demás endpoints requieren autenticación y se autorizan según la matriz.

### Informes e inventario

Todas las rutas `/api/reports/**` requieren un JWT válido con rol `ADMINISTRADOR`. Sin token (o con token inválido/expirado), la respuesta es HTTP `401`; con otro rol autenticado, HTTP `403`.

| Método y ruta | Parámetros / request | DTO de respuesta y comportamiento |
|---|---|---|
| `GET /api/reports` | Ninguno | Resumen `Map`: `modulo`, `estado`, `totalAtencionesRegistradas`, `totalItemsInventario` y `reportesDisponibles`. Solo enumera reportes con endpoint implementado. |
| `GET /api/reports/inventario` | Ninguno | Lista `InventarioItemDto`: `id`, `nombre`, `categoria`, `cantidadDisponible`, `unidadMedida`, `estado`. |
| `POST /api/reports/inventario` | `InsumoRequest`: `nombre`, `categoria`, `cantidadDisponible`, `unidadMedida`, `stockMinimo` | HTTP `201` y un `InventarioItemDto`. |
| `PUT /api/reports/inventario/{id}` | `id` en la ruta y `InsumoRequest` en el body | HTTP `200` y un `InventarioItemDto`; si el insumo no existe, HTTP `404`. |
| `GET /api/reports/atenciones?desde=YYYY-MM-DD&hasta=YYYY-MM-DD` | Fechas obligatorias en formato ISO; límites inclusivos | `AtencionesReportDto`: `tipo`, `desde`, `hasta`, `totalAtenciones`, `detalle`. El detalle contiene turno, fecha/hora, paciente, odontólogo, motivo y estado. Solo cuenta turnos `ATENDIDO` de hoy o anteriores; nunca cuenta fechas futuras, aunque el turno esté marcado como atendido. |
| `GET /api/reports/anual?anio=YYYY` | Año calendario obligatorio (entre 1 y 9999) | `AnualReportDto`: `anio`, `totalAtenciones`, `meses`. Incluye los 12 meses; cada `MesAnualReportDto` contiene `mes`, `nombre`, `totalAtenciones`. No cuenta turnos futuros aunque figuren como atendidos. |

Para el informe por rango, `desde` debe ser anterior o igual a `hasta`. Fechas faltantes o inválidas, rangos invertidos y años fuera del rango producen HTTP `400`. Una escritura de inventario exitosa retorna `201`; lecturas y actualización, `200`.

**Fuera de alcance académico:** no hay endpoints de pagos, cobros o ganancias; tampoco se publican en Swagger. Si una base MySQL local conserva una tabla `pagos` de una versión anterior, este cambio no la borra ni altera sus datos. Las recetas quedan solo como concepto de pruebas, sin endpoint REST público. H2 en memoria se usa para tests y no modifica MySQL.

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

### Diagnóstico por Imagen con IA

El backend integra un motor inteligente para el triaje y diagnóstico preliminar asistido por IA de estudios de imágenes dentales:

- **Modalidades admitidas:**
  - `RADIOGRAFIA_PANORAMICA`: Evaluación general de maxilares, terceros molares retenidos, pérdida ósea generalizada y caries interproximales.
  - `RADIOGRAFIA_PERIAPICAL`: Evaluación de pieza dental completa, lesiones radiolúcidas periapicales (granulomas, quistes), necrosis pulpar y caries profundas.
  - `BITE_WING`: Aletas de mordida para detección precoz de caries interproximales y desajuste marginal de restauraciones.
  - `FOTO_INTRAORAL`: Detección visual de cálculo/sarro subgingival, placa bacteriana y gingivitis marginal.
  - `TOMOGRAFIA_CONE_BEAM`: Análisis tridimensional (CBCT 3D) de proximidad con el conducto dentario inferior y reabsorción ósea.
- **Salida estructurada:** Hallazgos con nomenclatura odontológica FDI, severidad (`LEVE`, `MODERADA`, `SEVERA`), nivel de urgencia (`BAJA`, `MEDIA`, `ALTA`, `URGENTE`), índice de severidad (1-10) y roadmap de tratamientos sugeridos.
- **Integración automática en Historia Clínica:** Mediante `guardarEnHistoria: true`, crea automáticamente un registro `Diagnostico` vinculado a la `HistoriaClinica` del paciente con los `Tratamiento`s en estado `PENDIENTE`.

## Estructura del proyecto

```text
src/main/java/com/proyectohoussay/odonto/
├── BackendOdontoApplication.java
├── auth/              # Controladores, servicios y DTOs de login
├── config/            # Configuración de OpenAPI / Swagger
├── clinicalhistory/   # Historia clínica, diagnósticos, tratamientos y controladores REST
│   ├── ai/            # Diagnóstico por Imagen con IA (motor, entidad, repositorio, controlador y DTOs)
│   └── dto/           # DTOs de historias clínicas, diagnósticos y tratamientos
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
├── braian-dashboard-integration.md
├── gestion-personal-backlog.md
└── swagger-mysql-workflow.md
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
- [Backlog: Gestión de Personal](docs/gestion-personal-backlog.md)
- [`docs/backend-scaffold.md`](docs/backend-scaffold.md)
- [`docs/braian-dashboard-integration.md`](docs/braian-dashboard-integration.md)
