# Prueba del backend en Vercel

## Configuración del proyecto

Conectá el repositorio GitHub `Proyecto-Houssay/backend-odonto` a Vercel y elegí `backend-odonto` como raíz del proyecto. El despliegue de la rama `Braian` debe quedar como **Preview**. Vercel detecta `Dockerfile.vercel`, compila el backend y publica una URL temporal. El contenedor lee el puerto dinámico `PORT`.

No uses el servicio MySQL de `docker-compose.yml` como base de Vercel: ese servicio solo es accesible dentro del entorno local. La aplicación necesita una instancia MySQL externa cuya dirección acepte conexiones desde Vercel.

## Variables para el entorno Preview

Cargá estas variables en la configuración de Vercel, dentro del entorno **Preview**:

- `MYSQL_HOST`
- `MYSQL_PORT`
- `MYSQL_DATABASE`
- `MYSQL_USER`
- `MYSQL_PASSWORD`
- `JWT_SECRET_BASE64`

Usá los datos de conexión del proveedor MySQL. No configures `MYSQL_ROOT_PASSWORD`: solo lo necesita el contenedor MySQL local. No copies el archivo `.env` al repositorio ni pegues valores secretos en el chat.

`JWT_SECRET_BASE64` debe ser Base64 que decodifique a 32 bytes como mínimo. Podés generar uno localmente con PowerShell:

```powershell
$bytes = New-Object byte[] 32
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
[Convert]::ToBase64String($bytes)
$rng.Dispose()
Remove-Variable bytes
```

Guardá el resultado directamente como variable secreta de Vercel.

## Primer usuario administrador

Si la base de prueba todavía no tiene administrador, la aplicación permite un alta inicial controlada. Para ese primer despliegue Preview, agregá temporalmente:

- `SPRING_PROFILES_ACTIVE=bootstrap-admin`
- `BOOTSTRAP_ADMIN_USERNAME`
- `BOOTSTRAP_ADMIN_EMAIL`
- `BOOTSTRAP_ADMIN_PASSWORD`

Después de que el primer despliegue haya iniciado y creado el administrador, quitá esas cuatro variables del entorno Preview y volvé a desplegar. No habilites el perfil de bootstrap para el uso normal.

## Comprobación

Cuando el despliegue figure como **Ready**, comprobá:

- `GET /api/health`
- `GET /swagger-ui/index.html`
- `POST /api/auth/login` con el administrador de prueba

Usá datos ficticios durante la prueba. La aplicación web/frontend requiere su propio despliegue y todavía necesita integración con la API para mostrar el sistema completo.