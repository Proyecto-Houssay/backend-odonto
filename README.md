# Novadent — Backend

API REST para la gestión de un consultorio odontológico. El backend utiliza Java 17, Spring Boot 3.5.6, Spring Web, Bean Validation, Spring Data JPA y base de datos H2 en memoria.

## Stack tecnológico

- **Java:** 17
- **Framework:** Spring Boot 3.5.6
- **Capa Web:** Spring Web (REST)
- **Seguridad:** Spring Security OAuth2 Resource Server y JWT HS256
- **Persistencia:** Spring Data JPA / Hibernate
- **Validaciones:** Jakarta Bean Validation
- **Base de datos (configuración actual):** H2 en memoria (`jdbc:h2:mem:odontodb`). No hay perfiles ni una base de datos de producción configurados todavía.
- **Build tool:** Maven Wrapper (`mvnw` / `mvnw.cmd`)

## Ejecución y pruebas

Se requiere JDK 17 configurado en el entorno. El Maven Wrapper evita instalar Maven globalmente.

### En Windows (PowerShell)

```powershell
# Ejecutar todas las pruebas unitarias y de integración
.\mvnw.cmd clean test

# Iniciar el servidor de desarrollo
.\mvnw.cmd spring-boot:run
```

### En macOS / Linux (Bash)

```bash
# Ejecutar todas las pruebas unitarias y de integración
./mvnw clean test

# Iniciar el servidor de desarrollo
./mvnw spring-boot:run
```

El servidor queda disponible en `http://localhost:8080`.
- Verificación técnica: `GET http://localhost:8080/api/health`
- Consola de base de datos H2: `http://localhost:8080/h2-console`

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
| **Informes** | `GET /api/reports`, `GET /api/reports/inventario`, `POST /api/reports/inventario`, `PUT /api/reports/inventario/{id}`, `GET /api/reports/atenciones` | Resumen e informes de inventario persistido y atenciones. |

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

#### Configuración de JWT

La aplicación requiere la variable `JWT_SECRET_BASE64` en cada entorno. Debe ser Base64 válido que decodifique a **32 bytes como mínimo**; no hay una clave predeterminada ni una clave de producción en el repositorio. Conservá la misma clave entre reinicios y no la subas a Git.

PowerShell (clave temporal para la sesión; guardá el valor en un gestor seguro si necesitás persistirlo):

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

PowerShell (misma sesión donde configuraste `JWT_SECRET_BASE64`):

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
