# Colección de Postman de Psicoapp

Esta carpeta contiene la colección completa de la API implementada en la rama `develop`:

- `Psicoapp-develop.postman_collection.json`: 53 peticiones organizadas por módulo.
- `Psicoapp-local.postman_environment.json`: entorno local sin credenciales reales.

La colección usa autenticación JWT Bearer. No usa cookies ni CSRF. Al ejecutarla en orden crea datos únicos, guarda tokens e identificadores como variables de colección y recorre autenticación, identidad, espacios académicos, casos, sesiones, participación, evaluación, valoraciones, reportes y notificaciones.

## Preparación

1. Cree `.env` en la raíz a partir de `.env.example` y configure MySQL.
2. Defina `PSICOAPP_CORREO_ADMINISTRADOR`, `PSICOAPP_CLAVE_ADMINISTRADOR` y al menos un dominio en `PSICOAPP_DOMINIOS_INSTITUCIONALES`.
3. Inicie el backend en el puerto 8080.
4. Importe los dos archivos JSON en Postman y seleccione el entorno **Psicoapp - local (develop)**.
5. Complete `adminEmail` y `adminPassword`. Ajuste `institutionalDomain` para que coincida con la configuración del backend.

La contraseña del docente de prueba es un valor ficticio del entorno. Puede cambiarla antes de ejecutar la colección. Los correos del docente y del estudiante incorporan una marca de tiempo para evitar colisiones entre ejecuciones.

## Ejecución

Use el Runner con una iteración y conserve el orden de las carpetas. La primera petición reinicia los UUID, correos y fechas del recorrido. La sesión se programa desde un minuto antes de la ejecución hasta una hora después para que pueda iniciarse inmediatamente.

También puede ejecutarla con Newman desde la raíz:

```powershell
npx.cmd --yes newman run docs/pruebas/postman/Psicoapp-develop.postman_collection.json `
  -e docs/pruebas/postman/Psicoapp-local.postman_environment.json `
  --env-var "adminEmail=administrador@universidad.edu.co" `
  --env-var "adminPassword=SU_CLAVE" `
  --env-var "institutionalDomain=universidad.edu.co" `
  --bail
```

La ejecución deja los registros creados en la base de datos; no realiza borrado ni limpieza.

## Operaciones especiales

La carpeta **09 - Operaciones manuales** conserva las dos rutas cuyo estado no surge de forma natural en el recorrido feliz:

- `PUT /api/sesiones/{id}/convocados` requiere asignar `additionalStudentId` a otro estudiante activo y ejecutarse antes del cierre de la sesión.
- `POST /api/participaciones/{id}/reanudar` requiere un intento suspendido. Sobre el intento finalizado del recorrido devuelve `409`, respuesta esperada y documentada.

Marcar una notificación como leída acepta `404` cuando el despacho interno está deshabilitado o todavía no procesó el aviso. Para probar el `204`, habilite `PSICOAPP_NOTIFICACIONES_DESPACHO`, use un intervalo corto y vuelva a listar las notificaciones antes de marcar una.

## Regeneración

Los JSON se generan de forma determinista a partir de `tools/generar-coleccion-postman.mjs`:

```powershell
node tools/generar-coleccion-postman.mjs
```

El generador no incorpora contraseñas administrativas ni tokens en los archivos versionados.
