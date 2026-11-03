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

La consola H2 está desactivada por defecto. Para desarrollo local puede habilitarse con `H2_CONSOLE_ENABLED=true`; no la habilites al desplegar públicamente.

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
| **Autenticación** | `POST /api/auth/login` | Inicio de sesión con credenciales configuradas por entorno. Devuelve un bearer token firmado con vencimiento de una hora y rol. |
| **Informes** | `GET /api/reports`, `GET /api/reports/inventario`, `POST /api/reports/inventario`, `PUT /api/reports/inventario/{id}`, `GET /api/reports/atenciones?desde=AAAA-MM-DD&hasta=AAAA-MM-DD`, `GET /api/reports/anual?anio=AAAA`, `GET /api/reports/ganancias?desde=AAAA-MM-DD&hasta=AAAA-MM-DD`, `GET /api/reports/cobros?desde=AAAA-MM-DD&hasta=AAAA-MM-DD` | Inventario persistido, turnos y atenciones completadas, cobros y resultado financiero calculado a partir de movimientos guardados. |
| **Finanzas** | `POST /api/finanzas/movimientos` | Registra un ingreso vinculado a un turno o un egreso. Solo ADMIN. |
| **Asistente RAG** | `POST /api/chatbot/ask`, `POST /api/chatbot/knowledge`, `DELETE /api/chatbot/knowledge/{source}` | Pacientes y ADMIN pueden consultar; solo ADMIN administra documentos. |

> `DELETE /api/turnos/{id}` cancela el turno modificando su estado a `CANCELADO`; no elimina el registro de la base de datos. El estado de atención se cambia con `PATCH /api/turnos/{id}/estado` y `{"estado":"COMPLETADO"}`.

### Credenciales y permisos

La aplicación no crea usuarios ni contraseñas por defecto. Antes de iniciar el backend, configura `AUTH_JWT_SECRET` con al menos 32 bytes y define credenciales fuera del repositorio para cada rol que se vaya a usar: `AUTH_ADMIN_USERNAME` / `AUTH_ADMIN_PASSWORD`, `AUTH_PATIENT_USERNAME` / `AUTH_PATIENT_PASSWORD`, `AUTH_ODONTOLOGO_USERNAME` / `AUTH_ODONTOLOGO_PASSWORD` y `AUTH_RECEPCIONISTA_USERNAME` / `AUTH_RECEPCIONISTA_PASSWORD`. Una pareja incompleta o vacía no habilita el acceso. Las cuentas son de configuración por entorno; todavía no hay alta/autogestión de cuentas ni contraseñas almacenadas en la base de datos.

Después de iniciar sesión, enviar `Authorization: Bearer <token>` en las rutas protegidas. ADMIN y PATIENT pueden consultar el chatbot; solo ADMIN administra la base documental. ADMIN, RECEPCIONISTA y ODONTOLOGO pueden consultar inventario y turnos; solo ADMIN puede crear o editar insumos. Los informes financieros están limitados a ADMIN y RECEPCIONISTA; solo ADMIN registra movimientos. El endpoint anual expone los totales financieros registrados únicamente a esos dos roles.

Los cobros se registran como movimientos `INGRESO` asociados a un turno existente; los egresos se registran como `EGRESO` sin turno. El resultado neto es ingresos menos egresos guardados. Si la base no tiene movimientos, las cifras son cero y se indica que no hay registros: no se generan valores de muestra. Esta implementación ofrece captura manual, no conciliación con un medio de pago ni integración contable.

El informe de turnos separa `totalTurnos` de `totalAtenciones`: solo los turnos con estado `COMPLETADO` cuentan como atenciones. El cambio de estado se limita a ADMIN y ODONTOLOGO.

### Asistente RAG (prototipo)

El endpoint de preguntas devuelve `respuesta`, `citas[]` (identificador, documento, versión y fragmento), `intencion`, `accionesSugeridas` y `timestamp`. La lista queda vacía si las fuentes aprobadas no superan el umbral `CHATBOT_MINIMUM_SIMILARITY` (por defecto `0.65`), y entonces el asistente se abstiene. Las fuentes Markdown de `src/main/resources/knowledge/` se indexan al iniciar cuando hay `OPENAI_API_KEY` configurada.

La gestión de documentos administrados se protege con el rol ADMIN y el bearer token. Se guardan en `CHATBOT_KNOWLEDGE_DIRECTORY` (por defecto `./data/knowledge/`, excluido de Git), se versionan y se vuelven a indexar al iniciar. No configures credenciales ni la clave de firma en el frontend.

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
