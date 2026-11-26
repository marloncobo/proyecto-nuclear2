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
| `POST /api/identidad/ingreso` | Público | `correo`, `clave` | Token JWT Bearer, tipo, vencimiento y estado de cambio de contraseña inicial. |
| `GET /api/identidad/yo` | Autenticado | — | Identidad y rol de la cuenta actual. |
| `PUT /api/identidad/yo/clave` | Autenticado con acceso completo | `actual`, `nueva` | Verifica la contraseña actual y actualiza la de la propia cuenta. |
| `PUT /api/identidad/clave-inicial` | Estudiante pendiente | `clave` | Sustituye la contraseña inicial e invalida el token temporal. |
| `GET /api/identidad/usuarios` | Administrador | Filtros opcionales `rol`, `activo`, `busqueda` | Usuarios administrables ordenados. |
| `GET /api/identidad/usuarios/{idUsuario}` | Administrador | — | Detalle del usuario, sin credenciales. |
| `POST /api/identidad/docentes` | Administrador | `correo`, `nombre`, `clave` | Docente creado. |
| `POST /api/identidad/estudiantes` | Docente o administrador | `correo`, `nombre`, `clave` | Estudiante creado con contraseña inicial, sin exponer credenciales. |
| `PUT /api/identidad/estudiantes/{id}/clave-inicial` | Docente o administrador | `clave` | Asigna contraseña inicial solo a una cuenta estudiantil que aún no tiene. |
| `PUT /api/identidad/docentes/{idUsuario}` | Administrador | `correo`, `nombre` | Docente actualizado. |
| `PUT /api/identidad/usuarios/{idUsuario}/estado` | Administrador | `activo` | Usuario activado o desactivado. |
| `PUT /api/identidad/usuarios/{idUsuario}/rol` | Administrador | `rol` | Usuario con el rol actualizado. |
| `GET /api/identidad/roles` | Administrador | — | Roles disponibles. |
| `PUT /api/identidad/usuarios/{idUsuario}/creacion-casos` | Administrador | `habilitado` | Configura el permiso individual de un docente y devuelve su perfil actualizado. |
| `POST /api/identidad/estudiantes` | Docente | `correo`, `nombre` | Estudiante creado o cuenta estudiantil existente. |
| `GET /api/identidad/estudiantes?correo={correo}` | Docente | Correo institucional exacto | Estudiante activo encontrado. |
| `GET /api/identidad/estudiantes/listado` | Docente | — | Estudiantes activos disponibles. |

## Espacios académicos

| Método y ruta | Acceso | Entrada | Resultado |
| --- | --- | --- | --- |
| `POST /api/academico/espacios` | Docente | `nombre`, `periodo`, lista opcional `estudiantes` | Espacio académico creado con su lista predeterminada. |
| `GET /api/academico/espacios` | Docente | — | Espacios disponibles para el docente. |
| `GET /api/academico/espacios/{id}` | Docente | — | Espacio académico solicitado. |
| `PUT /api/academico/espacios/{id}` | Docente | `nombre`, `periodo`, lista opcional `estudiantes` | Actualiza el espacio. Si ya tiene sesiones, solo se puede cambiar la lista predeterminada. Las convocatorias existentes se conservan. |

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
| `GET /api/sesiones` | Docente | — | Sesiones por fecha de creación descendente. Las sesiones históricas sin fecha de creación aparecen después, por inicio programado descendente. |
| `GET /api/sesiones/{id}` | Docente responsable | — | Programación, estado, convocatorias y horarios de apertura y cierre reales cuando están registrados. |
| `PUT /api/sesiones/{id}/convocados` | Docente responsable | Listas de UUID `agregar` y `retirar` | Sesión con convocatorias actualizadas. |
| `POST /api/sesiones/{id}/iniciar` | Docente responsable | — | Sesión en estado `EN_CURSO`. |
| `POST /api/sesiones/{id}/finalizar` | Docente responsable | — | Sesión en estado `FINALIZADO`; la repetición es idempotente. |

## Participación estudiantil

| Método y ruta | Acceso | Entrada | Resultado |
| --- | --- | --- | --- |
| `GET /api/estudiante/actividades` | Estudiante | — | Convocatorias propias con la acción disponible y `limiteIntento` si hay un intento en curso. Consultar el listado no crea intentos. |
| `GET /api/estudiante/actividades/{idSesion}` | Estudiante convocado | — | Caso visible e intento existente, sin claves de corrección ni creación implícita. |
| `POST /api/estudiante/actividades/{idSesion}/iniciar` | Estudiante convocado | — | Intento creado explícitamente con las preguntas ordenadas del caso. |
| `GET /api/estudiante/resultados` | Estudiante | — | Intentos finalizados propios, soluciones, explicaciones y valoraciones. |
| `POST /api/participaciones` | Estudiante convocado | `idSesion` | Contrato de inicio compatible sin código de convocatoria. |
| `GET /api/participaciones/{id}` | Estudiante propietario | — | Estado y progreso del intento. |
| `PUT /api/participaciones/{id}/respuestas` | Estudiante propietario | `idPregunta`, `idOpcion` | Intento con la respuesta guardada. |
| `POST /api/participaciones/{id}/avanzar` | Estudiante propietario | — | Intento situado en la siguiente pregunta. |
| `POST /api/participaciones/{id}/retroceder` | Estudiante propietario | — | Intento situado en la pregunta anterior, si la modalidad lo permite. |
| `POST /api/participaciones/{id}/reanudar` | Estudiante propietario | — | Intento reanudado. |
| `POST /api/participaciones/{id}/finalizar` | Estudiante propietario | — | Intento finalizado y evaluación solicitada. |

El docente responsable puede usar `PUT /api/sesiones/{sesion}/reintentos/{estudiante}`
para autorizar un reintento de un estudiante convocado con un intento anterior
cerrado. El estudiante vuelve a iniciar explícitamente dentro de la ventana
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
| `GET /api/notificaciones/no-leidas` | Docente o estudiante | — | Número total de avisos internos entregados y sin leer del usuario. |
| `PUT /api/notificaciones/{id}/leida` | Destinatario del aviso | — | Marca el aviso como leído de forma idempotente. |

## Reportes

| Método y ruta | Acceso | Entrada | Resultado |
| --- | --- | --- | --- |
| `GET /api/reportes/casos/{idCaso}/estudiantes/{idEstudiante}` | Docente responsable del caso | — | Intentos finalizados o vencidos, respuestas y evaluación del estudiante. |
| `GET /api/reportes/sesiones/{idSesion}` | Docente responsable | — | Participación, ausencias, horarios por intento, aciertos y notas de la sesión. |
| `POST /api/reportes/participaciones/{idIntento}/valoraciones` | Docente responsable | `mensaje` | Valoración general creada y aviso interno solicitado. |

## Fuente de verdad

Esta página resume el contrato HTTP vigente. Ante un cambio, deben actualizarse
en conjunto los controladores de `backend/src/main/java/com/psicoapp/*/api`, la
configuración de seguridad, las pruebas del módulo y esta referencia.
