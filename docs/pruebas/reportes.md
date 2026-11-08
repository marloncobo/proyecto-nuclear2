# Reportes — consulta docente del MVP

Implementacion de RF-15 / CU-30, con historial independiente (RN-06) y
retroalimentacion disponible solo al terminar el intento (RN-08/09).
La descarga de reportes RF-16 / CU-31 sigue pospuesta segun
[el alcance del MVP](../alcance.md). La valoracion general docente RF-26 se implementa en `evaluation` al integrar
notificaciones; se consulta mediante su API de valoraciones y no se incluye
en la respuesta de reportes. La consolidacion de ausencias CU-35 sigue pendiente.
Esta consulta no declara completos RF-16 ni CU-35.

## Contrato HTTP

`GET /api/reportes/casos/{idCaso}/estudiantes/{idEstudiante}`

Requiere HTTP Basic con rol DOCENTE y propiedad del caso. La identidad del
docente proviene de la autenticacion. Los estudiantes, administradores y
docentes ajenos no acceden al seguimiento. Un caso inexistente devuelve 404;
un UUID invalido, 400. Los casos propios archivados conservan su historial.

Devuelve una lista ordenada por inicio e identificador de intento. Cada elemento
contiene `participacion` y `resultado`:

- `participacion`: `idIntento`, `idSesion`, `idEstudiante`, `estado`, `inicio`,
  `limite`, `preguntas` ordenadas y `respuestas` (mapa pregunta/opcion vigente).
- `resultado`: `calificacion`, `fecha` de evaluacion y `detalles` por pregunta
  (`idPregunta`, `idOpcion`, `contestada`, `correcta`, `puntos`, `retroalimentacion`).
  Es `null` cuando la evaluacion aun no esta almacenada, sin inventar una nota.

Solo se incluyen intentos FINALIZADO o VENCIDO; cada intento conserva su propia
entrada. Una lista vacia significa que no hay intentos terminados para ese
estudiante y caso. No representa ausencia ni calificacion de cero. Las preguntas
sin responder siguen en `preguntas` y, si existe evaluacion, en su detalle.
Consultar no calcula notas, modifica intentos ni envia notificaciones.

## Diseno y limites

`ControladorReportes` delega en `FachadaConsultaParticipaciones`, conforme a
[Facade en el diseno](../diseño/patrones-diseno.md#32-facade--coordinación-de-consultas-y-administración).
La fachada depende unicamente de `ConsultaParticipaciones` y `ConsultaResultados`,
contratos publicos de aplicacion con DTO inmutables. No accede a repositorios
ajenos ni requiere tablas o dependencias nuevas.

`ServicioConsultaParticipaciones` exige propiedad del caso antes de listar y
propiedad de la sesion antes de entregar un intento individual. Consume los
contratos pequenos `ConsultaCasosDocente` y `ConsultaSesionesDocente`; los
servicios propietarios implementan esas comprobaciones. El puerto de lectura
`RepositorioConsultaIntentos` separa el seguimiento de las escrituras y su
adaptador reutiliza `RepositorioIntentosJdbc`.

`ServicioConsultaResultados` comprueba autorizacion y finalizacion mediante
participation antes de leer la evaluacion persistida. El calculo sigue siendo
responsabilidad del flujo existente de evaluation. Las dependencias son
`reports -> evaluation -> participation -> cases/sessions`, sin ciclos.
El dominio de reports no necesita entidades para esta consulta.

Esta separacion aplica responsabilidad unica, segregacion de interfaces e
inversion de dependencias sin agregar estrategias o persistencia propias de
reportes. La consulta por identificador de caso es la operacion minima de esta
entrega; no agrega catalogos de navegacion ni exportaciones.

## Verificacion JUnit

Desde `backend/`: `./mvnw.cmd verify` en PowerShell o `sh mvnw verify` en Unix.

| Prueba | Requisito / comportamiento |
| --- | --- |
| `PruebaFachadaConsultaParticipaciones` | RF-15 / CU-30 / RN-06: coordina contratos, separa intentos, conserva evaluacion pendiente y propaga denegaciones. |
| `PruebaServicioConsultaParticipaciones` | RF-15 / RN-06/08: autoriza antes de consultar, conserva preguntas sin respuesta, rechaza intentos activos e inexistentes. |
| `PruebaServicioConsultaResultados` | RF-15 / RN-08/09/16: devuelve nota y explicaciones almacenadas sin calcularlas; bloquea accesos ajenos o anticipados. |
| `PruebaControladorReportes` | RF-15 / CU-30: autenticacion, roles, identidad del principal, JSON y errores 400/403/404. |
| `PruebaRepositorioIntentosJdbc` | RF-15 / RN-06: consulta real con H2, filtros de estudiante/caso/estado, orden y respuestas. |
| `PruebaServicioConsultaCasos` y `PruebaServicioGestionSesiones` | RF-15: rol, propiedad y existencia; seguimiento de casos archivados. |

Las pruebas usan dobles de puertos y H2; no requieren MySQL y no sustituyen una
verificacion de este recorrido en MySQL.

Validacion local del 8 de noviembre de 2026: `mvnw.cmd verify` termino con
`BUILD SUCCESS`, 205 pruebas reportadas, 0 fallos, 0 errores y 1 omitida
(el recorrido MySQL optativo existente). Se ejecuto con JDK 24 y compilacion
`release 21`, sin cambiar la configuracion del proyecto. `git diff --check`
tambien termino sin errores.

Validacion posterior del mismo dia: con MySQL local disponible se ejecuto
`mvnw.cmd -Dtest=PruebaFlujoMySql test`. El recorrido de integracion existente
termino con `BUILD SUCCESS`: 1 prueba ejecutada, 0 fallos, 0 errores y 0 omitidas.
La prueba utiliza un esquema temporal aislado y lo elimina al terminar.
Las credenciales se proporcionaron solo mediante variables del proceso.
Este recorrido cubre casos, convocatorias y estados; no constituye una prueba
del endpoint de reportes de extremo a extremo en MySQL.
