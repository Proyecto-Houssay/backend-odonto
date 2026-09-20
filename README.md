# Novadent — Backend

Backend del sistema de gestión odontológica. La rama `Braian` contiene ahora el scaffold mínimo de Spring Boot y un endpoint técnico de salud; todavía no incluye módulos de negocio.

## Estado actual

- Java 17
- Spring Boot 3.5.6
- Maven Wrapper (`mvnw` / `mvnw.cmd`)
- Spring Web
- Bean Validation
- Pruebas Spring Boot
- Endpoint `GET /api/health`

La base de datos, JPA, autenticación, roles y OpenAPI/Swagger quedan para cambios posteriores con HU/TR definidos.

## Inicio rápido

En Windows:

```powershell
git clone https://github.com/Proyecto-Houssay/backend-odonto.git
cd backend-odonto
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

En macOS/Linux:

```bash
./mvnw test
./mvnw spring-boot:run
```

El endpoint técnico queda disponible en:

```text
GET http://localhost:8080/api/health
```

Respuesta esperada:

```json
{"status":"ok"}
```

## Estructura inicial

```text
src/
├── main/
│   ├── java/com/proyectohoussay/odonto/
│   │   ├── BackendOdontoApplication.java
│   │   └── health/HealthController.java
│   └── resources/application.properties
└── test/
    ├── java/com/proyectohoussay/odonto/BackendOdontoApplicationTests.java
    └── java/com/proyectohoussay/odonto/health/HealthControllerTest.java
docs/
├── backend-scaffold.md
└── braian-dashboard-integration.md
```

## Comandos

| Comando | Uso |
|---|---|
| `.\mvnw.cmd test` | Compila y ejecuta las pruebas en Windows |
| `.\mvnw.cmd spring-boot:run` | Inicia la aplicación en Windows |
| `./mvnw test` | Compila y ejecuta las pruebas en macOS/Linux |
| `./mvnw spring-boot:run` | Inicia la aplicación en macOS/Linux |

## Flujo de trabajo

- `develop` es la base de integración.
- La rama de este bloque es `Braian`.
- Los Pull Requests deben apuntar a `develop` y vincular `TR-00.5`.
- No publicar secretos ni archivos de entorno.

## Próximos pasos

1. Confirmar motor de base de datos con el equipo.
2. Agregar JPA y el driver correspondiente.
3. Definir contratos OpenAPI/Swagger.
4. Implementar módulos de negocio mediante sus HU/TR, sin mezclar responsabilidades.

## Documentación

- [`docs/backend-scaffold.md`](docs/backend-scaffold.md)
- [`docs/braian-dashboard-integration.md`](docs/braian-dashboard-integration.md)
- [Wiki del proyecto](https://github.com/Proyecto-Houssay/backend-odonto/wiki)
