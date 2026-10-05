# Integración backend para el dashboard de Braian

## Estado actual

Este documento conserva las pautas de integración del dashboard. La descripción del scaffold y de sus endpoints iniciales quedó superada: el backend ya expone rutas de pacientes, odontólogos, especialidades, usuarios, autenticación y turnos. El inventario actualizado está en el [README](../README.md).

Las páginas del dashboard deben consumir únicamente rutas y formatos que estén implementados y probados. No asumir que una operación existe para un módulo solo porque exista su entidad; por ejemplo, pacientes actualmente expone el registro mediante `POST /api/pacientes`.

## Punto de integración

Cuando se definan o actualicen los contratos OpenAPI/Swagger, cada página podrá consumir servicios mediante una capa cliente aislada. El layout no debe conocer detalles de persistencia; solamente las páginas de módulo o sus hooks deben depender de contratos API versionados.

```text
DashboardLayout
  └── Outlet
      └── Página de módulo
          └── Cliente API / hook
              └── Endpoint REST Spring Boot
```

## Reglas para la integración

- Mantener el layout y las rutas como infraestructura compartida.
- Definir primero el contrato OpenAPI/Swagger y luego implementar el cliente frontend.
- Centralizar la URL base mediante configuración de entorno; nunca hardcodear secretos.
- Modelar estados de carga, vacío y error antes de reemplazar un placeholder.
- Coordinar cambios de API con la HU/TR del módulo correspondiente.

## Documento relacionado

El alcance y la verificación del scaffold original están documentados en [`backend-scaffold.md`](backend-scaffold.md); ese documento describe una etapa anterior del backend.

- Frontend: `frontend-odonto/docs/braian-dashboard.md`
- Rama de trabajo: `Braian`
- Base de integración: `develop`
