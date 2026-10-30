# Patrones de diseño propuestos para Psicoapp

> **Estado:** guía de implementación propuesta, no evidencia de código implementado.  
> **Origen:** documento académico *Primer_Corte_Patrones_Diseno_Psicologia_Social*.  
> **Proyecto actual:** Psicoapp.  
> **Arquitectura:** monolito modular Spring Boot, módulos `identity`, `academic`, `cases`, `sessions`, `participation`, `evaluation`, `notifications`, `reports` y `shared`.

## 1. Propósito y criterio de uso

Este archivo traduce la propuesta académica de patrones en tareas verificables para el equipo. **Un patrón no es una funcionalidad por sí solo**: se implementará si simplifica una necesidad concreta respaldada por los requisitos vigentes. Antes de programar, contrastar los identificadores RF/RN con `docs/requisitos/`.

La organización sugerida respeta `api/`, `application/`, `domain/` e `infrastructure/` dentro de cada módulo. Las clases nombradas son ejemplos de diseño, no una obligación de nomenclatura ni clases ya existentes. Mantener reglas de negocio en `domain` y coordinación en `application`.

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
| Facade | `academic` / aplicación orquestadora | Simplificar operaciones que combinan varios casos de uso | RF-03, RF-04, RF-15 | Condicional: evitar fachada global |
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

**Criterio de aceptación:** se puede ensamblar un caso y detectar composiciones inválidas sin que el controlador conozca detalles de construcción.

### 3.2 Facade — coordinación de consultas y administración

**Necesidad:** ofrecer operaciones claras cuando una acción requiere coordinar funcionalidades de grupos, usuarios y resultados.

**Qué hacer:**
1. Identificar un caso de uso real que requiera varios contratos de módulos.
2. Exponer una operación de aplicación específica (por ejemplo, `ConsultarResumenGrupo`) que coordine esos contratos.
3. Dejar la lógica de dominio en los módulos propietarios; usar DTO para consultas transversales.
4. Probar coordinación, manejo de fallos y permisos.

**No hacer:** crear una clase `FachadaAdministracion` gigante que dependa de todos los repositorios y concentre todas las funcionalidades del proyecto.

**Criterio de aceptación:** una operación externa tiene una interfaz sencilla sin romper los límites de los módulos.

### 3.3 Proxy — acceso protegido a participación

**Necesidad:** impedir que usuarios no autorizados inicien o modifiquen intentos ajenos.

**Qué hacer:**
1. Definir claramente las reglas de autorización: rol, identidad, asignación y permiso para participar.
2. Garantizar su aplicación en el backend mediante el sistema de seguridad elegido (`shared/security` e `identity`).
3. **Si es útil un Proxy explícito**, crear una interfaz de servicio y un decorador/proxy que valide autorización antes de delegar al servicio real de `participation`.
4. Probar acceso autorizado, usuario ajeno, sesión no disponible y usuario sin asignación.

**No hacer:** confiar exclusivamente en controles del frontend; tampoco duplicar validaciones de Spring Security solo para demostrar el patrón.

**Criterio de aceptación:** no puede ejecutarse una operación de participación fuera de las condiciones de acceso autorizadas.

### 3.4 State — ciclo de vida de sesiones

**Necesidad:** controlar transiciones y operaciones distintas en `SIN_INICIAR`, `EN_CURSO` y `FINALIZADO`.

**Qué hacer:**
1. Definir una tabla de transiciones y eventos permitidos con los requisitos vigentes.
2. Separar **estado de la sesión programada** de **estado/progreso de cada intento individual**.
3. Implementar reglas de transición en `sessions/domain`; valorar clases `EstadoSesion` cuando el comportamiento por estado sea suficientemente complejo.
4. Activar el cierre docente y el cierre automático mediante casos de uso de aplicación. El programador temporal solicita la transición, no se salta las reglas.
5. Probar transiciones válidas, repetidas, prohibidas y cierres por vencimiento.

**Criterio de aceptación:** no se admiten participaciones en una sesión cerrada ni transiciones imposibles. Confirmar en las RN vigentes el cierre automático después del plazo establecido.

### 3.5 Strategy — cálculo de calificaciones

**Necesidad:** reemplazar el algoritmo de calificación sin modificar el servicio evaluador, **si existen fórmulas alternativas aprobadas**.

**Qué hacer:**
1. Confirmar reglas de rúbrica, pesos y penalizaciones en `docs/requisitos/` antes de diseñar fórmulas.
2. Si hay varias modalidades reales, declarar `EstrategiaCalculoNota` y las implementaciones necesarias en `evaluation`.
3. Hacer que `EvaluadorCalificacion` seleccione una estrategia y suministre respuestas/rúbrica.
4. Probar el cálculo, preguntas no respondidas, última respuesta válida y límites de la escala.

**No hacer:** implementar obligatoriamente `EstrategiaCalculoPorPuntos` y `EstrategiaCalculoPonderado` sin que los requisitos confirmen ambas modalidades.

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

### 3.7 Template Method — validaciones de publicación

**Necesidad:** validar estructura, preguntas y rúbrica antes de publicar un caso, manteniendo un orden estable.

**Qué hacer:**
1. Precisar condiciones de publicación con RF-09 y RN vigentes.
2. Si hay variantes por tipo de caso, considerar `PlantillaValidacionPublicacion` con `validarEstructura`, `validarPreguntas` y `validarRubrica`, especializadas por subclases.
3. Acumular errores en `ResultadoValidacion` y bloquear publicación si se incumplen restricciones.
4. Probar casos incompletos, rúbricas inválidas y publicaciones exitosas.

**Alternativa más simple:** un servicio de validación compuesto por validadores, sin herencia, si existe un único flujo fijo. No introducir Template Method artificialmente.

**Criterio de aceptación:** ningún caso inválido cambia a publicado y se reportan los errores de validación.

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

### Aspectos por confirmar antes de implementar

1. **Políticas de navegación:** el documento describe modos flexible y restringido. Consultar las reglas de negocio vigentes para precisar el comportamiento; no confundirlas con estrategias de cálculo de notas.
2. **Cierre automático:** comprobar el plazo exacto en las reglas de negocio y reflejarlo en las pruebas; evitar valores arbitrarios en el código.
3. **Cálculo de calificaciones:** los algoritmos por puntos y ponderado son ejemplos de diseño. Implementar varias estrategias solo si las modalidades están definidas en los requisitos.
4. **Requisitos no funcionales:** mantenibilidad, extensibilidad y bajo acoplamiento son criterios técnicos derivados, no RNF formalmente aprobados en este documento.

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
- [ ] Implementación sin dependencias circulares ni conocimiento de `infrastructure` desde `domain`.
- [ ] Pruebas unitarias de las reglas y, cuando corresponda, pruebas de integración.
- [ ] Diagrama o nota de diseño actualizado si la estructura se aleja de la propuesta inicial.
- [ ] Pull Request explica por qué se usó el patrón y qué comportamiento cambió.

## 7. Fuente y mantenimiento

Documento de referencia (versión con trazabilidad corregida): [Análisis y aplicación de patrones de diseño — primer corte](https://docs.google.com/document/d/1HNopS8XdbuWhGxF1Ptrd_EFtbzTmdlaF/edit).

Este Markdown es una **guía técnica derivada**, no una transcripción literal de la entrega académica. Ante discrepancias, prevalecen los requisitos y reglas de negocio oficialmente vigentes del proyecto. Revisar este archivo cuando cambien los requisitos o los límites de módulos.
