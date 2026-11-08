# Catálogo de casos de uso

> **Fuente:** `Requisitos_RF_RN_con_patrones.xlsx`. Transcripción y reorganización de la hoja vigente; los identificadores y descripciones se conservan. No se inventan requisitos nuevos.

**Hoja de origen:** `Casos de uso`. **Total:** 36 casos de uso.

## CU-01 — Autenticarse en la convocatoria

- **Módulo del documento:** Acceso
- **Actor principal:** Estudiante
- **Objetivo / descripción:** Identificar al estudiante mediante correo institucional y código de autorización de su convocatoria; la autorización específica para iniciar se comprueba en CU-22.
- **RF relacionados:** RF-01, RF-20
- **RN relacionadas:** RN-04
- **Clasificación:** Principal
- **Relaciones / observaciones:** Autenticación previa a CU-17 cuando corresponda; CU-01 identifica al estudiante, CU-22 autoriza la sesión.

## CU-02 — Gestionar docentes

- **Módulo del documento:** Administración
- **Actor principal:** Administrador
- **Objetivo / descripción:** Registrar y administrar las cuentas docentes.
- **RF relacionados:** RF-03
- **RN relacionadas:** RN-10
- **Clasificación:** Principal
- **Relaciones / observaciones:** Sin observaciones

## CU-03 — Asignar roles y permisos

- **Módulo del documento:** Administración
- **Actor principal:** Administrador
- **Objetivo / descripción:** Definir permisos según rol, incluida creación de casos.
- **RF relacionados:** RF-02, RF-03
- **RN relacionadas:** RN-10
- **Clasificación:** Principal
- **Relaciones / observaciones:** Sin observaciones

## CU-04 — Crear y editar caso

- **Módulo del documento:** Casos
- **Actor principal:** Docente autorizado
- **Objetivo / descripción:** Registrar información académica, contexto y tiempo estimado del caso.
- **RF relacionados:** RF-05
- **RN relacionadas:** RN-01
- **Clasificación:** Principal
- **Relaciones / observaciones:** Sin observaciones

## CU-05 — Gestionar escenarios

- **Módulo del documento:** Casos
- **Actor principal:** Docente autorizado
- **Objetivo / descripción:** Registrar contextos y ordenar escenarios del caso.
- **RF relacionados:** RF-05, RF-06
- **RN relacionadas:** RN-02
- **Clasificación:** Principal
- **Relaciones / observaciones:** Sin observaciones

## CU-06 — Gestionar preguntas y opciones

- **Módulo del documento:** Casos
- **Actor principal:** Docente autorizado
- **Objetivo / descripción:** Crear preguntas, alternativas y marcar opciones correctas o incorrectas.
- **RF relacionados:** RF-06, RF-07
- **RN relacionadas:** RN-11
- **Clasificación:** Principal
- **Relaciones / observaciones:** Sin observaciones

## CU-07 — Registrar justificaciones

- **Módulo del documento:** Casos
- **Actor principal:** Docente autorizado
- **Objetivo / descripción:** Definir explicación de corrección para cada respuesta.
- **RF relacionados:** RF-08
- **RN relacionadas:** RN-11
- **Clasificación:** Principal
- **Relaciones / observaciones:** Sin observaciones

## CU-08 — Registrar y asociar rúbrica

- **Módulo del documento:** Casos
- **Actor principal:** Docente autorizado
- **Objetivo / descripción:** Configurar criterios de evaluación asociados al caso.
- **RF relacionados:** RF-17
- **RN relacionadas:** RN-03, RN-09
- **Clasificación:** Principal
- **Relaciones / observaciones:** Sin observaciones

## CU-09 — Publicar caso

- **Módulo del documento:** Casos
- **Actor principal:** Docente autorizado
- **Objetivo / descripción:** Cambiar BORRADOR a PUBLICADO cuando esté completo y válido.
- **RF relacionados:** RF-09
- **RN relacionadas:** RN-03, RN-05
- **Clasificación:** Principal
- **Relaciones / observaciones:** CU-09 «include» CU-10 (validación obligatoria antes de publicar).

## CU-10 — Validar caso y DSL

- **Módulo del documento:** Casos
- **Actor principal:** — (validación interna)
- **Objetivo / descripción:** Comprobar estructura académica, preguntas, opciones, rúbrica y reglas del DSL antes de permitir PUBLICADO.
- **RF relacionados:** RF-09
- **RN relacionadas:** RN-03, RN-05, RN-11
- **Clasificación:** Incluido
- **Relaciones / observaciones:** CU-09 «include» CU-10; verificación ejecutada internamente, sin actor propio.

## CU-11 — Archivar caso

- **Módulo del documento:** Casos
- **Actor principal:** Docente autorizado
- **Objetivo / descripción:** Impedir nuevas programaciones del caso archivado.
- **RF relacionados:** RF-09
- **RN relacionadas:** RN-03
- **Clasificación:** Principal
- **Relaciones / observaciones:** Sin observaciones

## CU-12 — Programar sesión

- **Módulo del documento:** Programación
- **Actor principal:** Docente
- **Objetivo / descripción:** Agendar un caso PUBLICADO con fechas, duración válida, espacio académico, lugar y mensaje, incluyendo la convocatoria inicial.
- **RF relacionados:** RF-04, RF-18
- **RN relacionadas:** RN-03, RN-13
- **Clasificación:** Principal
- **Relaciones / observaciones:** CU-12 «include» CU-13 (convocatoria inicial). Los cambios posteriores se realizan mediante CU-34.

## CU-13 — Convocar estudiantes inicialmente

- **Módulo del documento:** Programación
- **Actor principal:** Docente
- **Objetivo / descripción:** Vincular estudiantes autorizados a la sesión programada, asociar código de acceso y solicitar notificación inicial; no incluye cambios posteriores.
- **RF relacionados:** RF-04, RF-20, RF-21
- **RN relacionadas:** RN-04
- **Clasificación:** Incluido
- **Relaciones / observaciones:** CU-12 «include» CU-13; CU-13 «include» CU-14. Modificar convocados es CU-34.

## CU-14 — Enviar notificación de convocatoria

- **Módulo del documento:** Comunicación
- **Actor principal:** — (envío interno; correo externo secundario)
- **Objetivo / descripción:** Preparar y solicitar el envío de datos del caso, URL, código y mensaje de convocatoria, así como avisos de altas o retiros.
- **RF relacionados:** RF-21
- **RN relacionadas:** RN-04
- **Clasificación:** Incluido
- **Relaciones / observaciones:** CU-13 y CU-34 «include» CU-14. El servicio de correo puede mostrarse como actor secundario.

## CU-15 — Iniciar sesión programada

- **Módulo del documento:** Sesiones
- **Actor principal:** Docente
- **Objetivo / descripción:** Cambiar la sesión de Sin iniciar a En curso.
- **RF relacionados:** RF-19
- **RN relacionadas:** RN-12
- **Clasificación:** Principal
- **Relaciones / observaciones:** Sin observaciones

## CU-16 — Finalizar sesión

- **Módulo del documento:** Sesiones
- **Actor principal:** Docente
- **Objetivo / descripción:** Cerrar manualmente la sesión, impedir nuevos ingresos y consolidar resultados de estudiantes convocados sin intento.
- **RF relacionados:** RF-19, RF-24
- **RN relacionadas:** RN-12, RN-16
- **Clasificación:** Principal
- **Relaciones / observaciones:** CU-16 «include» CU-35 (consolidación de ausencias al cierre).

## CU-17 — Iniciar participación

- **Módulo del documento:** Participación
- **Actor principal:** Estudiante
- **Objetivo / descripción:** Comenzar un intento nuevo con identidad autenticada, autorización vigente, ventana temporal válida y sin otro intento EN_PROGRESO.
- **RF relacionados:** RF-01, RF-20, RF-22, RF-23, RF-25
- **RN relacionadas:** RN-04, RN-07, RN-12, RN-15
- **Clasificación:** Principal
- **Relaciones / observaciones:** CU-17 «include» CU-22 y CU-23. CU-01 es autenticación previa, no una autorización equivalente.

## CU-18 — Responder preguntas

- **Módulo del documento:** Participación
- **Actor principal:** Estudiante
- **Objetivo / descripción:** Consultar escenarios en secuencia fija y registrar las selecciones del estudiante durante un intento activo.
- **RF relacionados:** RF-10
- **RN relacionadas:** RN-02, RN-14
- **Clasificación:** Principal
- **Relaciones / observaciones:** CU-18 «include» CU-19 (persistir respuestas y progreso). Navegación: CU-20.

## CU-19 — Guardar respuestas y progreso

- **Módulo del documento:** Participación
- **Actor principal:** — (persistencia interna)
- **Objetivo / descripción:** Guardar o actualizar la última selección por pregunta y el progreso del intento, conservando el historial exigido.
- **RF relacionados:** RF-11, RF-12
- **RN relacionadas:** RN-06, RN-19
- **Clasificación:** Incluido
- **Relaciones / observaciones:** CU-18 y CU-36 «include» CU-19; no es una acción iniciada por un actor propio.

## CU-20 — Navegar entre preguntas según modalidad

- **Módulo del documento:** Participación
- **Actor principal:** Estudiante
- **Objetivo / descripción:** Navegar bajo modalidad FLEXIBLE (retroceder y avanzar con pendientes) o RESTRINGIDA (contestar antes de avanzar y no retroceder).
- **RF relacionados:** RF-10
- **RN relacionadas:** RN-14, RN-19
- **Clasificación:** Principal
- **Relaciones / observaciones:** CU-36 «extend» CU-20 solo si navegación FLEXIBLE e intento EN_PROGRESO.

## CU-21 — Reanudar intento

- **Módulo del documento:** Participación
- **Actor principal:** Estudiante
- **Objetivo / descripción:** Reanudar el mismo intento EN_PROGRESO, recuperar selecciones y avance mientras no expire el temporizador, que sigue transcurriendo.
- **RF relacionados:** RF-11, RF-12, RF-23
- **RN relacionadas:** RN-06, RN-18
- **Clasificación:** Principal
- **Relaciones / observaciones:** No crea otro intento. Validar estado y vencimiento del tiempo restante; CU-23 controla solo ingreso inicial.

## CU-22 — Verificar autorización de convocatoria

- **Módulo del documento:** Acceso
- **Actor principal:** — (autorización interna)
- **Objetivo / descripción:** Comprobar que el estudiante autenticado figure como convocado autorizado para la sesión vigente; distinto de verificar credenciales.
- **RF relacionados:** RF-01, RF-20
- **RN relacionadas:** RN-04
- **Clasificación:** Incluido
- **Relaciones / observaciones:** CU-17 «include» CU-22. CU-01 comprueba correo y código; CU-22 autoriza la participación concreta.

## CU-23 — Verificar ventana de ingreso

- **Módulo del documento:** Acceso
- **Actor principal:** — (validación temporal interna)
- **Objetivo / descripción:** Confirmar sesión EN_CURSO y que no haya transcurrido más de la mitad del intervalo programado contado desde el inicio de sesión (RN-15).
- **RF relacionados:** RF-22, RF-25
- **RN relacionadas:** RN-12, RN-15
- **Clasificación:** Incluido
- **Relaciones / observaciones:** CU-17 «include» CU-23. Se aplica RN-15; conciliar posteriormente la redacción ambigua de RF-22.

## CU-24 — Finalizar intento

- **Módulo del documento:** Participación
- **Actor principal:** Estudiante
- **Objetivo / descripción:** Finalizar voluntariamente el intento, bloquear cambios, calcular la nota con rúbrica y solicitar aviso de resultado.
- **RF relacionados:** RF-12, RF-13, RF-14, RF-23, RF-27
- **RN relacionadas:** RN-08, RN-09, RN-14
- **Clasificación:** Principal
- **Relaciones / observaciones:** CU-24 «include» CU-25 y CU-33.

## CU-25 — Calcular calificación con rúbrica

- **Módulo del documento:** Evaluación
- **Actor principal:** — (evaluación interna)
- **Objetivo / descripción:** Calcular y guardar la nota del intento con la última selección válida y cero a preguntas no respondidas, según rúbrica; las ausencias sin intento se tratan en CU-35.
- **RF relacionados:** RF-13, RF-24
- **RN relacionadas:** RN-09, RN-16, RN-19
- **Clasificación:** Incluido
- **Relaciones / observaciones:** CU-24 y CU-27 «include» CU-25; CU-35 resuelve las ausencias sin intento.

## CU-26 — Consultar calificación y retroalimentación

- **Módulo del documento:** Evaluación
- **Actor principal:** Estudiante
- **Objetivo / descripción:** Mostrar al estudiante aciertos, errores, explicaciones y nota final únicamente después de finalizar su intento.
- **RF relacionados:** RF-14
- **RN relacionadas:** RN-08, RN-09
- **Clasificación:** Principal
- **Relaciones / observaciones:** Sin observaciones

## CU-27 — Finalizar intento por vencimiento

- **Módulo del documento:** Participación
- **Actor principal:** — (vencimiento detectado internamente)
- **Objetivo / descripción:** Al vencer el límite individual, finalizar el intento, conservar respuestas, calificar pendientes con cero y solicitar aviso del resultado.
- **RF relacionados:** RF-23, RF-24, RF-27
- **RN relacionadas:** RN-16, RN-17, RN-18
- **Clasificación:** Automático
- **Relaciones / observaciones:** CU-27 «include» CU-25 y CU-33. No dibujar un reloj como actor si el planificador es interno.

## CU-28 — Cerrar sesión automáticamente

- **Módulo del documento:** Sesiones
- **Actor principal:** — (plazo supervisado internamente)
- **Objetivo / descripción:** Finalizar una sesión que siga abierta un día después de su fin programado y consolidar las ausencias.
- **RF relacionados:** RF-19, RF-24
- **RN relacionadas:** RN-12, RN-16
- **Clasificación:** Automático
- **Relaciones / observaciones:** CU-28 «include» CU-35; solo dibujar planificador externo si realmente existe como sistema independiente.

## CU-29 — Autorizar reintento excepcional

- **Módulo del documento:** Participación
- **Actor principal:** Docente creador
- **Objetivo / descripción:** Permitir un nuevo intento independiente con sesión activa y sin otro intento en progreso.
- **RF relacionados:** RF-25
- **RN relacionadas:** RN-06, RN-07
- **Clasificación:** Principal
- **Relaciones / observaciones:** Sin observaciones

## CU-30 — Consultar participaciones

- **Módulo del documento:** Seguimiento
- **Actor principal:** Docente
- **Objetivo / descripción:** Revisar respuestas, notas y retroalimentación por estudiante.
- **RF relacionados:** RF-15
- **RN relacionadas:** RN-06, RN-09
- **Clasificación:** Principal
- **Relaciones / observaciones:** Sin observaciones

## CU-31 — Descargar reporte académico

- **Módulo del documento:** Seguimiento
- **Actor principal:** Docente
- **Objetivo / descripción:** Exportar participaciones, notas parciales y ausencias.
- **RF relacionados:** RF-16, RF-24
- **RN relacionadas:** RN-06, RN-16
- **Clasificación:** Principal
- **Relaciones / observaciones:** Sin observaciones

## CU-32 — Registrar retroalimentación general

- **Módulo del documento:** Seguimiento
- **Actor principal:** Docente
- **Objetivo / descripción:** Registrar valoración general de una participación individual y solicitar aviso al estudiante.
- **RF relacionados:** RF-26
- **RN relacionadas:** RN-06
- **Clasificación:** Principal
- **Relaciones / observaciones:** CU-32 «include» CU-33 (aviso por nueva valoración).

## CU-33 — Notificar calificación y valoración

- **Módulo del documento:** Comunicación
- **Actor principal:** — (notificación interna; canal externo secundario)
- **Objetivo / descripción:** Solicitar envío de la calificación al cerrar el intento y avisar de una nueva valoración docente, sin revertir registros ante fallos del canal.
- **RF relacionados:** RF-27
- **RN relacionadas:** RN-09
- **Clasificación:** Incluido
- **Relaciones / observaciones:** CU-24, CU-27 y CU-32 «include» CU-33. Servicio de notificación como actor secundario solo si es externo.

## CU-34 — Modificar participantes convocados

- **Módulo del documento:** Programación
- **Actor principal:** Docente
- **Objetivo / descripción:** Agregar o retirar estudiantes de una convocatoria existente, actualizando sus autorizaciones y notificando a los afectados.
- **RF relacionados:** RF-04, RF-20, RF-21
- **RN relacionadas:** RN-04
- **Clasificación:** Principal
- **Relaciones / observaciones:** CU-34 «include» CU-14; operación independiente de la programación inicial CU-12.

## CU-35 — Registrar ausencias y calificar no participantes

- **Módulo del documento:** Evaluación
- **Actor principal:** — (consolidación interna al cerrar sesión)
- **Objetivo / descripción:** Identificar convocados que no iniciaron intento y registrar la calificación correspondiente, con cero para sus preguntas conforme a rúbrica, para reportes.
- **RF relacionados:** RF-16, RF-24
- **RN relacionadas:** RN-09, RN-16
- **Clasificación:** Incluido
- **Relaciones / observaciones:** CU-16 y CU-28 «include» CU-35; no confundir ausencia sin intento con intento incompleto (CU-25).

## CU-36 — Modificar respuesta previamente seleccionada

- **Módulo del documento:** Participación
- **Actor principal:** Estudiante
- **Objetivo / descripción:** Cambiar una selección anterior únicamente en modo FLEXIBLE mientras el intento esté EN_PROGRESO; evaluar solo la última selección.
- **RF relacionados:** RF-10, RF-11
- **RN relacionadas:** RN-14, RN-19
- **Clasificación:** Extensión
- **Relaciones / observaciones:** CU-36 «extend» CU-20 [FLEXIBLE e intento EN_PROGRESO]; CU-36 «include» CU-19.
