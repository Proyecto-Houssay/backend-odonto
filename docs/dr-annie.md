# Dr. Annie — Diagnóstico por Imagen con IA

Dr. Annie (`Novadent DentalVision AI v2.4`) es el asistente de análisis de estudios radiográficos y fotográficos dentales del consultorio.

## Endpoints REST (`/api/ia/diagnostico-imagen`)

- `GET /api/ia/diagnostico-imagen/catalogo`: Devuelve las modalidades de imagen soportadas, patologías detectables, niveles de urgencia, versión del modelo y estado del servicio (`OPERATIVO`).
- `POST /api/ia/diagnostico-imagen/analizar`: Procesa una imagen dental en Base64 (`AnalisisImagenRequest`), detecta hallazgos según la modalidad seleccionada, calcula la urgencia y el índice de severidad, y opcionalmente registra el diagnóstico y tratamientos sugeridos en la historia clínica del paciente (`guardarEnHistoria: true`).
- `POST /api/ia/diagnostico-imagen/upload`: Procesa un archivo `multipart/form-data` y ejecuta el análisis.
- `GET /api/ia/diagnostico-imagen/paciente/{pacienteId}`: Lista el historial de estudios analizados por paciente.
- `GET /api/ia/diagnostico-imagen/{id}`: Obtiene el detalle de un estudio analizado por ID.

## Modalidades soportadas

- `RADIOGRAFIA_PANORAMICA`
- `RADIOGRAFIA_PERIAPICAL`
- `BITE_WING`
- `FOTO_INTRAORAL`
- `TOMOGRAFIA_CONE_BEAM`
