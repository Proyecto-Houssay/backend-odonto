# Integración backend para el dashboard de Braian

## Resultado

El dashboard se entrega como una capa de presentación independiente del backend. En esta etapa no se agregan endpoints ni dependencias al backend porque el repositorio todavía no contiene un proyecto Spring Boot ejecutable.

## Contrato actual

Las vistas del dashboard son placeholders y no deben asumir datos reales. Las siguientes capacidades quedan pendientes de sus historias y tareas propias:

- Usuarios, roles y permisos.
- Pacientes y odontólogos.
- Turnos.
- Historias clínicas.
- Informes, caja e inventario.

No se inventan rutas REST, nombres de entidades ni formatos JSON hasta que el scaffold backend y sus decisiones de persistencia estén aprobados.

## Punto de integración esperado

Cuando exista el backend Spring Boot, cada página podrá consumir servicios mediante una capa cliente aislada. El layout no debe conocer detalles de persistencia; solamente las páginas de módulo o sus hooks deben depender de contratos API versionados.

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

## Próximo paso backend

La implementación del scaffold Spring Boot corresponde a `TR-00.5 — Preparar el esqueleto inicial del backend`. Hasta que esa tarea se complete, este documento funciona como límite explícito de integración y evita crear contratos ficticios.

## Relación con frontend

- Frontend: `frontend-odonto/docs/braian-dashboard.md`
- Rama de trabajo: `Braian`
- Base de integración: `develop`
