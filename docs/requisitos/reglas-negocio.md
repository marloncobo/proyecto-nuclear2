# Reglas de negocio (RN)

> **Fuente de referencia:** Google Sheets `Requisitos_RF_RN_con_patrones`, hoja `RN depuradas`, con patrones y justificaciones actualizados el 29-10-2026. Las reglas y sus identificadores se conservan sin cambios.

**Hoja de origen:** `RN depuradas`. **Total:** 19 reglas.

## RN-01 — Alcance educativo

**Regla:** Los casos del simulador tendrán fines formativos en psicología social y no se utilizarán como herramienta para emitir diagnósticos clínicos reales.

- **Patrón recomendado en Google Sheets:** Sin patrón específico
- **Justificación en Google Sheets:** Sin patrón específico asociado

## RN-02 — Secuencia de escenarios

**Regla:** Los escenarios de cada caso se presentarán en un orden predefinido, independientemente de las respuestas seleccionadas por el estudiante.

- **Patrón recomendado en Google Sheets:** Sin patrón específico
- **Justificación en Google Sheets:** El orden fijo de escenarios se implementa con una colección ordenada; el documento no selecciona Iterator.

## RN-03 — Publicación

**Regla:** Un caso solo podrá publicarse cuando cumpla la estructura académica requerida y tenga una rúbrica asociada. Únicamente los casos publicados podrán agendarse.

- **Patrón recomendado en Google Sheets:** Template Method
- **Justificación en Google Sheets:** Template Method fija una secuencia de validación de estructura, preguntas y rúbrica antes de permitir la publicación del caso.

## RN-04 — Acceso autorizado

**Regla:** Los estudiantes únicamente podrán participar en sesiones para las que hayan sido convocados y autorizados, siempre que cumplan las condiciones de acceso vigentes.

- **Patrón recomendado en Google Sheets:** Proxy
- **Justificación en Google Sheets:** Proxy verifica convocatoria, autorización y condiciones de acceso antes de delegar al servicio de participación.

## RN-05 — Validez

**Regla:** Si un caso no cumple las reglas definidas en el DSL, no podrá publicarse ni ejecutarse.

- **Patrón recomendado en Google Sheets:** Template Method
- **Justificación en Google Sheets:** Template Method organiza las validaciones previas a publicar el caso; Interpreter no se selecciona únicamente por existir un DSL.

## RN-06 — Historial

**Regla:** Cada intento de participación deberá conservarse de manera independiente, incluyendo respuestas, tiempos, calificación y retroalimentación correspondiente.

- **Patrón recomendado en Google Sheets:** Sin patrón específico
- **Justificación en Google Sheets:** La conservación independiente de intentos requiere persistencia; el documento no prioriza Memento.

## RN-07 — Reintentos autorizados

**Regla:** Un estudiante solo podrá repetir una simulación cuando el profesor responsable lo autorice y la sesión permanezca en curso. Cada repetición se registrará como un nuevo intento independiente, conservando el historial de los intentos anteriores. No se permitirá iniciar un nuevo intento mientras exista otro en progreso.

- **Patrón recomendado en Google Sheets:** Sin patrón específico
- **Justificación en Google Sheets:** La autorización excepcional de un nuevo intento requiere reglas de negocio; State se propone para el ciclo de vida de sesiones, no para reemplazar los permisos del docente.

## RN-08 — Retroalimentación

**Regla:** Las explicaciones de respuestas correctas e incorrectas se mostrarán al finalizar el intento y no durante su resolución.

- **Patrón recomendado en Google Sheets:** Sin patrón específico
- **Justificación en Google Sheets:** Mostrar la retroalimentación al finalizar es una regla de presentación y momento de publicación, sin patrón específico elegido.

## RN-09 — Evaluación y calificación

**Regla:** La calificación de cada participación se calculará numéricamente de acuerdo con los criterios y ponderaciones establecidos en la rúbrica asociada al caso. El resultado deberá almacenarse y mostrarse al estudiante al finalizar su participación.

- **Patrón recomendado en Google Sheets:** Strategy
- **Justificación en Google Sheets:** Strategy encapsula posibles algoritmos de calificación por puntos o ponderación conforme a la rúbrica. Las variantes son propuestas técnicas, no modalidades obligatorias de los requisitos.

## RN-10 — Administración de usuarios

**Regla:** La creación de cuentas docentes y la asignación de sus roles y permisos serán responsabilidad del administrador.

- **Patrón recomendado en Google Sheets:** Facade
- **Justificación en Google Sheets:** Facade coordina las operaciones administrativas entre servicios de usuarios, permisos y grupos.

## RN-11 — Opciones de respuesta

**Regla:** Cada pregunta tendrá sus respectivas opciones de respuesta, identificadas como correctas o incorrectas y acompañadas de su justificación.

- **Patrón recomendado en Google Sheets:** Builder
- **Justificación en Google Sheets:** Builder organiza el ensamblaje de casos, preguntas, opciones y otros componentes académicos.

## RN-12 — Estados y cierre de sesiones

**Regla:** Las sesiones tendrán los estados Sin iniciar, En curso y Finalizado. El profesor será responsable de iniciar y finalizar las sesiones. Si una sesión no ha sido finalizada por el profesor, el sistema la finalizará automáticamente un día después de la fecha y hora programadas de finalización. Una sesión finalizada no permitirá nuevas participaciones.

- **Patrón recomendado en Google Sheets:** State
- **Justificación en Google Sheets:** State representa el ciclo de vida de sesiones y controla las transiciones válidas, incluido el cierre.

## RN-13 — Tiempo de participación

**Regla:** La duración máxima configurada para una sesión no podrá superar el tiempo estimado del caso. Cada estudiante deberá respetar el límite de participación asignado.

- **Patrón recomendado en Google Sheets:** Sin patrón específico
- **Justificación en Google Sheets:** El límite temporal es una validación de negocio, sin patrón específico seleccionado.

## RN-14 — Navegación configurable

**Regla:** El profesor podrá habilitar o deshabilitar el retroceso entre preguntas. Cuando esté habilitado, el estudiante podrá regresar a preguntas anteriores, modificar sus respuestas y avanzar dejando preguntas sin responder. Cuando esté deshabilitado, deberá responder obligatoriamente cada pregunta antes de avanzar y no podrá regresar a preguntas anteriores. Una vez finalizado el intento, las respuestas no podrán modificarse.

- **Patrón recomendado en Google Sheets:** Sin patrón específico
- **Justificación en Google Sheets:** Las políticas de navegación deben cumplirse según la configuración docente; el documento de patrones corregido reserva Strategy para posibles algoritmos de calificación.

## RN-15 — Límite de ingreso tardío

**Regla:** Un estudiante no podrá iniciar su participación cuando haya transcurrido más de la mitad del tiempo programado para la sesión, contado desde su hora de inicio.

- **Patrón recomendado en Google Sheets:** Sin patrón específico
- **Justificación en Google Sheets:** La restricción de ingreso tardío se comprueba mediante validación temporal, sin patrón específico.

## RN-16 — Evaluación de preguntas sin responder

**Regla:** Las preguntas que permanezcan sin responder al finalizar un intento recibirán una puntuación de cero. La calificación final se calculará aplicando todos los criterios y ponderaciones de la rúbrica asociada al caso, incluyendo las preguntas no respondidas.

- **Patrón recomendado en Google Sheets:** Strategy
- **Justificación en Google Sheets:** Las preguntas sin responder obtienen cero conforme a la regla de negocio; Strategy permite separar posibles algoritmos de calificación sin alterar esa regla.

## RN-17 — Finalización automática de intentos

**Regla:** Cuando se agote el tiempo máximo de participación, el sistema finalizará automáticamente el intento, conservará las respuestas registradas y asignará cero puntos a las preguntas sin responder. Posteriormente, calculará y almacenará la calificación final conforme a la rúbrica asociada al caso.

- **Patrón recomendado en Google Sheets:** Sin patrón específico
- **Justificación en Google Sheets:** La finalización del intento por tiempo agotado se controla con reglas de negocio y persistencia; State se aplica al ciclo de vida de la sesión, no al progreso individual.

## RN-18 — Continuidad de intentos

**Regla:** Si un estudiante abandona una simulación antes de finalizar su intento, podrá reanudarlo conservando las respuestas registradas, siempre que no haya agotado su tiempo de participación. El tiempo continuará transcurriendo durante su ausencia. Si se agota, el sistema finalizará automáticamente el intento conforme a la RN-17.

- **Patrón recomendado en Google Sheets:** Sin patrón específico
- **Justificación en Google Sheets:** La reanudación conserva respuestas y tiempos mediante persistencia; Memento no es prioritario según el documento.

## RN-19 — Evaluación de respuestas modificadas

**Regla:** Cuando el estudiante modifique una respuesta durante un intento en curso, únicamente se considerará la última opción seleccionada para calcular la calificación final. Las respuestas anteriores no tendrán efecto sobre la evaluación.

- **Patrón recomendado en Google Sheets:** Sin patrón específico
- **Justificación en Google Sheets:** Evaluar la última respuesta es una regla de dominio y persistencia, sin patrón específico seleccionado.
