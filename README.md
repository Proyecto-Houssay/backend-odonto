# Novadent — Backend

API REST para la gestión de un consultorio odontológico. El backend utiliza Java 17, Spring Boot 3.5.6, Spring Web, Bean Validation, Spring Data JPA y H2.

## Ejecutar y probar

Se requiere Java 17. El Maven Wrapper evita tener que instalar Maven globalmente.

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

La base configurada es H2 en memoria (`jdbc:h2:mem:odontodb`): los datos se pierden al detener la aplicación. Durante el desarrollo está habilitada la consola en `/h2-console`.

## API disponible

| Módulo | Rutas |
| --- | --- |
| Salud | `GET /api/health` |
| Pacientes | `POST /api/pacientes` |
| Odontólogos | `GET /api/odontologos`, `GET /api/odontologos/{id}`, `GET /api/odontologos/matricula/{matricula}`, `POST /api/odontologos`, `PUT /api/odontologos/{id}`, `DELETE /api/odontologos/{id}` |
| Especialidades | `GET /api/especialidades`, `GET /api/especialidades/{id}`, `POST /api/especialidades`, `DELETE /api/especialidades/{id}` |
| Usuarios | `GET /api/usuarios`, `GET /api/usuarios/{id}`, `POST /api/usuarios`, `PUT /api/usuarios/{id}`, `DELETE /api/usuarios/{id}` |
| Autenticación | `POST /api/auth/login` |
| Turnos | `GET /api/turnos`, `GET /api/turnos/{id}`, `GET /api/turnos/fecha/{fecha}`, `GET /api/turnos/odontologo/{odontologoId}`, `GET /api/turnos/paciente/{pacienteId}`, `GET /api/turnos/disponibilidad`, `POST /api/turnos`, `PUT /api/turnos/{id}`, `DELETE /api/turnos/{id}` |

`DELETE /api/turnos/{id}` cancela el turno cambiando su estado a `CANCELADO`; no elimina el registro. La ruta de disponibilidad recibe `odontologoId`, `fecha` y `hora` como parámetros de consulta.

El login es una implementación provisional con credenciales fijas en el servicio. No debe considerarse autenticación segura ni usarse en producción.

### Crear o actualizar un turno

El paciente y el odontólogo deben existir previamente. Para `POST /api/turnos` y `PUT /api/turnos/{id}`, el cuerpo acepta estos campos (`estado` es opcional):

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

## Flujo de integración

- `develop` es la rama de integración.
- Los pull requests se dirigen a `develop` y mencionan las HU/TR relacionadas.
- Ejecutar `test` antes de enviar cambios.
- Mantener commits descriptivos y no publicar secretos ni archivos de entorno.

## Documentación

- [Wiki del proyecto](https://github.com/Proyecto-Houssay/backend-odonto/wiki)
- [Sprint 0](https://github.com/Proyecto-Houssay/backend-odonto/wiki/Sprint-0)
- [Stack tecnológico](https://github.com/Proyecto-Houssay/backend-odonto/wiki/Stack-tecnologico)
- [`docs/backend-scaffold.md`](docs/backend-scaffold.md): decisiones del scaffold inicial; refleja el alcance de esa etapa, no el estado actual de la API.
- [`docs/braian-dashboard-integration.md`](docs/braian-dashboard-integration.md): pautas para integrar el dashboard con la API.
