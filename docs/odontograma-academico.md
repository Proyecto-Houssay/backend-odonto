# Odontograma académico: guardar y comprobar un presupuesto firmado

El módulo guarda una **copia inmutable** de las piezas evaluadas, los importes ilustrativos y el consentimiento firmado de un paciente existente. No registra pagos, no diagnostica automáticamente y no crea diagnósticos ni tratamientos en las historias clínicas generales.

## Prueba manual rápida

1. Iniciar el backend con su configuración habitual y actualizar Swagger tras reiniciarlo. No es necesario modificar Docker.
2. En Swagger, ejecutar `POST /api/auth/login` con un usuario existente `ODONTOLOGO` o `ADMINISTRADOR`. Copiar el JWT y usar **Authorize** (`bearerAuth`). No confundir la contraseña del usuario con `MYSQL_PASSWORD`.
3. Consultar `GET /api/pacientes` y elegir un paciente de demostración existente. La pantalla no crea ni duplica pacientes.
4. En el frontend local, iniciá sesión con una cuenta del backend y abrí el odontograma desde un paciente existente. La sesión agrega el JWT automáticamente; guardar requiere rol ODONTOLOGO o ADMINISTRADOR. La ruta de demostración no persiste.
5. Esperar **Catálogo verificado**. Seleccionar una pieza y asignar una afección. La selección sola deja la pieza **Sin evaluar**; **Sano** también debe asignarse explícitamente. Revisar el resumen, el tratamiento y el total.
6. Aceptar el consentimiento, dibujar con mouse o con el dedo, **Capturar firma** y comprobar la vista previa. **Limpiar firma** elimina tanto el trazo como el PNG. Cambiar paciente, marcas o consentimiento revoca la firma; seleccionar otra pieza no la revoca. Un cambio real de tamaño del canvas exige volver a aceptar y firmar.
7. Ejecutar **Guardar odontograma**. Solo **Guardado #ID** confirma una respuesta HTTP 201 válida. Ante errores, el borrador permanece; si la respuesta se perdió, consultar la API antes de reintentar para no duplicar una copia ya creada.
8. Ejecutar `GET /api/odontograma/ID` y comparar dientes, precios, total, consentimiento y firma. `GET /api/odontograma?pacienteId=ID` devuelve resúmenes sin el PNG.
9. Ejecutar **Descargar PDF con Firma** para obtener `presupuesto_paciente_ID.pdf`. Revisar título, fecha, todas las piezas marcadas, total ARS, consentimiento y el bloque **Firma del paciente:**. Sin guardado confirmado, lleva **BORRADOR NO GUARDADO**; un PDF por sí solo NO demuestra persistencia.

## Comprobar MySQL sin modificar datos

Conectarse en Workbench al mismo esquema que usa el backend y reemplazar los ID por los de la prueba. No ejecutar estas consultas en una base con datos clínicos ajenos.

```sql
SELECT id, paciente_id, creado_en, creado_por_id, moneda,
       version_catalogo, total_presupuesto, version_consentimiento,
       consentimiento_aceptado, CHAR_LENGTH(firma_base64) AS firma_caracteres
FROM odontograma
WHERE id = 15 AND paciente_id = 7;

SELECT numero, afeccion, nombre, color, descripcion, tratamiento, costo_estimado
FROM odontograma_diente
WHERE odontograma_id = 15
ORDER BY orden;
```

Las pruebas automatizadas usan H2, no MySQL. El `201` y la lectura por GET demuestran el contrato de la API; la consulta anterior comprueba el almacenamiento en el esquema MySQL elegido. El paciente con un odontograma no puede eliminarse (`409`). No hay actualización ni eliminación de copias de odontograma.

### Prueba local de persistencia (sin tocar otros registros)

1. Antes de iniciar el backend, revisar `MYSQL_HOST`, `MYSQL_PORT` y `MYSQL_DATABASE` en `.env`. Conectar Workbench a ese mismo host y puerto, seleccionar allí el esquema indicado por `MYSQL_DATABASE` y ejecutar `SELECT @@port AS puerto, DATABASE() AS esquema;` para comprobar la conexión. El puerto no debe suponerse: depende de la configuración local.
2. Iniciar el backend y usar `GET /api/health` para verificar que responde. Iniciar sesión en Swagger con una cuenta `ODONTOLOGO` o `ADMINISTRADOR` y autorizar su JWT; no usar la contraseña de MySQL como contraseña de Swagger.
3. Elegir un paciente de prueba ya existente, guardar un odontograma con **una sola pieza** y anotar el `id` recibido en la respuesta `201`. Consultar ese mismo ID con `GET /api/odontograma/{id}` y verificar `pacienteId`, `creadoPorId`, pieza, total y firma.
4. En Workbench, ejecutar las dos consultas anteriores reemplazando `15` por el ID nuevo y `7` por el paciente elegido. Debe aparecer una fila en `odontograma` y una fila por pieza en `odontograma_diente`. Si no aparece, comprobar que Workbench y el backend apuntan al mismo host, puerto y esquema antes de repetir el `POST`.
5. Iniciar sesión con una cuenta `RECEPCIONISTA` y comprobar que puede consultar pacientes o turnos y que los tres `GET` del odontograma responden `200`. El detalle conserva dientes, afecciones, presupuesto y consentimiento, pero **no incluye `firmaBase64`**. `POST /api/odontograma` devuelve `403`; sin token, los `GET` devuelven `401`.

Esta comprobación crea únicamente el odontograma de prueba del paso 3. No borrar ni modificar registros clínicos existentes para hacerla.

## Contrato de API

Todos los endpoints requieren JWT. `ODONTOLOGO` y `ADMINISTRADOR` pueden leer y registrar; `RECEPCIONISTA` solo puede leer los endpoints `GET` del odontograma. Sin token: `401`; recepción al intentar registrar: `403`.

| Rol autenticado | Pacientes y turnos | Catálogo y consultas de odontogramas | Alta de odontogramas |
|---|---|---|
| `ADMINISTRADOR` | Permitido | Permitido | Permitido |
| `ODONTOLOGO` | Permitido | Permitido | Permitido |
| `RECEPCIONISTA` | Permitido | Permitido | `403` |

Los tres roles pueden iniciar sesión mediante `POST /api/auth/login`. **Alcance del acceso de recepción:** `GET /api/odontograma/{id}` devuelve los datos clínicos ilustrativos y del consentimiento, pero omite `firmaBase64`; la firma PNG se entrega únicamente a `ODONTOLOGO` y `ADMINISTRADOR`. La restricción se aplica en el servidor según el rol del JWT, no según lo que oculte el frontend. Recepción no puede dar de alta, actualizar ni eliminar odontogramas, ni acceder a historias clínicas, diagnósticos o tratamientos. El token vence a los 15 minutos: ante un `401`, volver a iniciar sesión y reemplazar el token en Swagger o en el frontend.

| Operación | Resultado |
|---|---|
| `GET /api/odontograma/catalogo` | `versionCatalogo`, `versionConsentimiento`, `textoConsentimiento`, `afecciones` |
| `POST /api/odontograma` | `201` + `Location: /api/odontograma/{id}` + copia completa |
| `GET /api/odontograma/{id}` | Copia con firma PNG para odontólogo y administrador; sin campo `firmaBase64` para recepción |
| `GET /api/odontograma?pacienteId={id}` | Resúmenes sin firma ni colección de dientes |

Solicitud de ejemplo (reemplazar `firmaBase64` por el PNG real capturado, no enviar los puntos suspensivos):

```json
{
  "pacienteId": 7,
  "dientes": [{ "numero": 11, "afeccion": "caries" }],
  "consentimientoAceptado": true,
  "versionConsentimiento": "academic-v1",
  "firmaBase64": "data:image/png;base64,..."
}
```

El servidor calcula los campos clínicos ilustrativos, colores, precios y total desde el catálogo `academic-v1`; no confía en importes del cliente. Registra el texto de consentimiento vigente, hora del servidor y `userId` del JWT. Se rechazan números FDI inválidos, dientes duplicados, códigos desconocidos o con mayúsculas, consentimiento desactualizado y PNG corruptos o mayores a 256 KiB decodificados, 2048 píxeles por lado o un millón de píxeles. La validación estructural y de dimensiones precede a la decodificación del raster.

La respuesta completa para odontólogo y administrador incluye `id`, `pacienteId`, `creadoEn`, `creadoPorId`, `moneda`, `versionCatalogo`, `dientes`, `totalPresupuesto`, `consentimientoAceptado`, `versionConsentimiento`, `textoConsentimiento` y `firmaBase64`. La respuesta de detalle para recepción omite `firmaBase64`. Cada diente contiene `numero`, `afeccion`, `nombre`, `color`, `descripcion`, `tratamiento` y `costoEstimado`. Los importes son enteros ARS; la firma es una captura manuscrita electrónica ilustrativa, sin afirmaciones de validez jurídica.

## Desarrollo y verificación

Desde backend: `.\mvnw.cmd test`. Desde frontend: `npm test`, `npm run build`, `npm run lint`.

Vite redirige `/api` a `http://localhost:8080`. Para un backend aislado de pruebas en otro puerto, iniciar Vite con `$env:ODONTO_API_TARGET='http://localhost:18080'; npm run dev -- --port 15173`. Esto no modifica `.env` ni Docker. Un despliegue de producción necesita su propio reverse proxy `/api`; el proxy de Vite es solo de desarrollo.

`Odontograma` también se puede importar sin router: acepta `pacienteId`, `token` y `apiUrl` (por defecto `/api`). El JWT permanece exclusivamente en memoria. La aplicación verifica la paridad exacta del catálogo y consentimiento con el servidor antes de permitir firmar; no guarda datos en `localStorage`.

Límite de reversión: módulo `odontogram` y sus pruebas; guardia de eliminación de pacientes y matcher de seguridad; `Odontograma.jsx`, sus pruebas, rutas y enlace de menú; dependencias y configuración de pruebas/proxy; esta guía y enlaces README. No afecta la propiedad del módulo de historias clínicas.
