# Endpoints de la API

Esta referencia enumera los endpoints implementados por los controladores del
backend. La URL local base es `http://localhost:8080`. Salvo el ingreso, todas
las rutas requieren `Authorization: Bearer <token>` y cuerpos JSON con
`Content-Type: application/json` cuando corresponda.

## Convenciones

- Los identificadores de ruta son UUID.
- `ADMINISTRADOR`, `DOCENTE` y `ESTUDIANTE` indican los roles permitidos por la
  configuración de Spring Security. Las reglas de propiedad se vuelven a
  validar en los servicios de aplicación.
- Las creaciones responden `201 Created`; las consultas y actualizaciones,
  `200 OK`; marcar una notificación como leída y configurar permisos responden
  `204 No Content`.
- Los errores comunes son `400` para datos inválidos, `401` sin autenticación o
  con credenciales inválidas, `403` sin rol suficiente, `404` cuando el recurso
  no existe, `409` ante una transición o conflicto de estado y `422` cuando un
  caso no cumple las reglas de publicación.

## Autenticación e identidad

| Método y ruta | Acceso | Entrada | Resultado |
| --- | --- | --- | --- |
| `POST /api/identidad/ingreso` | Público | `correo`, `clave` | Token JWT Bearer, tipo y vencimiento. La clave también puede ser un código de convocatoria estudiantil. |
| `GET /api/identidad/yo` | Autenticado | — | Identidad y rol de la cuenta actual. |
| `GET /api/identidad/usuarios` | Administrador | Filtros opcionales `rol`, `activo`, `busqueda` | Usuarios administrables ordenados. |
| `GET /api/identidad/usuarios/{idUsuario}` | Administrador | — | Detalle del usuario, sin credenciales. |
| `POST /api/identidad/docentes` | Administrador | `correo`, `nombre`, `clave` | Docente creado. |
| `PUT /api/identidad/docentes/{idUsuario}` | Administrador | `correo`, `nombre` | Docente actualizado. |
| `PUT /api/identidad/usuarios/{idUsuario}/estado` | Administrador | `activo` | Usuario activado o desactivado. |
| `PUT /api/identidad/usuarios/{idUsuario}/rol` | Administrador | `rol` | Usuario con el rol actualizado. |
| `GET /api/identidad/roles` | Administrador | — | Roles disponibles. |
| `GET /api/identidad/roles/{rol}/creacion-casos` | Administrador | — | Estado del permiso de creación de casos. |
| `PUT /api/identidad/roles/{rol}/creacion-casos` | Administrador | `habilitado` | Configura el permiso de creación de casos. |
| `POST /api/identidad/estudiantes` | Docente | `correo`, `nombre` | Estudiante creado o cuenta estudiantil existente. |
| `GET /api/identidad/estudiantes?correo={correo}` | Docente | Correo institucional exacto | Estudiante activo encontrado. |
| `GET /api/identidad/estudiantes/listado` | Docente | — | Estudiantes activos disponibles. |

## Espacios académicos

| Método y ruta | Acceso | Entrada | Resultado |
| --- | --- | --- | --- |
| `POST /api/academico/espacios` | Docente | `nombre`, `periodo` | Espacio académico creado. |
| `GET /api/academico/espacios` | Docente | — | Espacios disponibles para el docente. |
| `GET /api/academico/espacios/{id}` | Docente | — | Espacio académico solicitado. |
| `PUT /api/academico/espacios/{id}` | Docente | `nombre`, `periodo` | Espacio actualizado; se rechaza si ya lo usa una sesión. |

## Casos

El cuerpo de creación y edición contiene la estructura académica del caso:
datos generales, escenarios, preguntas, opciones y rúbrica. Su contrato completo
se conserva en los DTO de `ServicioCreacionCaso`.

| Método y ruta | Acceso | Entrada | Resultado |
| --- | --- | --- | --- |
| `POST /api/casos` | Docente con permiso | Datos de creación del caso | Borrador creado. |
| `GET /api/casos` | Docente | — | Casos pertenecientes al docente. |
| `GET /api/casos/{id}` | Docente propietario | — | Caso solicitado. |
| `PUT /api/casos/{id}` | Docente propietario | Datos de creación del caso | Borrador sustituido. |
| `POST /api/casos/{id}/publicar` | Docente propietario con permiso | — | Caso publicado después de validar su estructura. |
| `POST /api/casos/{id}/archivar` | Docente propietario | — | Caso archivado. |

## Sesiones

| Método y ruta | Acceso | Entrada | Resultado |
| --- | --- | --- | --- |
| `POST /api/sesiones` | Docente | Programación, caso, espacio y lista `estudiantes` | Sesión programada y convocatorias iniciales. |
| `GET /api/sesiones` | Docente | — | Sesiones del docente. |
| `GET /api/sesiones/{id}` | Docente responsable | — | Programación, estado y convocatorias de la sesión. |
| `PUT /api/sesiones/{id}/convocados` | Docente responsable | Listas de UUID `agregar` y `retirar` | Sesión con convocatorias actualizadas. |
| `POST /api/sesiones/{id}/iniciar` | Docente responsable | — | Sesión en estado `EN_CURSO`. |
| `POST /api/sesiones/{id}/finalizar` | Docente responsable | — | Sesión en estado `FINALIZADO`; la repetición es idempotente. |

## Participación estudiantil

| Método y ruta | Acceso | Entrada | Resultado |
| --- | --- | --- | --- |
| `POST /api/participaciones/ingreso` | Estudiante convocado | `idSesion`, `codigoAcceso` | Caso visible sin claves de corrección e intento nuevo o recuperado. |
| `POST /api/participaciones` | Estudiante convocado | `idSesion`, `codigoAcceso` | Intento creado con las preguntas ordenadas del caso. |
| `GET /api/participaciones/{id}` | Estudiante propietario | — | Estado y progreso del intento. |
| `PUT /api/participaciones/{id}/respuestas` | Estudiante propietario | `idPregunta`, `idOpcion` | Intento con la respuesta guardada. |
| `POST /api/participaciones/{id}/avanzar` | Estudiante propietario | — | Intento situado en la siguiente pregunta. |
| `POST /api/participaciones/{id}/retroceder` | Estudiante propietario | — | Intento situado en la pregunta anterior, si la modalidad lo permite. |
| `POST /api/participaciones/{id}/reanudar` | Estudiante propietario | — | Intento reanudado. |
| `POST /api/participaciones/{id}/finalizar` | Estudiante propietario | — | Intento finalizado y evaluación solicitada. |

El docente responsable puede usar `PUT /api/sesiones/{sesion}/reintentos/{estudiante}`
para autorizar un reintento de un estudiante convocado con un intento anterior
cerrado. El estudiante vuelve a entrar con su código dentro de la ventana
vigente. Cada autorización se consume con el siguiente intento. Solo puede
existir un intento activo por estudiante entre todas las sesiones.

Al cerrar una sesión o retirar una convocatoria, se cierra y califica el intento activo con las respuestas registradas. Las preguntas pendientes reciben cero. El estudiante conserva la lectura de su intento con una sesión autenticada vigente.

## Evaluaciones y valoraciones

| Método y ruta | Acceso | Entrada | Resultado |
| --- | --- | --- | --- |
| `POST /api/evaluaciones/{idIntento}` | Estudiante propietario | — | Evaluación calculada; la operación es idempotente. |
| `GET /api/evaluaciones/{idIntento}` | Estudiante propietario | — | Nota y retroalimentación por pregunta. |
| `POST /api/evaluaciones/{idIntento}/valoraciones` | Docente responsable | `mensaje` de 1 a 4000 caracteres | Valoración docente creada. |
| `GET /api/evaluaciones/{idIntento}/valoraciones` | Docente responsable o estudiante propietario | — | Historial de valoraciones. |

## Notificaciones

| Método y ruta | Acceso | Entrada | Resultado |
| --- | --- | --- | --- |
| `GET /api/notificaciones` | Docente o estudiante | `limite` opcional, predeterminado `20`, rango 1–100; `desplazamiento` predeterminado `0` | Avisos internos entregados del usuario, del más reciente al más antiguo. |
| `PUT /api/notificaciones/{id}/leida` | Destinatario del aviso | — | Marca el aviso como leído de forma idempotente. |

## Reportes

| Método y ruta | Acceso | Entrada | Resultado |
| --- | --- | --- | --- |
| `GET /api/reportes/casos/{idCaso}/estudiantes/{idEstudiante}` | Docente responsable del caso | — | Intentos finalizados o vencidos, respuestas y evaluación del estudiante. |
| `GET /api/reportes/sesiones/{idSesion}` | Docente responsable | — | Participación, ausencias y notas de la sesión. |
| `POST /api/reportes/participaciones/{idIntento}/valoraciones` | Docente responsable | `mensaje` | Valoración general creada y aviso interno solicitado. |

## Fuente de verdad

Esta página resume el contrato HTTP vigente. Ante un cambio, deben actualizarse
en conjunto los controladores de `backend/src/main/java/com/psicoapp/*/api`, la
configuración de seguridad, las pruebas del módulo y esta referencia.
