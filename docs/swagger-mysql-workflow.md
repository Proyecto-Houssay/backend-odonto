# Probar escrituras de Swagger en MySQL

Utiliza Swagger para enviar una solicitud autenticada y después comprueba el registro en la misma base MySQL configurada para el backend. Las pruebas automatizadas usan H2 en memoria: sirven para validar el comportamiento, pero no demuestran que una fila se haya guardado en MySQL.

## Camino rápido: crear un paciente

1. **Inicia MySQL** con la configuración local ya preparada. No compartas los valores de `.env`.
2. **Inicia el backend desde la raíz de `backend-odonto`**, donde está `mvnw.cmd`:

   ```powershell
   cmd /c mvnw.cmd spring-boot:run
   ```

   Espera a que Spring Boot indique que Tomcat inició en el puerto `8080` y que se estableció la conexión a MySQL.

3. Abre [Swagger UI](http://localhost:8080/swagger-ui/index.html).
4. En `POST /api/auth/login`, selecciona **Try it out** y envía las credenciales del administrador existente:

   ```json
   {
     "usernameOrEmail": "<usuario-admin>",
     "password": "<contraseña-local>"
   }
   ```

   La respuesta exitosa incluye un JWT en `token`. Si todavía no existe ningún administrador, sigue una sola vez la sección [Alta inicial del administrador](../README.md#alta-inicial-del-administrador); no hay un registro público.

5. Selecciona **Authorize**. Pega el valor de `token` (sin agregar `Bearer ` si Swagger solicita solamente el token) y confirma. Swagger agregará el encabezado `Authorization: Bearer ...` a las operaciones protegidas.
6. En `POST /api/pacientes`, selecciona **Try it out** y utiliza datos ficticios. El DNI debe ser único en la base:

   ```json
   {
     "nombre": "Paciente",
     "apellido": "De prueba",
     "dni": "99900001",
     "telefono": "1123456789",
     "email": "paciente.prueba@example.com",
     "fechaNacimiento": "1990-05-20"
   }
   ```

   La fecha debe tener formato `YYYY-MM-DD` y no ser futura. Al crearse, el endpoint responde `200`. Un DNI duplicado responde `409`; datos inválidos, `400`.

## Verificar el registro en MySQL Workbench

Conéctate al mismo servidor, puerto y esquema configurados para el backend; elige como esquema predeterminado el valor correspondiente a `MYSQL_DATABASE` en tu configuración local. No hace falta copiar credenciales a este documento.

Después de ejecutar el `POST`, busca el DNI de prueba:

```sql
SHOW TABLES LIKE 'paciente';

SELECT id, nombre, apellido, dni, telefono, email, fecha_nacimiento
FROM paciente
WHERE dni = '99900001';
```

La entidad `Paciente` no declara un nombre de tabla explícito; con la estrategia de nombres predeterminada de Spring Boot/Hibernate, la tabla es `paciente`. Si la consulta no devuelve una fila, confirma que Workbench está conectado al mismo esquema que usa el backend y que la solicitud terminó con éxito.

## Por qué algunas operaciones piden autorización

La autenticación confirma quién realiza la solicitud; el rol determina qué operaciones puede hacer. Es una protección para que una cuenta anónima no pueda crear usuarios, asignar roles o administrar información sensible.

| Operaciones | Quién puede usarlas |
|---|---|
| `POST /api/auth/login`, `GET /api/health` | Públicas. |
| Pacientes y turnos | Requieren JWT de `ADMINISTRADOR`, `ODONTOLOGO` o `RECEPCIONISTA`. |
| Historias clínicas, diagnósticos y tratamientos | Requieren JWT de `ADMINISTRADOR` u `ODONTOLOGO`. |
| `POST /api/auth/change-password` | Cualquier usuario autenticado, solo para su propia contraseña. |
| Lectura de odontólogos y especialidades | Requiere JWT de cualquiera de esos tres roles. |
| Crear, modificar o borrar odontólogos y especialidades | Requiere JWT de `ADMINISTRADOR`. |
| Usuarios e informes | Requieren JWT de `ADMINISTRADOR`. |

Para probar `POST /api/pacientes`, sirve cualquiera de esos tres roles. Para crear cuentas o usar informes, inicia sesión con un administrador. Si aún no existe uno, utiliza el bootstrap documentado en el README; luego desactiva ese perfil y las variables correspondientes, como indica la guía.

## Probar la ficha clínica con datos ficticios

1. Consultá `GET /api/pacientes` para obtener el `id` del paciente de prueba. Con token de `ADMINISTRADOR` u `ODONTOLOGO`, ejecutá `POST /api/historias-clinicas` con `{"pacienteId": ID, "fechaApertura": "2026-10-14", "observaciones": "Control de prueba"}`. Guardá el `id` de la respuesta `201`.
2. Ejecutá `POST /api/diagnosticos` con `{"historiaClinicaId": ID_HISTORIA, "fecha": "2026-10-14", "descripcion": "Diagnóstico de prueba"}`. Guardá su `id`.
3. Ejecutá `POST /api/tratamientos` con `{"diagnosticoId": ID_DIAGNOSTICO, "descripcion": "Tratamiento de prueba", "fecha": "2026-10-14", "estado": "PENDIENTE"}`. Verificá con los `GET` de esas rutas. Una segunda historia para el mismo paciente devuelve `409`.
4. En Workbench, con el mismo esquema, ejecutá las consultas siguientes sustituyendo `ID` por el ID numérico del paciente:

   ```sql
   SELECT id, paciente_id, fecha_apertura, observaciones FROM historias_clinicas WHERE paciente_id = ID;
   SELECT id, historia_clinica_id, fecha, descripcion FROM diagnosticos WHERE historia_clinica_id = ID_HISTORIA;
   SELECT id, diagnostico_id, fecha, descripcion, estado FROM tratamientos WHERE diagnostico_id = ID_DIAGNOSTICO;
   ```

Para cambiar tu propia contraseña, `POST /api/auth/change-password` recibe `{"currentPassword":"<clave-actual>","newPassword":"<clave-nueva>"}` con JWT. Devuelve `204`; iniciá sesión de nuevo con la clave nueva. No publiques claves ni tokens de prueba.

## Seguridad y alcance de las pruebas

- No subas ni compartas `.env`, contraseñas ni JWT. No pegues tokens en capturas, issues o chats.
- No uses datos personales reales para pruebas; elige un DNI ficticio y único.
- No ejecutes `docker compose down -v`: elimina el volumen local de MySQL y sus datos de prueba.
- Los tests configuran H2 en memoria y se limpian al terminar. La persistencia real se confirma haciendo el `POST` con el backend conectado a MySQL y consultando la fila en Workbench.

El proyecto académico no expone pagos, cobros ni ganancias en Swagger. Las recetas no forman parte de la API normal; se reservan para pruebas.
