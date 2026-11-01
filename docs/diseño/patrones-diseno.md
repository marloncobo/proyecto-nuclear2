# Patrones de diseño propuestos para Psicoapp

> **Estado:** guía de implementación; Facade tiene una base parcial existente y los demás patrones están propuestos.
> **Origen:** documento académico *Primer_Corte_Patrones_Diseno_Psicologia_Social*.  
> **Proyecto actual:** Psicoapp.  
> **Arquitectura:** monolito modular Spring Boot, módulos `identity`, `academic`, `cases`, `sessions`, `participation`, `evaluation`, `notifications`, `reports` y `shared`.

## 1. Propósito y criterio de uso

Este archivo traduce la propuesta académica de patrones en tareas verificables para el equipo. **Un patrón no es una funcionalidad por sí solo**: se implementará si simplifica una necesidad concreta respaldada por los requisitos vigentes. Antes de programar, contrastar los identificadores RF/RN con `docs/requisitos/`.

La organización sugerida respeta `api/`, `application/`, `domain/` e `infrastructure/` dentro de cada módulo. Mantener reglas de negocio en `domain` y coordinación en `application`.

Las tablas de la sección 3 documentan las clases e interfaces propuestas para implementar cada patrón. Conservan los participantes nombrados en el texto del documento y completan sus colaboradores, paquetes y operaciones como **diseño técnico derivado**, no como transcripción de las firmas de los diagramas UML. Los paquetes parten de `com.psicoapp`. Salvo los elementos marcados como existentes, están pendientes de implementación. Los identificadores del diseño académico se adaptan al español conforme a la convención obligatoria siguiente.

**Base existente revisada:** `FachadaAdministracion`, `ControladorEspaciosAcademicos`, `RepositorioEspaciosAcademicos`, `DirectorioIdentidad`, `ServicioUsuarios` y `VerificadorAccesoEstudiante`. Los módulos restantes conservan su estructura inicial. No crear duplicados de estas clases ni declarar completos los RF por contar con una fachada mínima.

### Convención obligatoria de nombres

Los identificadores propios del proyecto deben estar en español: clases,
interfaces, enumeraciones, DTO, métodos, atributos, variables, pruebas, campos
JSON, rutas HTTP y claves de configuración propias. Usar `PascalCase` para
tipos y `camelCase` para métodos y variables, sin tildes ni letra ñ en los
identificadores; las constantes usan `UPPER_SNAKE_CASE` con palabras españolas.
Las clases de prueba nuevas se nombran `Prueba<Clase>` y Maven las descubre
mediante su configuración de Surefire.

Se conservan las firmas y símbolos impuestos por Java, Spring, JUnit, SQL y
otras dependencias, por ejemplo `authenticate`, `supports`, `toString` y
`spring.datasource.url`. También se conservan los nombres de paquetes y la
clase de arranque preexistentes de la arquitectura (`identity`, `academic`,
`application`, `domain`, `infrastructure`, `shared`, `PsicoappApplication`).
Estas excepciones de integración no autorizan nombres nuevos en inglés.
Los nombres técnicos de patrones como Facade y Builder pueden citarse en la
explicación, pero los identificadores se escriben en español: por ejemplo,
`FachadaAdministracion`, `ConstructorCaso` y `EstadoSesion`.

## 2. Resumen de aplicación

| Patrón | Módulo principal | Problema que aborda | RF citados en el documento | Prioridad propuesta |
| --- | --- | --- | --- | --- |
| Builder | `cases` | Construcción paso a paso de casos complejos | RF-05, RF-07, RF-17 | Condicional: cuando ensamblar el agregado lo requiera |
| Facade | `academic` / aplicación orquestadora | Simplificar operaciones que combinan varios casos de uso | RF-03, RF-04, RF-15 | Base parcial existente; ampliar por caso de uso |
| Proxy | `participation`, con `identity` | Restringir acceso a operaciones de participación | RF-02, RF-20 | Alta para autorización, patrón Proxy opcional |
| State | `sessions` | Validar transiciones del ciclo de vida de sesiones | RF-19, RF-25 | Alta para reglas de estado |
| Strategy | `evaluation` | Intercambiar algoritmos de calificación | RF-13, RF-14, RF-24 | Solo si hay más de un algoritmo aprobado |
| Observer | `notifications` con productores de eventos | Desacoplar notificaciones de procesos académicos | RF-21, RF-27 | Cuando se implementen notificaciones |
| Template Method | `cases` | Mantener secuencia fija de validaciones de publicación | RF-09 | Condicional: si hay variantes por herencia |

## 3. Guías de implementación por patrón

### 3.1 Builder — creación de casos

**Necesidad:** formar casos que incluyan escenarios, preguntas y elementos relacionados sin concentrar todas las decisiones de construcción en un constructor enorme.

**Qué hacer:**
1. Identificar el agregado y sus invariantes en `cases/domain` según el modelo vigente.
2. Diseñar `ConstructorCaso` o una fábrica/servicio de aplicación que añada elementos progresivamente; no duplicar una API CRUD sin necesidad.
3. Construir un `Caso` válido para borrador; mantener la validación de publicación como proceso independiente.
4. Probar casos completos, elementos opcionales y datos inválidos.

**No hacer:** usar Builder para guardar datos en la BD, autorizar usuarios o publicar el caso automáticamente.

**Clases e interfaces a implementar — RF-05/RF-07/RF-17; apoyo RF-06/RF-08 y RN-02/RN-11:**

| Participante / tipo | Paquete | Responsabilidad y operaciones propuestas |
| --- | --- | --- |
| `ConstructorCaso` — interfaz Builder | `cases.domain` | Pasos `definirDatos(...)`, `agregarEscenario(...)`, `asociarRubrica(...)` y `construir(): Caso`. |
| `ConstructorCasoAcademico` — implementación | `cases.domain` | Ensamblar un borrador por pasos. Usar una instancia por construcción, sin compartir estado mutable entre solicitudes. |
| `Caso` — producto | `cases.domain` | Contener información académica, estado y escenarios ordenados; comprobar invariantes básicas permitiendo borradores incompletos. |
| `Escenario`, `Pregunta`, `OpcionRespuesta` — componentes | `cases.domain` | Modelar orden, enunciados y opciones con corrección y justificación. La respuesta del estudiante pertenece a `participation`. |
| `Rubrica`, `CriterioRubrica` — contenido académico | `cases.domain` | Representar criterios y ponderaciones del caso; proporcionar una instantánea para evaluar por contrato público. |
| `ServicioCreacionCaso` — cliente | `cases.application` | `crear(datos): CasoId`: comprobar permiso con `identity`, ensamblar y guardar mediante el puerto `RepositorioCasos` de esta capa. |

**Colaboración:** `ServicioCreacionCaso → ConstructorCaso ← ConstructorCasoAcademico → Caso`. El adaptador de `RepositorioCasos` pertenece a `cases.infrastructure`. `construir()` no persiste ni publica; la validación completa de publicación corresponde a la sección 3.7. No se requiere un director adicional sin recetas reutilizables de construcción.

**Criterio de aceptación:** se puede ensamblar un caso y detectar composiciones inválidas sin que el controlador conozca detalles de construcción.

### 3.2 Facade — coordinación de consultas y administración

**Necesidad:** ofrecer operaciones claras cuando una acción requiere coordinar funcionalidades de grupos, usuarios y resultados.

**Qué hacer:**
1. Identificar un caso de uso real que requiera varios contratos de módulos.
2. Exponer una operación de aplicación específica (por ejemplo, `ConsultarResumenGrupo`) que coordine esos contratos.
3. Dejar la lógica de dominio en los módulos propietarios; usar DTO para consultas transversales.
4. Probar coordinación, manejo de fallos y permisos.

**No hacer:** crear una clase `FachadaAdministracion` gigante que dependa de todos los repositorios y concentre todas las funcionalidades del proyecto.

**Clases y contratos — RF-03/RF-04/RF-15; RN-10:**

| Participante / tipo | Paquete | Responsabilidad y operaciones |
| --- | --- | --- |
| `FachadaAdministracion` — fachada existente | `academic.application` | Implementación parcial de la fachada académica: ya ofrece `crearEspacio(idDocente, nombre, periodo)`, `consultarEspacio(idDocente, id)` y `listarEspacios(idDocente)` para espacios. No consulta resultados todavía. |
| `ControladorEspaciosAcademicos` — cliente existente | `academic.api` | Delegar las operaciones HTTP de espacios en la fachada; representa parte del rol de `ControladorAdministracion`. |
| `DirectorioIdentidad` — contrato existente | `identity.application` | `exigirDocente(idUsuario)` y `registrarEstudiante(idDocente, correo, nombre)` sin revelar credenciales ni entidades internas. La fachada actual utiliza la primera operación. |
| `ServicioUsuarios` — servicio existente | `identity.application` | Mantener la administración de docentes, roles y permisos en su módulo, sin trasladar reglas a la fachada de espacios. |
| `RepositorioEspaciosAcademicos` — puerto existente | `academic.application` | Guardar y consultar espacios mediante el adaptador existente `RepositorioEspaciosAcademicosJdbc`. |
| `FachadaConsultaParticipaciones` — fachada pendiente | `reports.application` | `consultarPorEstudiante(docenteId, estudianteId, casoId)`: coordinar autorización, respuestas y resultados para RF-15 y devolver un DTO `ResumenParticipacion`. |
| `ConsultaParticipaciones`, `ConsultaResultados` — contratos pendientes | `participation.application`, `evaluation.application` | Consultar progreso/respuestas y resultados publicados mediante DTO y controles de acceso del módulo propietario. |

**Colaboración:** reutilizar `ControladorEspaciosAcademicos → FachadaAdministracion → DirectorioIdentidad / RepositorioEspaciosAcademicos`. Para seguimiento, la fachada de `reports` consume contratos públicos sin acceder a repositorios ajenos; los productores no dependen de `reports`. `FachadaAdministracion` adapta el nombre académico al español y conserva un alcance parcial; no es una fachada global de todos los módulos.

**Criterio de aceptación:** una operación externa tiene una interfaz sencilla sin romper los límites de los módulos.

### 3.3 Proxy — acceso protegido a participación

**Necesidad:** impedir que usuarios no autorizados inicien o modifiquen intentos ajenos.

**Qué hacer:**
1. Definir claramente las reglas de autorización: rol, identidad, asignación y permiso para participar.
2. Garantizar su aplicación en el backend mediante el sistema de seguridad elegido (`shared/security` e `identity`).
3. **Si es útil un Proxy explícito**, crear una interfaz de servicio y un proxy que valide autorización antes de delegar al servicio real de `participation`.
4. Probar acceso autorizado, usuario ajeno, sesión no disponible y usuario sin asignación.

**No hacer:** confiar exclusivamente en controles del frontend; tampoco duplicar validaciones de Spring Security solo para demostrar el patrón.

**Clases e interfaces a implementar — RF-02/RF-20; apoyo RF-22/RF-25 y RN-04/RN-07/RN-15:**

| Participante / tipo | Paquete | Responsabilidad y operaciones propuestas |
| --- | --- | --- |
| `ServicioParticipacion` — interfaz Subject | `participation.application` | Contrato común `iniciar(actor, sesionId, codigo)`, `responder(actor, intentoId, respuesta)` y `finalizar(actor, intentoId)`. El actor procede de la autenticación del backend. |
| `IntermediarioParticipacion` — proxy de protección | `participation.application` | Implementar el contrato, verificar identidad, convocatoria, autorización y propiedad del intento, y delegar solo si procede. |
| `ServicioParticipacionReal` — servicio real | `participation.application` | Ejecutar los casos de uso y persistir intentos/respuestas; aplicar invariantes transaccionales también ante entradas internas. |
| `AutorizacionParticipacion` — colaborador | `participation.application` | `verificarInicio(...)` y `verificarPropietario(...)`: consultar identidad, convocatoria, código, sesión y autorización de reintento por contratos públicos. |
| `PoliticaIngreso` — regla temporal | `participation.domain` | `validar(instante, inicioPrevisto, duracionProgramada)`: comprobar ingreso tardío; conciliar antes RF-22/RN-15 según sección 4. |
| `VerificadorAccesoConvocatoria` — adaptador pendiente | `sessions.infrastructure` | Implementar el puerto existente `identity.application.VerificadorAccesoEstudiante.verificar(idEstudiante, codigo)` con convocatorias de `sessions`. No sustituye la autorización de participación. |

**Colaboración:** `controlador → ServicioParticipacion (IntermediarioParticipacion) → ServicioParticipacionReal`. Configurar la inyección para que el controlador reciba el proxy y este reciba explícitamente el servicio real, evitando autoinyección o ambigüedad. Consultar el estado de sesión por contrato de `sessions.application`, sin importar su entidad interna.

**Criterio de aceptación:** no puede ejecutarse una operación de participación fuera de las condiciones de acceso autorizadas.

### 3.4 State — ciclo de vida de sesiones

**Necesidad:** controlar transiciones y operaciones distintas en `SIN_INICIAR`, `EN_CURSO` y `FINALIZADO`.

**Qué hacer:**
1. Definir una tabla de transiciones y eventos permitidos con los requisitos vigentes.
2. Separar **estado de la sesión programada** de **estado/progreso de cada intento individual**.
3. Implementar reglas de transición en `sessions/domain`; valorar clases `EstadoSesion` cuando el comportamiento por estado sea suficientemente complejo.
4. Activar el cierre docente y el cierre automático mediante casos de uso de aplicación. El programador temporal solicita la transición, no se salta las reglas.
5. Probar transiciones válidas, repetidas, prohibidas y cierres por vencimiento.

**Clases e interfaces a implementar — RF-19/RF-25; RN-12:**

| Participante / tipo | Paquete | Responsabilidad y operaciones propuestas |
| --- | --- | --- |
| `Sesion` — contexto | `sessions.domain` | Mantener estado y fechas programadas; delegar `iniciar()`, `validarParticipacion()` y `finalizar(motivo)`. |
| `EstadoSesion` — interfaz State | `sessions.domain` | Definir operaciones de ciclo de vida sobre el contexto sin HTTP, SQL ni planificación temporal. |
| `SinIniciar` — estado concreto | `sessions.domain` | Permitir inicio docente y denegar participación; admitir cierre automático por RN-12 aunque nunca se haya iniciado. |
| `EnCurso` — estado concreto | `sessions.domain` | Permitir participación sujeta a las demás reglas y cierre docente/automático; rechazar segundo inicio. |
| `Finalizado` — estado concreto | `sessions.domain` | Denegar inicio y participación; hacer idempotente una solicitud repetida de cierre. |
| `ServicioGestionSesiones` — coordinador | `sessions.application` | `iniciar(...)`, `finalizar(...)`, `cerrarVencidas(ahora)`: autorizar, ejecutar transiciones y guardar mediante el puerto `RepositorioSesiones`. |
| `PlanificadorCierreSesiones` — disparador | `sessions.infrastructure` | Invocar `cerrarVencidas` con reloj inyectable; no modificar directamente el estado en SQL. |

| Estado actual | Solicitud | Resultado |
| --- | --- | --- |
| `SIN_INICIAR` | Inicio docente autorizado | `EN_CURSO` |
| `EN_CURSO` | Cierre docente autorizado | `FINALIZADO` |
| `SIN_INICIAR` o `EN_CURSO` | Cierre automático al vencer `finPrevisto + 1 día` | `FINALIZADO` |
| `FINALIZADO` | Participar o reiniciar | Rechazo |
| `FINALIZADO` | Repetir cierre | Sin cambio ni efectos duplicados |

**Colaboración:** `PlanificadorCierreSesiones → ServicioGestionSesiones → Sesion → EstadoSesion`. La transición automática desde `SIN_INICIAR` completa la propuesta lineal del documento para cumplir RN-12 cuando una sesión nunca se inició. El cierre manual anticipado desde ese estado requiere una decisión funcional; no se presupone. Persistir un identificador estable del estado y reconstruir su objeto al cargar. Proteger las transiciones frente a participación/cierre concurrentes. La expiración de intentos individuales corresponde a `participation` según RN-17/RN-18.

**Criterio de aceptación:** no se admiten participaciones en una sesión cerrada ni transiciones imposibles. RF-19/RN-12 fijan el cierre automático un día después de la fecha y hora programadas de finalización si el docente no la ha cerrado.

### 3.5 Strategy — cálculo de calificaciones

**Necesidad:** reemplazar el algoritmo de calificación sin modificar el servicio evaluador, **si existen fórmulas alternativas aprobadas**.

**Qué hacer:**
1. Confirmar reglas de rúbrica, pesos y penalizaciones en `docs/requisitos/` antes de diseñar fórmulas.
2. Si hay varias modalidades reales, declarar `EstrategiaCalculoNota` y las implementaciones necesarias en `evaluation`.
3. Hacer que `ServicioCalificacionIntentos` seleccione una estrategia y suministre respuestas/rúbrica a `EvaluadorCalificacion`.
4. Probar el cálculo, preguntas no respondidas, última respuesta válida y límites de la escala.

**No hacer:** implementar obligatoriamente `EstrategiaCalculoPorPuntos` y `EstrategiaCalculoPonderado` sin que los requisitos confirmen ambas modalidades.

**Clases e interfaces a implementar — RF-13/RF-14/RF-24; RN-09/RN-16/RN-19:**

| Participante / tipo | Paquete | Responsabilidad y operaciones propuestas |
| --- | --- | --- |
| `EstrategiaCalculoNota` — interfaz Strategy | `evaluation.domain` | `calcular(respuestas, rubrica): ResultadoCalificacion`; incluir preguntas sin responder con cero. |
| `EstrategiaCalculoPorPuntos` — alternativa condicionada | `evaluation.domain` | Aplicar fórmula por puntos si se aprueba; no inventar escala, normalización ni penalizaciones. |
| `EstrategiaCalculoPonderado` — alternativa condicionada | `evaluation.domain` | Aplicar criterios y pesos de la rúbrica si esta modalidad se confirma. |
| `EvaluadorCalificacion` — contexto | `evaluation.domain` | Recibir una estrategia y delegar `evaluar(...)`; no guardar respuestas ni decidir permisos. |
| `ServicioCalificacionIntentos` — coordinador | `evaluation.application` | Obtener instantáneas por contratos públicos, seleccionar la estrategia aprobada, invocarla a través del contexto y persistir el resultado por un puerto propio. |
| `RespuestaEvaluable`, `RubricaEvaluacion`, `ResultadoCalificacion` — valores | `evaluation.domain` | Representar última respuesta por pregunta, criterios/pesos y desglose/nota. No importar entidades internas de otros módulos. |

**Colaboración:** `ServicioCalificacionIntentos → EvaluadorCalificacion → EstrategiaCalculoNota`. `RubricaEvaluacion` es una instantánea de la rúbrica académica de `cases`, no otro catálogo editable. La selección pertenece a aplicación; el evaluador recibe la estrategia. Persistencia y presentación diferida de feedback son responsabilidades separadas; Strategy no satisface por sí solo RF-14.

**Criterio de aceptación:** la nota cumple las reglas vigentes; nuevas modalidades pueden añadirse sin modificar el contrato del evaluador.

### 3.6 Observer — avisos a partir de eventos

**Necesidad:** una programación de sesión o publicación de resultados puede generar notificaciones sin acoplar el caso de uso a cada canal.

**Qué hacer:**
1. Definir eventos de negocio con datos mínimos (`SesionProgramada`, `ResultadosPublicados`) sin filtrar entidades internas entre módulos.
2. Emitir eventos después de completar correctamente la operación de negocio.
3. Suscribir manejadores de `notifications` para correo o notificación interna, según lo requerido.
4. Definir tratamiento de fallos, reintentos e idempotencia; no revertir una operación académica ya completada por un error de envío.
5. Probar que un evento activa el aviso correcto sin duplicaciones indebidas.

**Criterio de aceptación:** el productor no conoce las implementaciones concretas de envío y el fallo del canal se controla.

**Clases e interfaces a implementar — RF-21/RF-27:**

| Participante / tipo | Paquete | Responsabilidad y operaciones propuestas |
| --- | --- | --- |
| `EventoNotificacion` — contrato de evento | `shared.events` | Identificador de evento, instante y datos mínimos, sin entidades internas ni credenciales generales de usuario. |
| `SesionProgramada`, `ConvocatoriaModificada`, `ResultadosPublicados`, `RetroalimentacionAgregada` — eventos inmutables | `shared.events` | Cubrir convocatoria inicial, altas/bajas, nota disponible y feedback. Proteger el código de convocatoria y evitar registrarlo en logs. |
| `NotificacionSujeto` — Subject/publicador | `shared.events` | Puerto `publicar(evento)` consumido por servicios productores sin dependencia de canales concretos. |
| `SpringNotificacionSujeto` — adaptador | `shared.configuration` | Implementar el publicador con eventos de Spring, manteniendo el framework fuera del dominio. |
| `ObservadorNotificacion` — interfaz Observer | `notifications.application` | Declarar `soporta(evento)` y `actualizar(evento)` para los canales. |
| `ObservadorCorreoNotificacion` — observador concreto | `notifications.application` | Preparar correo de convocatoria/cambios con mensaje, URL y código según corresponda, y enviarlo por `PuertoCorreo`. |
| `ObservadorAvisoInterno` — observador concreto | `notifications.application` | Crear avisos de nota o feedback mediante `RepositorioAvisos`, sin anticipar resultados durante el intento. |
| `ReceptorEventosNotificacion` — puente del framework | `notifications.infrastructure` | Recibir eventos después del commit y despachar a observadores compatibles; ubicar aquí `@TransactionalEventListener` si se usa Spring. |
| `ServicioEntregaNotificaciones` — control de entrega | `notifications.application` | Registrar entregas por evento/destinatario/canal, deduplicar y gestionar reintentos. `PuertoCorreo` y `RepositorioAvisos` son puertos de esta capa con adaptadores en infraestructura. |

**Colaboración:** `productor → NotificacionSujeto → listener → ObservadorNotificacion → puerto de canal`. La suscripción de Spring sustituye el registro manual de observadores; no mantener dos listas simultáneas. Un evento en memoria después del commit no garantiza recuperación ante caída: para reintentos durables, registrar eventos pendientes en una outbox dentro de la transacción productora y procesarlos con deduplicación. Un fallo de envío no revierte la operación académica confirmada.

### 3.7 Template Method — validaciones de publicación

**Necesidad:** validar estructura, preguntas y rúbrica antes de publicar un caso, manteniendo un orden estable.

**Qué hacer:**
1. Precisar condiciones de publicación con RF-09 y RN vigentes.
2. Si hay variantes por tipo de caso, considerar `PlantillaValidacionPublicacion` con `validarEstructura`, `validarPreguntas` y `validarRubrica`, especializadas por subclases.
3. Acumular errores en `ResultadoValidacion` y bloquear publicación si se incumplen restricciones.
4. Probar casos incompletos, rúbricas inválidas y publicaciones exitosas.

**Alternativa más simple:** un servicio de validación compuesto por validadores, sin herencia, si existe un único flujo fijo. No introducir Template Method artificialmente.

**Criterio de aceptación:** ningún caso inválido cambia a publicado y se reportan los errores de validación.

**Clases a implementar — RF-09; RN-03/RN-05:**

| Participante / tipo | Paquete | Responsabilidad y operaciones propuestas |
| --- | --- | --- |
| `PlantillaValidacionPublicacion` — clase abstracta | `cases.domain` | Método `final validar(Caso): ResultadoValidacion`; ejecutar en orden `validarEstructura`, `validarPreguntas`, `validarRubrica`. Los pasos protegidos se especializan por herencia y acumulan errores. |
| `ValidacionCasoAcademico` — subclase concreta | `cases.domain` | Validar estructura/reglas aplicables del DSL, preguntas/opciones/justificaciones y rúbrica asociada. No imponer mínimos aún no aprobados. |
| `ResultadoValidacion`, `ErrorValidacion` — resultados | `cases.domain` | Exponer `esValido()` y errores con código, ruta y mensaje; reportar varios problemas sin publicar parcialmente. |
| `ServicioPublicacionCaso` — cliente | `cases.application` | `publicar(actor, casoId)`: autorizar al docente responsable, cargar borrador, validar y cambiar a publicado solo si es válido; guardar en la misma transacción. |

**Colaboración:** `ServicioPublicacionCaso → PlantillaValidacionPublicacion ← ValidacionCasoAcademico`. Builder entrega un borrador potencialmente incompleto; la plantilla comprueba las condiciones académicas para publicar. Validar DSL no presupone implementar Interpreter. Probar el orden de etapas y que un error conserva el borrador. La entrega propone una subclase concreta; no inventar tipos de caso para añadir subclases.

## 4. Trazabilidad validada y consideraciones de implementación

La sección **4. Trazabilidad y coherencia** de la versión actualizada del documento académico confirma estas asociaciones:

Los identificadores de esta tabla se adaptan al español conforme a la convención del repositorio. No son una transcripción literal de los nombres del UML original; esta adaptación no modifica el documento académico de referencia.

| Necesidad | RF | Patrón | Elemento UML propuesto |
| --- | --- | --- | --- |
| Construcción de casos académicos | RF-05, RF-07, RF-17 | Builder | `ConstructorCaso` |
| Coordinación de administración y consulta | RF-03, RF-04, RF-15 | Facade | `FachadaAdministracion` |
| Protección de acceso a sesiones | RF-02, RF-20 | Proxy | `IntermediarioParticipacion` |
| Ciclo de vida de las sesiones | RF-19, RF-25 | State | `EstadoSesion` |
| Cálculos alternativos de nota | RF-13, RF-14, RF-24 | Strategy | `EstrategiaCalculoNota` |
| Notificaciones a partir de eventos | RF-21, RF-27 | Observer | `ObservadorNotificacion` |
| Validación secuencial de publicación | RF-09 | Template Method | `PlantillaValidacionPublicacion` |

La propuesta de **Template Method** es la seleccionada para publicación; **Chain of Responsibility** permanece como alternativa no prioritaria. **Strategy** se vincula a los requisitos de evaluación, no a RF-10. Estas relaciones están corregidas en el documento fuente y ya no constituyen inconsistencias pendientes.

### Reglas confirmadas y decisiones pendientes

1. **Navegación confirmada (RN-14):** con retroceso habilitado se puede volver, cambiar respuestas y avanzar sin responder; sin retroceso se debe responder antes de avanzar y no se permite regresar. Tras finalizar no se modifican respuestas. No confundir con Strategy para notas.
2. **Cierre confirmado (RF-19/RN-12):** un día después del fin programado si el docente no ha cerrado la sesión. Precisar zona horaria y límites temporales al implementar sin cambiar el plazo de negocio.
3. **Cálculo de calificaciones:** los algoritmos por puntos y ponderado son ejemplos de diseño. Implementar varias estrategias solo si las modalidades están definidas en los requisitos.
4. **Requisitos no funcionales:** mantenibilidad, extensibilidad y bajo acoplamiento son criterios técnicos derivados, no RNF formalmente aprobados en este documento.
5. **Ingreso tardío:** RN-15 fija la mitad del tiempo programado de la sesión desde su inicio, mientras RF-22 conserva una nota pendiente sobre su base de cálculo. Conciliar los textos antes de cerrar `PoliticaIngreso`, sin inventar un umbral.

Consultar [reglas de negocio](../requisitos/reglas-negocio.md), [requisitos funcionales](../requisitos/requisitos-funcionales.md) y [arquitectura del backend](../../backend/README.md) para mantener estos contratos alineados.

## 5. Plan sugerido de trabajo y tickets

| Ticket sugerido | Trabajo verificable | Módulo |
| --- | --- | --- |
| `docs: confirmar-trazabilidad-patrones` | Revisar RF/RN y aprobar dónde aplica cada patrón | `docs` |
| `feat: ciclo-vida-sesiones` | Definir/transicionar estados de sesiones con pruebas | `sessions` |
| `feat: acceso-participacion` | Implementar y probar autorización del lado del servidor | `identity`, `participation` |
| `feat: validacion-publicacion-casos` | Validar requisitos de publicación con errores comprensibles | `cases` |
| `feat: calificacion-rubrica` | Calcular notas con reglas vigentes y pruebas; Strategy solo si procede | `evaluation` |
| `feat: construccion-casos` | Ensamblar casos y validar composición; Builder solo si aporta valor | `cases` |
| `feat: notificaciones-eventos` | Emitir y consumir eventos de aviso, manejar fallos | `notifications` |
| `refactor: fachada-administracion` | Simplificar coordinación de operaciones reales cuando aparezca complejidad | `academic` / aplicación |

## 6. Definición de terminado para cualquier patrón

- [ ] Requisito o regla vigente identificados y enlazados.
- [ ] Problema real y alternativa más simple documentados.
- [ ] Responsabilidad del módulo propietario respetada.
- [ ] Clases, interfaces, métodos y colaboraciones de la sección 3 implementados o adaptación justificada; distinguir existentes, nuevas y variantes condicionadas.
- [ ] Implementación sin dependencias circulares ni conocimiento de `infrastructure` desde `domain`.
- [ ] Pruebas unitarias de las reglas y, cuando corresponda, pruebas de integración.
- [ ] Diagrama o nota de diseño actualizado si la estructura se aleja de la propuesta inicial.
- [ ] Pull Request explica por qué se usó el patrón y qué comportamiento cambió.

## 7. Fuente y mantenimiento

Documento revisado: [Análisis y aplicación de patrones de diseño — primer corte](https://docs.google.com/document/d/1HNopS8XdbuWhGxF1Ptrd_EFtbzTmdlaF/edit), archivo Word alojado en Drive. Se consultaron las descripciones de aplicación, participantes y trazabilidad de las secciones 3 y 4. Revisión: 30-10-2026.

Este Markdown es una **guía técnica derivada**, no una transcripción literal de la entrega académica. Ante discrepancias, prevalecen los requisitos y reglas de negocio oficialmente vigentes del proyecto. Revisar este archivo cuando cambien los requisitos o los límites de módulos.
