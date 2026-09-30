# Novadent — Backend

API REST para la gestión de un consultorio odontológico. El proyecto usa Java 17, Spring Boot 3, Spring Web, Bean Validation, Spring Data JPA y H2 para desarrollo y pruebas.

## Ejecutar y probar

En Windows:

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

En macOS/Linux:

```bash
./mvnw test
./mvnw spring-boot:run
```

La aplicación expone el estado en `GET /api/health`. La consola H2 queda habilitada en `/h2-console` durante el desarrollo.

## Módulos disponibles

- Pacientes: `POST /api/pacientes`.
- Odontólogos y especialidades: rutas bajo `/api/odontologos` y `/api/especialidades`.
- Turnos: rutas bajo `/api/turnos` para consultar, registrar, modificar, cancelar y comprobar disponibilidad.
- Usuarios y autenticación: repositorios y servicios asociados.

Para registrar un turno se envían los identificadores de las entidades existentes:

```json
{
  "fecha": "2026-10-15",
  "hora": "10:30:00",
  "motivo": "Limpieza dental",
  "pacienteId": 1,
  "odontologoId": 1
}
```

## Flujo de integración

- `develop` es la rama de integración.
- Los pull requests se dirigen a `develop` y mencionan las HU/TR relacionadas.
- Ejecutar `test` antes de enviar cambios.
- Mantener commits descriptivos y no publicar secretos ni archivos de entorno.

## Documentación

- [Wiki del proyecto](https://github.com/Proyecto-Houssay/backend-odonto/wiki)
- [Sprint 0](https://github.com/Proyecto-Houssay/backend-odonto/wiki/Sprint-0)
- [Stack tecnológico](https://github.com/Proyecto-Houssay/backend-odonto/wiki/Stack-tecnologico)
- [`docs/backend-scaffold.md`](docs/backend-scaffold.md)
- [`docs/braian-dashboard-integration.md`](docs/braian-dashboard-integration.md)
