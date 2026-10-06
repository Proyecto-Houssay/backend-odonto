# Novadent — Backend

API REST para la gestión de un consultorio odontológico. El backend utiliza Java 17, Spring Boot 3.5.6, Spring Web, Bean Validation, Spring Data JPA y base de datos H2 en memoria.

## Stack tecnológico

- **Java:** 17
- **Framework:** Spring Boot 3.5.6
- **Capa Web:** Spring Web (REST)
- **Persistencia:** Spring Data JPA / Hibernate
- **Validaciones:** Jakarta Bean Validation
- **Base de datos (configuración actual):** H2 en memoria (`jdbc:h2:mem:odontodb`). No hay perfiles ni una base de datos de producción configurados todavía.
- **Build tool:** Maven Wrapper (`mvnw` / `mvnw.cmd`)

## Ejecución y pruebas

Se requiere JDK 17 configurado en el entorno. El Maven Wrapper evita instalar Maven globalmente.

### En Windows (PowerShell)

```powershell
# Ejecutar todas las pruebas unitarias y de integración
.\mvnw.cmd test

# Iniciar el servidor de desarrollo
.\mvnw.cmd spring-boot:run
```

### En macOS / Linux (Bash)

```bash
# Ejecutar todas las pruebas unitarias y de integración
./mvnw test

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
| **Autenticación** | `POST /api/auth/login` | Inicio de sesión base. |
| **Informes** | `GET /api/reports`, `GET /api/reports/inventario`, `POST /api/reports/inventario`, `PUT /api/reports/inventario/{id}`, `GET /api/reports/atenciones` | Resumen e informes de inventario persistido y atenciones. |

> `DELETE /api/turnos/{id}` cancela el turno modificando su estado a `CANCELADO`; no elimina el registro de la base de datos.

### Usuarios y autenticación

- `POST /api/usuarios` crea usuarios y almacena la contraseña con BCrypt. Las respuestas REST de usuarios no incluyen la contraseña ni su hash.
- `PUT /api/usuarios/{id}` actualiza los datos de perfil y estado, pero no permite cambiar ni borrar la contraseña. El cambio de contraseña requiere un flujo explícito que todavía no está implementado.
- `POST /api/auth/login` conserva el payload `usernameOrEmail` y `password`. Busca el usuario persistido por nombre de usuario o email, compara la contraseña recibida con el hash BCrypt y rechaza credenciales incorrectas, cuentas inexistentes e inactivas con el mismo mensaje genérico.
- El login solo valida credenciales: todavía no emite sesiones ni tokens y la API no aplica autorización por roles ni protección de acceso a los demás endpoints. Por eso, este mecanismo no es suficiente para considerar la autenticación lista para producción.

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
