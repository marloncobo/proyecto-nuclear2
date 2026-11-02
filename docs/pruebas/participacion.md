# Participacion — alcance y plan de implementacion

Este documento registra el alcance inicial de la rama `feat/participation`.
La funcionalidad se implementara en Java dentro de
`backend/src/main/java/com/psicoapp/participation`, respetando las capas
`api`, `application`, `domain` e `infrastructure`.

## Requisitos cubiertos

La primera entrega aborda:

- CU-17: iniciar participacion.
- CU-18 y CU-19: consultar preguntas, registrar la ultima respuesta y el
  progreso.
- CU-20: navegar segun `FLEXIBLE` o `RESTRINGIDA`.
- CU-21: reanudar el mismo intento.
- CU-22 y CU-23: reutilizar la autorizacion de convocatoria y la ventana de
  ingreso ya implementadas en `sessions`.
- CU-24 y CU-27: finalizar voluntariamente o por vencimiento, dejando el
  intento listo para evaluacion.
- CU-29: permitir reintento solo cuando exista autorizacion del docente y no
  haya otro intento en progreso.

Estas reglas se relacionan con RN-04, RN-06, RN-07, RN-12, RN-13, RN-14,
RN-15, RN-17, RN-18 y RN-19.

## Decisiones de diseño

- Un intento es independiente de la sesion y conserva su propio inicio, fin,
  estado y respuestas actuales.
- Una respuesta se identifica por intento y pregunta. Guardar una nueva
  seleccion reemplaza la seleccion anterior para esa pregunta; no se expone
  el historial interno de modificaciones.
- Las respuestas solo pueden modificarse mientras el intento este
  `EN_PROGRESO`. Al finalizar, el intento queda inmutable.
- La autorizacion concreta se delega al contrato
  `AutorizacionConvocatorias`; el modulo no duplica la consulta de
  convocatorias.
- El dominio no depende de Spring, JDBC ni HTTP. La coordinacion transaccional
  y los adaptadores JDBC permanecen en `application` e `infrastructure`.
- El calculo de la calificacion pertenece al modulo `evaluation`; participation
  solo conserva el intento y solicita su evaluacion al finalizar.

## Persistencia

Las tablas `intento`, `intento_pregunta` y `respuesta` conservan el orden de
preguntas y una sola respuesta vigente por pregunta e intento. El planificador
`PlanificadorCierreIntentos` revisa cada minuto los intentos vencidos y los
marca como `VENCIDO`; el intervalo y su habilitacion son configurables.

## Verificacion

Cada paso incorporara pruebas `Prueba<Clase>` en el paquete correspondiente.
La verificacion final de la rama sera:

```powershell
cd backend
.\mvnw.cmd verify
```

Los contratos HTTP y cualquier cambio de esquema se actualizaran en
`backend/README.md` cuando la API quede implementada.

## API inicial

La API protegida usa la identidad del `Principal` autenticado como estudiante:

- `POST /api/participaciones`: inicia un intento con `idSesion`,
  `codigoAcceso` y la lista ordenada de `preguntas`.
- `GET /api/participaciones/{id}`: consulta el progreso propio.
- `PUT /api/participaciones/{id}/respuestas`: guarda `idPregunta` e
  `idOpcion`, reemplazando la seleccion vigente.
- `POST /api/participaciones/{id}/avanzar`,
  `/retroceder`, `/reanudar` y `/finalizar`: ejecutan las transiciones
  permitidas por la modalidad y el estado.

La lista de preguntas se recibe actualmente como contrato de entrada para
mantener aislado el dominio; la integracion de lectura del caso publicado y
la validacion de que la lista no sea alterada por el cliente quedan pendientes
para el siguiente incremento. El servicio de dominio nunca recibe respuestas
correctas.

## Pruebas unitarias ejecutadas

Las pruebas unitarias del modulo se ejecutaron con Maven y release temporal
21, porque el entorno local no dispone de JDK 25:

```powershell
cd backend
.\mvnw.cmd '-Dmaven.compiler.release=21' '-Dtest=com.psicoapp.participation.domain.PruebaIntento,com.psicoapp.participation.application.PruebaServicioGestionParticipacion,com.psicoapp.participation.infrastructure.PruebaPlanificadorCierreIntentos' test
```

Resultado de la ejecucion del 2026-11-01:

- 14 pruebas ejecutadas.
- 0 fallos.
- 0 errores.
- 0 omitidas.
- `BUILD SUCCESS`.

Se cubren el ciclo de vida del intento, respuestas y navegacion, reglas del
servicio y cierre automatico de intentos vencidos.
