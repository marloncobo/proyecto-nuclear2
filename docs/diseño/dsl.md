# DSL de casos situacionales de Psicoapp

> **Origen:** extracción documental de *Proyecto Nuclear – Primer corte | Estructura para desarrollo*, parte II, apartados 9–11. Se conserva la propuesta técnica del documento; no representa un validador implementado ni una versión formalmente aprobada.

> **Nota de trazabilidad:** el documento fuente todavía menciona en otras partes patrones de una selección anterior (Chain of Responsibility y Strategy para navegación). Esta extracción se limita al DSL/OCL/JSON y no modifica los requisitos actuales ni la selección de patrones del repositorio.

## 9. Identificación del DSL

### 9.1 Propósito y alcance

El DSL de casos situacionales de Psicología Social es un lenguaje declarativo en JSON para describir un caso académico, sus escenas, personajes, recursos, preguntas, opciones, retroalimentaciones y desenlaces. Su finalidad es facilitar la creación, intercambio y validación estructural de casos sin codificar directamente la lógica de una interfaz. Se conserva el dominio de la entrega previa y se amplía la especificación para contemplar las necesidades de evaluación y navegación identificadas en el análisis del primer corte.

Delimitación: el archivo del DSL define el contenido académico reutilizable. Los usuarios, grupos, convocatorias, sesiones, intentos, respuestas y calificaciones pertenecen al modelo de ejecución y persistencia del sistema; no se insertan como registros operativos dentro de la definición del caso. El ejemplo anterior mezclaba ambas responsabilidades. La integración entre ambos modelos se realiza mediante identificadores de caso y versión.

### 9.2 Conceptos y correspondencia conceptual

Caso: unidad didáctica con título, contexto, estado y tiempo estimado. Escenario: situación académica con posición ordinal predeterminada; su orden no depende de la opción seleccionada (RN-02). Pregunta: enunciado asociado a un escenario. Opción: alternativa con corrección, puntaje y justificación, sin rutas narrativas. Rúbrica: criterios y ponderaciones para evaluar. Sesion configura, de forma independiente del contenido del caso, la navegación FLEXIBLE o RESTRINGIDA (RN-14).

La correspondencia conceptual queda definida entre Caso, Escenario, Pregunta, OpcionRespuesta, Rubrica, Sesion, Convocatoria, Participacion y RespuestaEstudiante. Para su verificación formal, las invariantes utilizan nombres compatibles con este modelo; cuando una restricción depende de instantes o estados de objetos asociados, además se implementa en servicios transaccionales.

### 9.3 Requisitos y decisiones pendientes

La fuente vigente de requisitos es la matriz de 27 RF, 19 RN y 36 casos de uso. La estructura académica y las reglas de publicación están gobernadas por RF-05–RF-09, RF-17 y RN-02/RN-03/RN-05/RN-11. Los requisitos de ejecución (RF-10–RF-14, RF-18–RF-27) se representan mediante Sesion, Convocatoria, Participacion y RespuestaEstudiante, fuera del JSON portable del caso. La cantidad mínima de opciones y las ponderaciones exactas deberán acordarse como validaciones del dominio cuando no exista una cifra explícita aprobada.

## 10. Definición de reglas OCL e invariantes

### 10.1 Invariantes y restricciones del dominio

OCL-01 — Un estudiante tiene como máximo un intento en estado EN_PROGRESO. La invariante impide mantener dos o más intentos activos simultáneamente para el mismo estudiante.

```ocl
context Estudiante
inv UnSoloIntentoEnProgreso:
  self.intentos->select(i |
    i.estado = EstadoIntento::EN_PROGRESO)->size() <= 1
```

OCL-02 — Todo intento en estado EN_PROGRESO está asociado a un caso en estado PUBLICADO. La restricción verifica el estado del caso durante la ejecución del intento. La creación de asignaciones y el control de acceso se validan mediante sus propias condiciones.

```ocl
context Intento
inv CasoPublicadoDuranteIntento:
  self.estado = EstadoIntento::EN_PROGRESO implies
    self.caso.estado = EstadoCaso::PUBLICADO
```

OCL-03 — Toda decisión perteneciente a un caso PUBLICADO contiene al menos dos opciones de respuesta. La restricción exige una estructura completa para las decisiones publicadas y permite elaborar decisiones incompletas mientras el caso está en BORRADOR.

```ocl
context Decision
inv MinimoDosOpcionesAlPublicar:
  self.caso.estado = EstadoCaso::PUBLICADO implies
    self.opciones->size() >= 2
```

OCL-04 — En un caso publicado los escenarios conservan un orden predefinido: para dos escenarios del mismo caso no se repite su posición. Ninguna selección del estudiante altera esa secuencia (RN-02). La comprobación de orden único se refleja además en UK(caso_id, orden) del modelo relacional.

```ocl
context Caso
inv EscenariosConOrdenUnico:
  self.escenarios->isUnique(e | e.orden)
```

OCL-05 — La programación de una sesión exige que el inicio previsto sea anterior al fin previsto. Las fechas y horas son obligatorias según RF-18 y se almacenan en Sesion.

```ocl
context Sesion
inv VentanaProgramadaCoherente:
  self.inicioPrevisto < self.finPrevisto
```

### 10.2 Reglas de integridad y condiciones de operación

OCL-06 — El puntaje asignado a cada opción de respuesta es mayor o igual a cero y no supera el puntaje máximo de su decisión. La invariante preserva la validez de los valores usados en la evaluación.

```ocl
context OpcionDecision
inv PuntajeEnRango:
  self.puntaje >= 0 and
  self.puntaje <= self.decision.puntajeMaximo
```

OCL-07 — Todo caso PUBLICADO tiene una escena inicial definida y dicha escena pertenece a la colección de escenas del caso. La condición asegura que la simulación comience en un elemento válido.

```ocl
context Caso
inv EscenaInicialValidaAlPublicar:
  self.estado = EstadoCaso::PUBLICADO implies
    (self.escenaInicial->notEmpty() and
     self.escenas->includes(self.escenaInicial))
```

OCL-08 — La operación registrarRespuesta se ejecuta únicamente cuando la sesión está EN_CURSO y el intento del estudiante permanece EN_PROGRESO. La precondición impide registrar respuestas en sesiones cerradas o intentos finalizados.

```ocl
context Intento::registrarRespuesta(r : Respuesta)
pre SesionPermiteResponder:
  self.sesion.estado = EstadoSesion::EN_CURSO and
  self.estado = EstadoIntento::EN_PROGRESO
```

OCL-09 — Todo intento nuevo corresponde a un estudiante autorizado en la convocatoria de su sesión, que debe permanecer EN_CURSO. Este control se aplica en el momento de iniciar el intento y se complementa con el límite temporal de RN-15.

```ocl
context Participacion::iniciar()
pre ConvocatoriaYEstadoValidos:
  self.convocatoria.autorizada = true and
  self.convocatoria.sesion.estado = EstadoSesion::EN_CURSO
```

La autorización por código y el instante de ingreso se comprueban además en el servicio de acceso; la expresión anterior representa la condición mínima del modelo de dominio.

### 10.3 Reglas complementarias del validador JSON

El validador JSON debe comprobar: identificadores únicos, estado válido, escenarios con orden único y consecutivo, preguntas referidas a escenarios existentes, opciones asociadas a su pregunta y puntajes dentro del máximo de la pregunta; al publicar, también rúbrica completa, ponderaciones correctas, justificaciones y presencia del contenido académico mínimo (RN-03/RN-05/RN-11). No se interpretan las opciones como saltos narrativos: las respuestas no modifican el orden de los escenarios (RN-02).

### 10.4 Casos de prueba de validación

Válido: caso PUBLICADO con escenarios E01 y E02 ordenados (1 y 2), preguntas con opciones y rúbrica asociada; un intento EN_PROGRESO con convocatoria autorizada y sesión EN_CURSO. Inválido: orden duplicado de escenarios; opción sin justificación; puntaje fuera del máximo; referencias a preguntas inexistentes; rúbrica incompleta; dos intentos en progreso del mismo estudiante. Estas son especificaciones de prueba, no resultados de una ejecución automatizada.

## 11. Sintaxis concreta en JSON

### 11.1 Convenciones, estructura y validación

Se adopta un objeto raíz con versionDSL y caso. El caso contiene título, contexto, tiempo estimado, estado, escenarios ordenados, preguntas, opciones y rúbrica. Los escenarios incluyen un atributo orden y las opciones no declaran siguienteEscena ni conclusion: la navegación de preguntas pertenece a Sesion y no altera el orden académico (RN-02/RN-14). El JSON Schema puede comprobar tipos y obligatoriedad; un validador complementario verifica referencias, unicidad, pesos y restricciones de publicación.

### 11.2 Ejemplo JSON válido: definición académica

```json
{
"versionDSL": "1.0",
"caso": {
"id": "CASO-001", "titulo": "Conflicto en una organización",
"contexto": "Desacuerdo entre integrantes de un equipo.",
"estado": "PUBLICADO", "tiempoEstimadoMin": 40,
"escenarios": [
{"id": "E01", "orden": 1, "contexto": "Reunión inicial",
"preguntas": [{"id": "P01", "enunciado": "¿Cómo intervenir?",
"puntajeMaximo": 5, "opciones": [
{"id": "OP01", "texto": "Escuchar a todos", "esCorrecta": true, "puntaje": 5, "justificacion": "Reconoce perspectivas."},
{"id": "OP02", "texto": "Imponer la solución", "esCorrecta": false, "puntaje": 0, "justificacion": "Omite intereses."}]}]},
{"id": "E02", "orden": 2, "contexto": "Construcción de acuerdos",
"preguntas": [{"id": "P02", "enunciado": "¿Cómo cerrar el acuerdo?",
"puntajeMaximo": 5, "opciones": [
{"id": "OP03", "texto": "Verificar compromisos", "esCorrecta": true, "puntaje": 5, "justificacion": "Aclara tareas."},
{"id": "OP04", "texto": "Cerrar sin verificar", "esCorrecta": false, "puntaje": 0, "justificacion": "Deja ambigüedad."}]}]}
], "rubrica": {"criterios": [
{"id": "C01", "nombre": "Análisis", "ponderacion": 0.5},
{"id": "C02", "nombre": "Intervención", "ponderacion": 0.5}]}
}
}
```

### 11.3 Ejemplo inválido y errores esperados

```json
{
"versionDSL": "1.0", "caso": {
"id": "CASO-ERR", "estado": "PUBLICADO", "tiempoEstimadoMin": 0,
"escenarios": [
{"id": "E01", "orden": 1, "contexto": "Escena A", "preguntas": [
{"id": "P01", "enunciado": "Decisión", "puntajeMaximo": 5, "opciones": [
{"id": "OP01", "texto": "Opción", "esCorrecta": true, "puntaje": 7}]}]},
{"id": "E02", "orden": 1, "contexto": "Escena B", "preguntas": []}
], "rubrica": {"criterios": []}
}
}
```

Errores esperados: tiempo estimado no positivo; posición de escenario duplicada (RN-02); puntaje 7 superior al máximo 5; opción sin justificación (RN-11); rúbrica sin criterios (RN-03). Ejemplo sintácticamente JSON válido y semánticamente inválido.

### 11.4 Integración con sesiones, respuestas y resultados

El caso publicado se almacena con identificador y versión. Una asignación refiere ese caso; una sesión referencia asignaciones y condiciones temporales; los intentos referencian estudiantes y sesiones; las respuestas registran opción, pregunta, marca temporal y puntaje; los resultados se calculan sobre la rúbrica. Estos registros se modelarán en UML y en el esquema relacional, sin incorporarlos a la definición portable de caso del DSL. Si el equipo decide especificar también mensajes de ejecución, deberán declararse como un esquema JSON independiente.

### 11.5 Trazabilidad y decisiones por confirmar

Confirmar contra la matriz definitiva: códigos RF/RN aplicables a publicación, modalidad de navegación, opciones mínimas, puntuación y rúbrica; estados de sesión e intento; política de reintentos; semántica de archivado; obligatoriedad de retroalimentación; y las asociaciones UML que permiten ejecutar las reglas OCL-03, 04 y 06–09. Esta sección se considera propuesta técnica integrada y no validación formal ejecutada en un evaluador OCL.

## Consideraciones para integración

- La definición portable del caso se separa de sesiones, convocatorias, intentos, respuestas y calificaciones, tal como se especifica en la fuente.
- Las OCL representan restricciones de diseño; además requieren validación de dominio y/o transacciones en la aplicación.
- La fuente alterna términos `Escenario`/`Escena`, `Pregunta`/`Decision` y `OpcionDecision`/`OpcionRespuesta` entre apartados. Es necesaria una normalización del metamodelo antes de usar estas expresiones como OCL ejecutable.
- Las reglas de mínimo dos opciones y escena inicial figuran en la especificación del primer corte; deben contrastarse con los requisitos vigentes antes de imponerse al MVP.
- Los ejemplos JSON mantienen el contenido del documento original; se adjuntan por separado para facilitar las pruebas.
