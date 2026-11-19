# Odontograma: historias de usuario y tareas técnicas

Responsable propuesto para todas las historias y tareas: **@Braianaguilera**.

> Estas son las definiciones de trabajo del repositorio. La publicación y asignación en GitHub quedan pendientes hasta que exista acceso de escritura a Issues.

## HU-OD-01 — Registrar odontograma digital firmado

**Como** odontólogo autorizado, **quiero** registrar el estado de los 32 dientes FDI de un paciente con consentimiento y firma, **para** conservar un registro clínico trazable y emitir un presupuesto académico ilustrativo.

**Criterios de aceptación**

- El odontólogo o administrador autenticado puede registrar y consultar odontogramas de pacientes existentes.
- La API valida dientes FDI, afecciones, consentimiento y firma PNG; calcula el total con el catálogo del servidor.
- El registro firmado es inmutable. El listado por paciente no incluye la imagen de la firma.
- El PDF del frontend coincide con los datos devueltos por la API y no se presenta como comprobante de pago.
- El flujo se comprueba con pruebas automatizadas y una lectura/escritura controlada en MySQL local.

## HU-OD-02 — Respetar los permisos de los roles

**Como** integrante del consultorio, **quiero** consultar el odontograma de acuerdo con mi rol y limitar su registro a personal clínico o administrador, **para** coordinar la atención sin modificar registros desde recepción.

**Criterios de aceptación**

- Odontólogo y administrador pueden crear y consultar odontogramas.
- Recepcionista puede gestionar pacientes y turnos según los permisos existentes y consultar catálogo, listado por paciente y detalle del odontograma (HTTP 200), pero no registrarlo (HTTP 403).
- El detalle para recepción incluye los datos clínicos ilustrativos, pero omite `firmaBase64`; solo odontólogo y administrador pueden recibir la firma PNG. La restricción se aplica en el backend con el rol autenticado, no solo en la interfaz. No abre historias clínicas, diagnósticos o tratamientos.
- Una sesión no autenticada recibe HTTP 401.
- Los permisos se verifican iniciando sesión con usuarios persistidos de cada rol; un JWT sintético por sí solo no completa esta comprobación.

## TR-OD-01 — Probar autenticación y autorización reales

**Relacionada con:** HU-OD-02. **Responsable:** @Braianaguilera.

- Crear usuarios de prueba persistidos con roles ADMINISTRADOR, ODONTOLOGO y RECEPCIONISTA.
- Iniciar sesión a través de `/api/auth/login` y usar los JWT recibidos para verificar los códigos 200/201, 401 y 403 de `/api/odontograma`.
- Probar que recepción conserva sus funciones de pacientes y turnos, puede leer el catálogo, listado y detalle del odontograma sin recibir la firma PNG, pero recibe 403 al intentar registrar uno. Verificar que odontólogo y administrador sí reciben la firma en el detalle y la respuesta de creación.

## TR-OD-02 — Verificar persistencia e integración con MySQL

**Relacionada con:** HU-OD-01. **Responsable:** @Braianaguilera.

- Ejecutar un smoke test controlado en la instancia local de MySQL configurada por `.env`, sin modificar pacientes reales.
- Registrar un odontograma de prueba mediante la API, consultarlo por ID y por paciente, y verificar sus filas relacionadas en MySQL.
- Comparar catálogo, total, consentimiento y firma del registro con lo mostrado por el frontend; documentar el resultado y la limpieza de datos de prueba.

## TR-OD-03 — Trazar al profesional responsable

**Relacionada con:** HU-OD-01. **Responsable:** @Braianaguilera.

- Definir la relación entre una cuenta con rol ODONTOLOGO y su ficha profesional/matrícula sin confiar en un ID enviado por el cliente.
- Conservar en el odontograma la identidad profesional que corresponda al JWT y exponerla de forma segura en la respuesta.
- Rechazar asociaciones inexistentes o de otro usuario y agregar pruebas de integración. Esta tarea no debe permitir que RECEPCIONISTA registre ni altere odontogramas.

## Límite académico

Pagos y facturación quedan fuera del alcance. Las recetas no forman parte de este flujo funcional; cualquier dato de demostración debe señalarse como prueba.
