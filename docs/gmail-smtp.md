# SMTP de Gmail para recuperación local de cuentas

El backend puede enviar avisos de registro y enlaces de restablecimiento de contraseña de un solo uso mediante Gmail. La configuración es local y opcional: no subas la dirección remitente ni la contraseña de aplicación a Git, no las muestres en capturas y no las pongas en el frontend.

## Configuración rápida

1. En la cuenta de Google remitente, activá la [verificación en dos pasos](https://support.google.com/accounts/answer/185839). Después creá una [contraseña de aplicación](https://support.google.com/accounts/answer/185833) para este proyecto local. Usá esa contraseña, **no** la contraseña habitual de Google. Algunas cuentas administradas o con Protección Avanzada no permiten crearla.
2. Abrí `backend-odonto/.env` en tu equipo. Completá estas claves sin compartir la dirección real ni la contraseña de aplicación:

   ```dotenv
   APP_MAIL_ENABLED=false
   MAIL_HOST=smtp.gmail.com
   MAIL_PORT=587
   MAIL_USERNAME=remitente@gmail.com
   MAIL_PASSWORD=contraseña-de-aplicación-de-16-caracteres
   MAIL_FROM=remitente@gmail.com
   MAIL_SMTP_AUTH=true
   MAIL_SMTP_STARTTLS_ENABLE=true
   MAIL_SMTP_STARTTLS_REQUIRED=true
   APP_FRONTEND_BASE_URL=http://localhost:5173
   ```

3. Si Vite muestra otro puerto, por ejemplo `5174`, cambiá `APP_FRONTEND_BASE_URL` para que coincida. Cuando completes los valores de Gmail, poné `APP_MAIL_ENABLED=true` y reiniciá el backend. Las variables de entorno que ya tenga la terminal tienen prioridad sobre `.env`.
4. En el frontend, elegí **¿Olvidaste tu contraseña?**, ingresá el usuario o correo y enviá la solicitud. Revisá la bandeja de entrada y el correo no deseado de la dirección registrada. El enlace vence a los 30 minutos y solo se puede usar una vez. La respuesta es la misma para cuentas existentes e inexistentes; nunca revela el correo almacenado.

## Si olvidaste la dirección de correo

- Si recordás usuario y contraseña, iniciá sesión con el usuario. Para restablecer la contraseña también podés ingresar el usuario: el enlace se enviará únicamente al correo registrado en esa cuenta.
- Si ya no tenés acceso a ese buzón, un administrador debe verificar tu identidad y actualizar el correo de la cuenta. El formulario público no debe mostrar ni cambiar la dirección de otra persona.

Una respuesta `emailConfigured: true` indica que están presentes los ajustes locales, **no** que Gmail haya aceptado o entregado el mensaje. Revisá la bandeja y el registro del backend. Si cambiás la contraseña de Google, la contraseña de aplicación puede revocarse y requerir una nueva.
