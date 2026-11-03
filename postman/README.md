# Pruebas de API con Postman

## Preparación

1. Inicia MySQL y el backend siguiendo la [guía del backend](../backend/README.md).
2. Importa `Psicoapp.postman_collection.json` en Postman.
3. Importa `Psicoapp.local.postman_environment.json` y selecciónalo como entorno activo.
4. Define `adminPassword`, `teacherPassword` y, si se van a probar endpoints de participación, `studentPassword`.
5. Asegúrate de que `PSICOAPP_CORREO_ADMINISTRADOR` y `PSICOAPP_CLAVE_ADMINISTRADOR` coincidan con `adminEmail` y `adminPassword`.

## Orden recomendado

En la colección, ejecuta las carpetas en este orden:

1. **Identidad**: `Obtener CSRF`, `Crear docente`, `Crear estudiante` y `Habilitar creación de casos para docentes`.
2. **Académico**: crea el espacio y conserva `spaceId`.
3. **Casos**: crea el borrador, publícalo y conserva `caseId`.
4. **Sesiones**: programa la sesión y conserva `sessionId`.

La colección no contiene scripts JavaScript de Postman: las validaciones automatizadas pertenecen al backend Java. Después de cada respuesta, copia manualmente los UUID generados a `teacherId`, `studentId`, `spaceId`, `caseId`, `sessionId` y `attemptId` en el entorno. Postman conserva la cookie `XSRF-TOKEN`; copia también el `token` y `nombreCabecera` de `/api/identidad/csrf` a `csrfToken` y `csrfHeader` antes de las operaciones `POST` y `PUT`.

Los endpoints de **Participación y evaluación** están incluidos para documentar el contrato disponible. Actualmente la configuración de seguridad del backend todavía restringe esas rutas, por lo que pueden responder `403` hasta que se habilite su autorización.

## Repetir pruebas

Las cuentas y los registros se persisten en MySQL. Para repetir el flujo usa correos distintos o limpia los datos de prueba; de lo contrario, algunas operaciones pueden responder `409` por duplicidad.
