# Backlog — Gestión de Personal (Personal, Médicos y Pacientes)

Este documento detalla la planificación, historias de usuario (HU), tareas técnicas (TR), modelos de datos y endpoints para la siguiente etapa de desarrollo: **Gestión Integral de Personal (personal administrativo/auxiliar, profesionales médicos/odontólogos y padrón de pacientes)**.

Todas las issues correspondientes se encuentran dadas de alta y sincronizadas en el repositorio oficial de GitHub: [Proyecto-Houssay/backend-odonto Issues](https://github.com/Proyecto-Houssay/backend-odonto/issues).

---

## 1. Mapa de Historias de Usuario y Tareas Técnicas

| ID HU | Título de Historia de Usuario | Issue GitHub | Tareas Técnicas Asociadas |
|---|---|---|---|
| **HU-12** | [HU-12 - Gestionar personal administrativo y auxiliar](https://github.com/Proyecto-Houssay/backend-odonto/issues/67) | [#67](https://github.com/Proyecto-Houssay/backend-odonto/issues/67) | [TR-31 (#71)](https://github.com/Proyecto-Houssay/backend-odonto/issues/71), [TR-32 (#72)](https://github.com/Proyecto-Houssay/backend-odonto/issues/72), [TR-33 (#73)](https://github.com/Proyecto-Houssay/backend-odonto/issues/73) |
| **HU-13** | [HU-13 - Gestionar profesionales y médicos odontólogos](https://github.com/Proyecto-Houssay/backend-odonto/issues/68) | [#68](https://github.com/Proyecto-Houssay/backend-odonto/issues/68) | [TR-34 (#74)](https://github.com/Proyecto-Houssay/backend-odonto/issues/74), [TR-35 (#75)](https://github.com/Proyecto-Houssay/backend-odonto/issues/75), [TR-36 (#76)](https://github.com/Proyecto-Houssay/backend-odonto/issues/76) |
| **HU-14** | [HU-14 - Gestión avanzada de pacientes y cobertura médica](https://github.com/Proyecto-Houssay/backend-odonto/issues/69) | [#69](https://github.com/Proyecto-Houssay/backend-odonto/issues/69) | [TR-37 (#77)](https://github.com/Proyecto-Houssay/backend-odonto/issues/77), [TR-38 (#78)](https://github.com/Proyecto-Houssay/backend-odonto/issues/78), [TR-39 (#79)](https://github.com/Proyecto-Houssay/backend-odonto/issues/79) |
| **HU-15** | [HU-15 - Directorio unificado de personal y búsqueda clínica](https://github.com/Proyecto-Houssay/backend-odonto/issues/70) | [#70](https://github.com/Proyecto-Houssay/backend-odonto/issues/70) | [TR-40 (#80)](https://github.com/Proyecto-Houssay/backend-odonto/issues/80) |

---

## 2. Detalle de Historias de Usuario

### [#67 HU-12 — Gestionar personal administrativo y auxiliar](https://github.com/Proyecto-Houssay/backend-odonto/issues/67)

**Historia de Usuario:**
- **Como** administrador de la clínica,
- **quiero** registrar, actualizar y administrar el personal administrativo y auxiliar (recepcionistas, asistentes dentales, secretarios, maestranza y técnicos de laboratorio),
- **para** organizar las responsabilidades operativas, horarios laborales y roles dentro del consultorio.

**Criterios de Aceptación:**
- [ ] Registro de personal con datos obligatorios: nombre, apellido, DNI (único), email, teléfono, cargo/puesto, fecha de ingreso y estado (`ACTIVO`/`INACTIVO`).
- [ ] Posibilidad de vincular una cuenta de usuario existente del sistema (`usuarioId`) según su cargo operativo.
- [ ] Actualización de datos de contacto, horario laboral asignado y estado de actividad.
- [ ] Endpoint de consulta paginada y filtrado por cargo y estado.
- [ ] Validación de DNI único en el padrón de personal.
- [ ] Acceso restringido exclusivamente al rol `ADMINISTRADOR`.

---

### [#68 HU-13 — Gestionar profesionales y médicos odontólogos](https://github.com/Proyecto-Houssay/backend-odonto/issues/68)

**Historia de Usuario:**
- **Como** administrador de la clínica,
- **quiero** administrar el cuerpo profesional médico y odontológico, sus especialidades, matrículas y disponibilidad horaria,
- **para** coordinar la agenda médica, asignación de turnos y garantizar la habilitación profesional correspondiente.

**Criterios de Aceptación:**
- [ ] Registro y edición de profesionales con matrícula profesional única y validada.
- [ ] Asignación de una o múltiples especialidades odontológicas o médicas.
- [ ] Configuración de franjas horarias y días de atención semanal para cada profesional.
- [ ] Asignación de consultorio o sillón dental predeterminado.
- [ ] Consulta y filtrado de profesionales por especialidad, matrícula y estado de actividad.
- [ ] Baja lógica del profesional sin comprometer el histórico de turnos ni historias clínicas asociadas.
- [ ] Acceso de lectura para `ADMINISTRADOR`, `ODONTOLOGO` y `RECEPCIONISTA`; alta y modificación exclusiva para `ADMINISTRADOR`.

---

### [#69 HU-14 — Gestión avanzada de pacientes y cobertura médica](https://github.com/Proyecto-Houssay/backend-odonto/issues/69)

**Historia de Usuario:**
- **Como** integrante del equipo de salud (odontólogo, recepcionista o administrador),
- **quiero** gestionar el padrón integral de pacientes con su cobertura médica, contactos de emergencia y alertas clínicas,
- **para** brindar una atención médica segura, personalizada y adaptada a su cobertura de salud.

**Criterios de Aceptación:**
- [ ] Registro y actualización de datos personales y demográficos con DNI único.
- [ ] Asociación de cobertura médica (obra social o prepaga, número de afiliado, plan) o condición de particular.
- [ ] Registro de contacto de emergencia con nombre, parentesco y número telefónico.
- [ ] Registro de alertas médicas críticas: alergias (penicilina, látex, anestésicos), enfermedades crónicas (diabetes, hipertensión, cardiopatías) y condiciones especiales.
- [ ] Búsqueda rápida de pacientes por DNI, apellido o credencial de obra social.
- [ ] Acceso habilitado para roles `ADMINISTRADOR`, `ODONTOLOGO` y `RECEPCIONISTA`.

---

### [#70 HU-15 — Directorio unificado de personal y búsqueda clínica](https://github.com/Proyecto-Houssay/backend-odonto/issues/70)

**Historia de Usuario:**
- **Como** usuario del sistema (administrador, recepcionista u odontólogo),
- **quiero** contar con un directorio centralizado y buscador unificado de personas (personal auxiliar, profesionales médicos y pacientes),
- **para** localizar rápidamente la información de contacto y optimizar la comunicación interna y las derivaciones.

**Criterios de Aceptación:**
- [ ] Buscador global por DNI, nombre, apellido o tipo de persona (`PERSONAL`, `MEDICO`, `PACIENTE`).
- [ ] Respuesta unificada con datos de contacto, rol/tipo y estado dentro de la institución.
- [ ] Filtrado según permisos del rol autenticado (recepcionistas y odontólogos acceden a datos de contacto; administradores acceden al perfil administrativo completo).
- [ ] Documentación OpenAPI Swagger de los esquemas y respuestas de búsqueda.

---

## 3. Detalle de Tareas Técnicas (TR)

| Issue | Tarea Técnica | Descripción y Criterios |
|---|---|---|
| [#71](https://github.com/Proyecto-Houssay/backend-odonto/issues/71) | **TR-31** | **Crear entidad Personal, enum CargoPersonal y PersonalRepository:** Modelado JPA de tabla `personal`, enum `CargoPersonal` (`RECEPCIONISTA`, `ASISTENTE_DENTAL`, `SECRETARIO`, `ADMINISTRATIVO`, `MANTENIMIENTO`, `LABORATORIO`), claves foráneas y restricciones. Pruebas DataJpaTest en H2. |
| [#72](https://github.com/Proyecto-Houssay/backend-odonto/issues/72) | **TR-32** | **Implementar PersonalService con validación de DNI y reglas de negocio:** Capa de servicios para alta, edición, consulta y baja lógica. Validación estricta de unicidad de DNI y consistencia de datos de contacto. Pruebas unitarias en `PersonalServiceTest`. |
| [#73](https://github.com/Proyecto-Houssay/backend-odonto/issues/73) | **TR-33** | **Crear PersonalController con endpoints REST y protección de seguridad:** Rutas `/api/personal` protegidas exclusivamente para rol `ADMINISTRADOR`. Pruebas MockMvc y documentación OpenAPI. |
| [#74](https://github.com/Proyecto-Houssay/backend-odonto/issues/74) | **TR-34** | **Extender modelo Odontologo con disponibilidad horaria, consultorio y estado:** Atributos de consultorio asignado, estado (`ACTIVO`, `LICENCIA`, `INACTIVO`) y entidad/estructura `DisponibilidadHoraria` semanal. |
| [#75](https://github.com/Proyecto-Houssay/backend-odonto/issues/75) | **TR-35** | **Implementar servicio de gestión de disponibilidad horaria y validación de matrículas:** Lógica de validación de matrículas, cálculo de intervalos libres de atención y prevención de solapamientos. Pruebas en `OdontologoServiceTest`. |
| [#76](https://github.com/Proyecto-Houssay/backend-odonto/issues/76) | **TR-36** | **Exponer endpoints de consulta de disponibilidad y filtro por especialidad:** Endpoints en `/api/odontologos/especialidad/{id}` y `/api/odontologos/{id}/disponibilidad`. Seguridad por roles y pruebas WebMvc. |
| [#77](https://github.com/Proyecto-Houssay/backend-odonto/issues/77) | **TR-37** | **Modelar entidades ObraSocial, ContactoEmergencia y alertas médicas en Paciente:** Entidades `ObraSocial`, atributos de afiliación, contacto de emergencia y banderas/lista de alertas médicas críticas. |
| [#78](https://github.com/Proyecto-Houssay/backend-odonto/issues/78) | **TR-38** | **Implementar PacienteService con gestión de cobertura médica y alertas de salud:** Lógica de validación y actualización de coberturas y antecedentes clínicos. Pruebas unitarias con Mockito. |
| [#79](https://github.com/Proyecto-Houssay/backend-odonto/issues/79) | **TR-39** | **Exponer endpoints REST de ficha completa de paciente y búsqueda por DNI:** Rutas de consulta completa `/api/pacientes/{id}`, `/api/pacientes/dni/{dni}` y búsqueda avanzada para `ADMINISTRADOR`, `ODONTOLOGO` y `RECEPCIONISTA`. |
| [#80](https://github.com/Proyecto-Houssay/backend-odonto/issues/80) | **TR-40** | **Implementar servicio y endpoint unificado de directorio y búsqueda de personal:** `DirectorioService` con búsqueda polimórfica y endpoint `GET /api/directorio/buscar` para personal, médicos y pacientes. |

---

## 4. Matriz de Autorización Proyectada

| Recurso / Ruta | `ADMINISTRADOR` | `ODONTOLOGO` | `RECEPCIONISTA` |
|---|:---:|:---:|:---:|
| `/api/personal/**` (Gestión de personal auxiliar) | Alta, Lectura, Edición, Baja | Sin acceso | Sin acceso |
| `/api/odontologos/**` (Alta y configuración de médicos) | Alta, Lectura, Edición, Baja | Solo Lectura | Solo Lectura |
| `/api/odontologos/{id}/disponibilidad` | Configurar y Consultar | Consultar | Consultar |
| `/api/pacientes/**` (Padrón completo y alertas médicas) | Alta, Lectura, Edición | Alta, Lectura, Edición | Alta, Lectura, Edición |
| `/api/directorio/buscar` (Directorio institucional unificado) | Completo (Laboral + Contacto) | Contacto y Profesionales | Contacto y Profesionales |
