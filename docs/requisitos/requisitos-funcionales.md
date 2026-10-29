# Requisitos funcionales (RF)

> **Fuente de referencia:** Google Sheets `Requisitos_RF_RN_con_patrones`, hoja `16 oct`, con patrones actualizados el 29-10-2026. Los nombres, identificadores y descripciones de requisitos se conservan sin cambios.

**Hoja de origen:** `16 oct`. **Total:** 27 requisitos.

## RF-01 — Autenticar estudiantes

- **Módulo/épica de origen:** Acceso
- **Descripción:** El estudiante debe ingresar con su correo electrónico institucional y el código de autorización de la convocatoria.
- **Patrón recomendado en Google Sheets:** Facade

## RF-02 — Controlar acceso por rol

- **Módulo/épica de origen:** Acceso
- **Descripción:** El sistema debe restringir funciones según rol (administrador, docente, estudiante) y permiso específico para crear casos.
- **Patrón recomendado en Google Sheets:** Proxy

## RF-03 — Administrar docentes, roles y permisos

- **Módulo/épica de origen:** Acceso
- **Descripción:** El administrador debe crear los usuarios docentes y asignar sus roles y permisos.
- **Patrón recomendado en Google Sheets:** Facade

## RF-04 — Asociar espacio académico y estudiantes

- **Módulo/épica de origen:** Organización académica
- **Descripción:** Al agendar un caso, el docente debe asociar el nombre del espacio académico y uno o varios estudiantes autorizados.
- **Patrón recomendado en Google Sheets:** Facade

## RF-05 — Registrar estructura académica del caso

- **Módulo/épica de origen:** Diseño de casos
- **Descripción:** El docente autorizado debe registrar nombre, desarrollo situacional, contexto o historia, lista de escenarios académicos y tiempo estimado de participación.
- **Patrón recomendado en Google Sheets:** Builder

## RF-06 — Registrar contextos y preguntas por escenario

- **Módulo/épica de origen:** Diseño de casos
- **Descripción:** Cada escenario académico debe contener un contexto situacional y una lista de preguntas con sus respuestas.
- **Patrón recomendado en Google Sheets:** Builder

## RF-07 — Administar preguntas y respuestas

- **Módulo/épica de origen:** Diseño de casos
- **Descripción:** El docente debe agregar a cada escenario preguntas con opciones de respuesta e identificar cuáles son correctas o incorrectas.
- **Patrón recomendado en Google Sheets:** Builder

## RF-08 — Justificar respuestas correctas e incorrectas

- **Módulo/épica de origen:** Evaluación
- **Descripción:** El docente debe asociar a cada opción la explicación de por qué es correcta o incorrecta.
- **Patrón recomendado en Google Sheets:** Builder

## RF-09 — Validar estructura y rúbrica del caso

- **Módulo/épica de origen:** Validación
- **Descripción:** Todo caso nuevo queda en estado Borrador. El sistema debe validar que cumpla la estructura académica y tenga rúbrica asociada antes de pasarlo a Publicado; solo los casos publicados podrán agendarse. El docente podrá archivar un caso para que no se use en nuevas programaciones.
- **Patrón recomendado en Google Sheets:** Template Method

## RF-10 — Responder y modificar respuestas

- **Módulo/épica de origen:** Participación
- **Descripción:** El estudiante debe consultar los contextos académicos, responder las preguntas y volver entre ellas para cambiar sus selecciones hasta finalizar su participación.
- **Patrón recomendado en Google Sheets:** Sin patrón específico

## RF-11 — Registrar respuestas y mostrar progreso

- **Módulo/épica de origen:** Persistencia
- **Descripción:** El sistema debe registrar las respuestas del estudiante y mostrar su progreso en cada respuesta durante la participación.
- **Patrón recomendado en Google Sheets:** Sin patrón específico

## RF-12 — Registrar inicio y fin de la participación

- **Módulo/épica de origen:** Trazabilidad
- **Descripción:** Cada participación debe quedar asociada a un inicio, un fin, una calificación final y la retroalimentación por pregunta.
- **Patrón recomendado en Google Sheets:** Sin patrón específico

## RF-13 — Calcular nota con rúbrica

- **Módulo/épica de origen:** Evaluación
- **Descripción:** El sistema debe calcular y almacenar la calificación de cada intento a partir de las respuestas y de la rúbrica asociada.
- **Patrón recomendado en Google Sheets:** Strategy

## RF-14 — Mostrar resultado y retroalimentación final

- **Módulo/épica de origen:** Retroalimentación
- **Descripción:** Al finalizar el intento, el sistema debe presentar aciertos, errores, sus explicaciones y nota final; no debe anticipar justificaciones durante la resolución.
- **Patrón recomendado en Google Sheets:** Strategy

## RF-15 — Consultar participaciones de estudiantes

- **Módulo/épica de origen:** Seguimiento
- **Descripción:** El docente debe navegar por los casos ya realizados y consultar por estudiante sus respuestas, retroalimentación por respuesta y retroalimentación general docente.
- **Patrón recomendado en Google Sheets:** Facade

## RF-16 — Descargar reporte de participación

- **Módulo/épica de origen:** Reportes
- **Descripción:** El docente debe descargar el reporte de participación con los resultados académicos, incluidos avances parciales y ausencias, para registrar las notas en su sistema académico.
- **Patrón recomendado en Google Sheets:** Sin patrón específico

## RF-17 — Registrar y asociar la rúbrica

- **Módulo/épica de origen:** Evaluación
- **Descripción:** El docente debe registrar la rúbrica correspondiente al caso y asociarla para sustentar la calificación final.
- **Patrón recomendado en Google Sheets:** Builder

## RF-18 — Agendar fecha, horarios y condiciones

- **Módulo/épica de origen:** Programación
- **Descripción:** El docente debe definir fecha y horario de inicio y fin, tiempo máximo de participación no mayor al estimado del caso, lugar físico cuando aplique y mensaje personalizado.
- **Patrón recomendado en Google Sheets:** Sin patrón específico

## RF-19 — Administrar estados de sesiones

- **Módulo/épica de origen:** Programación
- **Descripción:** Cada sesión debe pasar por Sin iniciar, En curso y Finalizado. El docente la inicia y puede finalizarla; el sistema debe automatizar el cierre un día después del fin previsto cuando corresponda.
- **Patrón recomendado en Google Sheets:** State

## RF-20 — Asignar y validar código de autorización

- **Módulo/épica de origen:** Acceso
- **Descripción:** El sistema debe asociar un código de autorización al acceso del estudiante convocado e incluirlo en la notificación de la programación.
- **Patrón recomendado en Google Sheets:** Proxy

## RF-21 — Notificar convocatoria y cambios de estudiantes

- **Módulo/épica de origen:** Notificaciones
- **Descripción:** El sistema debe enviar por correo la información del caso, mensaje personalizado, URL y código; el docente podrá agregar o retirar estudiantes y cada afectado deberá ser notificado.
- **Patrón recomendado en Google Sheets:** Observer

## RF-22 — Restringir ingreso tardío

- **Módulo/épica de origen:** Participación
- **Descripción:** El sistema debe impedir iniciar la participación si ya pasó la mitad de tiempo del caso en que el estudiante debió iniciar. La base de cálculo de ese umbral debe precisarse con el responsable académico antes de implementarl
- **Patrón recomendado en Google Sheets:** Proxy

## RF-23 — Controlar tiempo máximo de participación

- **Módulo/épica de origen:** Participación
- **Descripción:** Al iniciar la participación, el sistema debe comenzar a contar el tiempo del estudiante y aplicar el tiempo máximo configurado, conservando las respuestas para su evaluación.
- **Patrón recomendado en Google Sheets:** Sin patrón específico

## RF-24 — Evaluar intentos incompletos y ausencias

- **Módulo/épica de origen:** Evaluación
- **Descripción:** El sistema debe calcular la nota parcial de quien no culmina y asignar cero a cada pregunta de quien no participa, aplicando las reglas de la rúbrica.
- **Patrón recomendado en Google Sheets:** Strategy

## RF-25 — Autorizar repetición excepcional

- **Módulo/épica de origen:** Participación
- **Descripción:** El sistema debe impedir repetir una participación registrada, salvo autorización del docente creador del caso; tampoco permitirá participar en una sesión finalizada.
- **Patrón recomendado en Google Sheets:** State

## RF-26 — Registrar retroalimentación general docente

- **Módulo/épica de origen:** Seguimiento
- **Descripción:** El docente debe añadir una valoración general a la participación individual del estudiante.
- **Patrón recomendado en Google Sheets:** Facade

## RF-27 — Notificar calificaciones y feedback

- **Módulo/épica de origen:** Notificaciones
- **Descripción:** El sistema debe notificar al estudiante su nota y avisar mediante el sistema cuando el docente agregue retroalimentación.
- **Patrón recomendado en Google Sheets:** Observer
