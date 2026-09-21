# Integración backend para el dashboard de Braian

## Resultado

El dashboard se entrega como una capa de presentación independiente del backend. La rama `Braian` ahora contiene un scaffold Spring Boot y un endpoint técnico de salud, pero todavía no expone contratos de negocio.

## Contrato actual

Las vistas del dashboard siguen siendo placeholders y no deben asumir datos reales. Las siguientes capacidades quedan pendientes de sus historias y tareas propias:

- Usuarios, roles y permisos.
- Pacientes y odontólogos.
- Turnos.
- Historias clínicas.
- Informes, caja e inventario.

El único endpoint disponible actualmente es `GET /api/health`, destinado a verificar que el servicio está activo. No se deben inventar rutas REST, entidades ni formatos JSON de negocio.

## Punto de integración esperado

Cuando se aprueben los contratos OpenAPI/Swagger, cada página podrá consumir servicios mediante una capa cliente aislada. El layout no debe conocer detalles de persistencia; solamente las páginas de módulo o sus hooks deben depender de contratos API versionados.

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

## Relación con el scaffold

La implementación y la verificación del scaffold están documentadas en [`backend-scaffold.md`](backend-scaffold.md).

- Frontend: `frontend-odonto/docs/braian-dashboard.md`
- Rama de trabajo: `Braian`
- Base de integración: `develop`
