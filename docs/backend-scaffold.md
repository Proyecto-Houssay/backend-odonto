# Scaffold inicial del backend

## Resultado

Se creó el proyecto mínimo de Spring Boot en la rama `Braian` para que el equipo pueda comenzar a programar sobre una base compilable y verificable.

## Decisiones

| Área | Decisión | Motivo |
|---|---|---|
| Java | 17 | Es la versión instalada y una base LTS compatible con el proyecto |
| Spring Boot | 3.5.6 | Está publicada en Maven Central y funciona con Java 17 |
| Build | Maven Wrapper | Permite ejecutar el proyecto sin instalar Maven globalmente |
| Web | Spring Web | Necesario para exponer la API REST |
| Validación | Bean Validation | Base para validar futuros DTOs |
| Persistencia | Pendiente | Falta confirmar motor y entorno de base de datos |
| API docs | Pendiente | Se definirá OpenAPI/Swagger junto con los contratos reales |

Spring Initializr ofrecía versiones 4.x en su metadata, pero la versión consultada no estaba disponible en Maven Central. Se eligió una versión publicada para evitar un proyecto que no pudiera resolver dependencias.

## Implementación

- `BackendOdontoApplication`: punto de entrada de Spring Boot.
- `HealthController`: endpoint técnico `GET /api/health`.
- `HealthControllerTest`: prueba MVC del contrato de salud.
- `BackendOdontoApplicationTests`: prueba de carga del contexto.
- `application.properties`: nombre de la aplicación, sin credenciales.
- Maven Wrapper: ejecución reproducible en Windows, macOS y Linux.

## Verificación

```text
Tests run: 2
Failures: 0
Errors: 0
BUILD SUCCESS
```

Comandos usados:

```powershell
.\mvnw.cmd test
```

## Alcance y límites

Este scaffold no implementa:

- Usuarios, roles o autenticación.
- Pacientes, odontólogos o turnos.
- Historias clínicas.
- Informes, caja o inventario.
- Persistencia.

Esas capacidades deben entrar mediante sus HU/TR y contratos API aprobados.

## Próximo cambio recomendado

Confirmar la base de datos y recién después agregar JPA, el driver y la configuración por ambiente. Luego definir los contratos OpenAPI/Swagger antes de conectar el dashboard frontend.
