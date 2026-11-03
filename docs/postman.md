# Pruebas de Psicoapp con Postman

Esta guía documenta la configuración y el flujo probado de la API Java de
Psicoapp usando Postman Web y el Desktop Agent.

## Archivos de Postman

- Colección: [`postman/Psicoapp.postman_collection.json`](../postman/Psicoapp.postman_collection.json)
- Entorno local: [`postman/Psicoapp.local.postman_environment.json`](../postman/Psicoapp.local.postman_environment.json)
- Guía breve: [`postman/README.md`](../postman/README.md)

Importa ambos archivos en Postman Web y selecciona el entorno **Psicoapp -
local**. Para acceder a `localhost`, selecciona **Desktop Agent**, no Cloud
Agent.

## Requisitos

1. MariaDB/MySQL activo y la base `psicoapp` disponible.
2. Backend Java iniciado desde `backend/`:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

3. API disponible en `http://localhost:8080`.
4. Credenciales del administrador configuradas en el entorno de Postman y en
   las variables del backend:

   - `PSICOAPP_CORREO_ADMINISTRADOR`
   - `PSICOAPP_CLAVE_ADMINISTRADOR`
   - `PSICOAPP_DOMINIOS_INSTITUCIONALES`

El entorno usa `admin@universidad.edu.co` como correo de prueba. No se
almacenan contraseñas reales en este repositorio; deben introducirse
manualmente en las variables secretas `adminPassword`, `teacherPassword` y
`studentPassword`.

## Flujo probado

Ejecuta las solicitudes en este orden:

1. **Identidad > Obtener CSRF**
2. **Identidad > Crear docente**
3. **Identidad > Crear estudiante**
4. **Identidad > Habilitar creación de casos para docentes**
5. **Académico > Crear espacio académico**
6. **Casos > Crear caso borrador**
7. **Casos > Publicar caso**

Las respuestas obtenidas durante la validación fueron:

| Operación | Resultado |
|---|---:|
| Obtener CSRF | `200 OK` |
| Crear docente | `201 Created` |
| Crear estudiante | `201 Created` |
| Habilitar creación de casos | `204 No Content` |
| Crear espacio académico | `201 Created` |
| Crear caso borrador | `201 Created` |
| Publicar caso | `200 OK` |

El caso validado terminó con estado `PUBLICADO`.

## Valores generados en la última prueba

Estos UUID sirven para continuar el flujo actual. Si se recrea la base de
datos, deben reemplazarse:

| Variable | Valor |
|---|---|
| `teacherId` | `f4686d91-e070-4c2f-b550-c34e57f7967c` |
| `studentId` | `fa346a5c-d05d-4574-867b-161d5dbc54cc` |
| `spaceId` | `0375892f-3a48-4f45-a6bc-3c0d3cb3a56e` |
| `caseId` | `06f2f02c-9ea4-4659-961c-aa232e0fb73c` |

## CSRF y autenticación

Las solicitudes protegidas usan HTTP Basic y CSRF:

- GET: normalmente requiere autenticación.
- POST y PUT: requieren autenticación, `X-XSRF-TOKEN` y `Content-Type:
  application/json`.
- El token se obtiene en `/api/identidad/csrf`.
- Copia `nombreCabecera` en `csrfHeader` y `token` en `csrfToken`.
- Si Postman envía literalmente `{{csrfToken}}` y no su valor actual, la
  respuesta será `403 Forbidden`.
- Si el token expira o cambia la instancia del backend, ejecuta de nuevo
  **Obtener CSRF** y copia el nuevo valor.

Postman conserva además la cookie `XSRF-TOKEN` mediante el Desktop Agent.

## Dominios institucionales

Los estudiantes solo pueden registrarse con un dominio configurado. Para el
entorno local, [`backend/src/main/resources/application.properties`](../backend/src/main/resources/application.properties)
usa `universidad.edu.co` como valor predeterminado:

```properties
psicoapp.identidad.dominios-institucionales=${PSICOAPP_DOMINIOS_INSTITUCIONALES:universidad.edu.co}
```

Para otro dominio, define `PSICOAPP_DOMINIOS_INSTITUCIONALES` y reinicia el
backend.

## Solicitudes aún disponibles

La colección también contiene solicitudes para:

- Consultar y actualizar casos.
- Archivar casos.
- Programar, iniciar, consultar y finalizar sesiones.
- Participación y evaluación.

Las rutas de participación y evaluación pueden responder `403` mientras su
autorización no esté habilitada en la configuración de seguridad. Las
solicitudes de sesiones deben ejecutarse después de tener un caso publicado y
un `caseId` válido.

## Repetir las pruebas

Los datos se guardan en la base local. Para repetir el flujo:

1. Genera un CSRF nuevo.
2. Usa correos diferentes para docente y estudiante, o limpia los datos de
   prueba.
3. Actualiza manualmente los UUID del entorno.
4. Si aparece `409 Conflict`, normalmente el correo o el recurso ya existe.

La colección no contiene scripts JavaScript de Postman; la automatización y
las pruebas de comportamiento permanecen en Java/JUnit.
