# Portal del paciente y seguimiento por pieza

## Alcance

La cuenta pública ahora tiene rol `PACIENTE`, nunca `RECEPCIONISTA`. Se separan las credenciales (`usuarios`) de la ficha clínica (`paciente`): `usuarios.paciente_id` es una relación opcional y única. Un paciente solo puede consultar su propia ficha y sus reportes. El odontólogo y el administrador consultan la bandeja y registran una revisión inmutable por reporte; recepción no accede a fotos ni reportes. Cada reporte nuevo del mismo diente FDI agrega un punto a la línea de tiempo. El odontograma firmado existente no se modifica.

Este módulo **no diagnostica automáticamente**, no recomienda esperar ni acudir a guardia, no estima porcentajes de cambio por fotografías y no llama a proveedores de IA. Las tres tarjetas ficticias del frontend son demostraciones explícitas, no historias clínicas. Los reportes son declaraciones del paciente; la revisión profesional es separada.

## API

| Rol | Ruta | Uso |
|---|---|---|
| Público | `POST /api/auth/register` | Crea cuenta `PACIENTE`, independientemente del `rol` enviado. |
| Paciente | `GET /api/portal-paciente/perfil` | Devuelve su ficha, o `404` si falta. |
| Paciente | `POST /api/portal-paciente/perfil` | Crea su ficha con `dni`, `fechaNacimiento`, `telefono` opcional; nombre y correo provienen de la cuenta. Si el DNI ya existe, `409`; debe solicitar vínculo administrativo. |
| Paciente | `PUT /api/portal-paciente/perfil` | Actualiza solo `telefono`; DNI y fecha de nacimiento no se cambian desde autoservicio. |
| Paciente | `GET/POST /api/portal-paciente/reportes` | Lista propios o crea un reporte: `pieza` FDI, `dolor` 0–10, `sintomas` y `fotoBase64` opcional. |
| Paciente | `GET /api/portal-paciente/reportes/{id}` | Detalle propio; otro paciente obtiene `404`. |
| Odontólogo/administrador | `GET /api/seguimiento/reportes?pacienteId={id}` | Bandeja resumida, sin foto; filtro opcional. |
| Odontólogo/administrador | `GET /api/seguimiento/reportes/{id}` | Detalle y foto del reporte. |
| Odontólogo/administrador | `PUT /api/seguimiento/reportes/{id}/revision` | `{"revisionProfesional":"..."}`; la segunda revisión responde `409`. |
| Administrador | `PUT /api/admin/vinculos-paciente/{userId}` | `{"pacienteId":7,"dniConfirmado":"..."}`; requiere verificar identidad fuera del sistema antes de usarlo. |

Si se envía foto, `consentimientoFoto` debe ser `true`. Se aceptan PNG/JPEG reales de hasta 1 MB y 2048 píxeles por lado. El servidor decodifica y vuelve a codificar como PNG, eliminando metadatos, y limita el resultado a 2 MB. El detalle entrega la foto solo al dueño o a odontólogo/administrador; la bandeja no entrega bytes de imagen. No subir imágenes reales de pacientes a una demo sin autorización y protección adecuadas.

## Prueba local

1. Verificar `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_DATABASE` en el `.env` privado del backend. No asumir que Workbench y Spring Boot usan la misma instancia solo porque ambos se inician.
2. Ejecutar backend y frontend como indica cada README. Crear una **cuenta ficticia nueva** desde `Crear una cuenta` e iniciar sesión.
3. Completar DNI ficticio y fecha de nacimiento. En el portal, señalar una pieza, escribir síntomas, enviar un reporte y abrirlo en su línea de tiempo. Se puede adjuntar una imagen PNG/JPEG de prueba, aceptando el consentimiento explícito.
4. Cerrar sesión. Ingresar como `ODONTOLOGO` o `ADMINISTRADOR`, abrir `Seguimiento de pacientes`, seleccionar el reporte, revisar su información y registrar una nota. Un segundo intento de revisión debe responder `409` en la API.
5. Volver a ingresar como el paciente y comprobar que ve la nota y sus propios reportes. Otro paciente no debe poder abrir el detalle por ID; una cuenta `RECEPCIONISTA` no debe abrir `/api/seguimiento/**`.
6. En Workbench, sobre el mismo esquema, comprobar `SELECT id, username, rol, paciente_id FROM usuarios WHERE username = 'USUARIO_DE_PRUEBA';` y `SELECT id, paciente_id, pieza, dolor, creado_en, revisado_en FROM reporte_dental ORDER BY id DESC LIMIT 10;`. No consultar ni copiar la columna `foto_base64` en capturas.

Los tests automáticos usan H2, no prueban el MySQL local. Hibernate `ddl-auto=update` crea la tabla nueva e intenta agregar `usuarios.paciente_id` durante el arranque local; **verificar** en Workbench que exista la restricción única de esa columna antes de dar por validada la instalación. No aplicar SQL de migración a ciegas sobre datos existentes. Las cuentas creadas públicamente antes de este cambio conservarán el rol `RECEPCIONISTA`; el administrador debe revisar cada cuenta antes de cambiarla a `PACIENTE` y vincular una ficha.
