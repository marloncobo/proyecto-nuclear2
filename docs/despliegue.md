# Despliegue provisional: Railway y Firebase

La arquitectura prevista usa un servicio Spring Boot y una base MySQL dentro del
mismo proyecto de Railway. El navegador carga el frontend estático desde Firebase
Hosting y llama directamente a la URL HTTPS pública del backend. La base de datos
permanece privada dentro de Railway.

## 1. Backend y MySQL en Railway

1. Crear un proyecto y agregar una base **MySQL**.
2. Crear un servicio desde este repositorio y configurar su **Root Directory** como
   `/backend`. Railway detectará `backend/Dockerfile` sin un comando de inicio manual.
3. En el servicio backend, agregar las variables de `.env.example`. Para la conexión
   usar referencias al servicio MySQL (si tiene otro nombre, sustituir `MySQL`):

   ```text
   PSICOAPP_BD_URL=jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&sslMode=DISABLED&allowPublicKeyRetrieval=true
   PSICOAPP_BD_USUARIO=${{MySQL.MYSQLUSER}}
   PSICOAPP_BD_CLAVE=${{MySQL.MYSQLPASSWORD}}
   PSICOAPP_SQL_INICIALIZACION=always
   ```

   No hace falta exponer MySQL mediante TCP público. Las referencias usan la red
   privada de Railway.

4. Definir valores de producción para estas variables críticas:

   ```text
   PSICOAPP_NOMBRE_APLICACION=psicoapp
   PSICOAPP_DOMINIOS_INSTITUCIONALES=universidad.edu.co
   PSICOAPP_CORREO_ADMINISTRADOR=administrador@universidad.edu.co
   PSICOAPP_CLAVE_ADMINISTRADOR=<clave inicial robusta>
   PSICOAPP_JWT_CLAVE=<Base64 aleatorio de 32 bytes o más>
   PSICOAPP_JWT_CLAVE_REQUERIDA=true
   PSICOAPP_NOTIFICACIONES_CLAVE_CIFRADO=<Base64 aleatorio de exactamente 32 bytes>
   PSICOAPP_NOTIFICACIONES_URL_ACCESO=https://<proyecto>.web.app/ingreso
   PSICOAPP_CORS_ORIGENES=https://<proyecto>.web.app,https://<proyecto>.firebaseapp.com
   ```

   Completar también las variables de participación, sesiones, notificaciones y
   correo indicadas en `.env.example`. Si todavía no se enviará correo, conservar
   `PSICOAPP_NOTIFICACIONES_DESPACHO=false`; no habilitarlo con credenciales vacías.
   Las claves JWT y de cifrado deben ser distintas y persistentes. Para generar cada
   una en PowerShell:

   ```powershell
   $bytesPsicoapp = [byte[]]::new(32)
   [Security.Cryptography.RandomNumberGenerator]::Fill($bytesPsicoapp)
   [Convert]::ToBase64String($bytesPsicoapp)
   ```

5. Crear el dominio público HTTP del backend. En **Healthcheck Path** configurar
   `/actuator/health` y usar un timeout de 120 segundos. La aplicación escucha el
   `PORT` asignado por Railway y el healthcheck incluye la disponibilidad de MySQL.
6. Verificar primero `https://<backend>.up.railway.app/actuator/health`; debe responder
   `{"status":"UP"}`. Después comprobar que un `POST /api/identidad/ingreso` válido
   funciona. Una respuesta 401 en rutas protegidas sin token es normal.

El esquema actual se crea de forma idempotente con `schema.sql`. Antes de cambios
estructurales posteriores conviene adoptar una herramienta de migraciones (Flyway o
Liquibase) y activar copias de seguridad en Railway.

## 2. Frontend en Firebase Hosting

El archivo `public/configuracion.js` mantiene `/api` en el mismo origen durante el
desarrollo. El build de Firebase reemplaza esa configuración en `dist` con la URL
pública de Railway, sin guardar el dominio de un entorno dentro del código fuente.

Desde `frontend/`:

```powershell
npm ci
npm install -g firebase-tools
firebase login
firebase use --add
$env:PSICOAPP_API_URL = 'https://<backend>.up.railway.app'
npm run build:firebase
firebase deploy --only hosting
```

`PSICOAPP_API_URL` debe ser un origen HTTPS, sin `/api`, ruta ni barra final. El
directorio publicado es `dist/psicoapp/browser`; `firebase.json` aplica fallback de
SPA hacia `index.html` y evita cachear `configuracion.js`.

Después del primer despliegue, copiar ambos orígenes asignados por Firebase a
`PSICOAPP_CORS_ORIGENES` en Railway y volver a desplegar el backend. Si se añade un
dominio personalizado, agregar su origen exacto a esa lista y usarlo también en
`PSICOAPP_NOTIFICACIONES_URL_ACCESO`.

## 3. Comprobación posterior

- Abrir directamente `/ingreso` y otra ruta interna para comprobar el fallback SPA.
- Iniciar sesión con la cuenta administrativa y cerrar sesión.
- Confirmar en las herramientas del navegador que las solicitudes van a Railway por
  HTTPS y que no hay errores CORS ni contenido mixto.
- Crear y consultar un registro de prueba, reiniciar el backend y confirmar que el
  dato persiste.
- Mantener MySQL sin acceso público, sellar los secretos en Railway y activar backups
  antes de usar datos reales.
