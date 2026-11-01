# Evaluacion — alcance y plan de implementacion

Este documento registra el alcance inicial de la rama `feat/evaluation`.
La funcionalidad se implementara en Java dentro de
`backend/src/main/java/com/psicoapp/evaluation`, respetando las capas
`api`, `application`, `domain` e `infrastructure`.

## Requisitos cubiertos

La primera entrega aborda:

- CU-25: calcular y almacenar la calificacion con la rubrica del caso.
- CU-26: consultar calificacion y retroalimentacion despues de finalizar.
- CU-27: evaluar un intento vencido una vez que `participation` lo marca como
  `VENCIDO`.
- CU-35: dejar documentado el punto de integracion para consolidar ausencias
  cuando se cierre una sesion.

Estas reglas se relacionan con RN-08, RN-09, RN-16, RN-17 y RN-19.

## Decisiones de diseño

- La calificacion se modelara mediante una estrategia de evaluacion para que
  el algoritmo no quede acoplado al servicio de casos o de participacion.
- Una pregunta sin respuesta obtiene cero.
- Solo la ultima respuesta vigente de cada pregunta participa en el calculo.
- Las justificaciones se entregan al consultar un intento finalizado; nunca
  durante la resolucion.
- Un intento en progreso no expone nota final ni retroalimentacion.
- El evaluador consumira contratos publicos de `cases` y `participation`, sin
  acceder a repositorios o detalles internos de esos modulos.
- La persistencia almacenara la nota final y el detalle evaluado por pregunta
  para conservar trazabilidad.
- El cierre automatico del intento lo ejecuta `PlanificadorCierreIntentos`;
  la solicitud de calculo de la nota sigue siendo
  `POST /api/evaluaciones/{idIntento}`.

## Dependencias entre ramas

`evaluation` depende del contrato estable que defina `participation` para
identificar el intento, sus respuestas y su estado. Por eso la implementacion
se realizara despues de establecer y probar esos contratos en
`feat/participation`.

## Verificacion

Cada paso incorporara pruebas `Prueba<Clase>` en el paquete correspondiente.
La verificacion final de la rama sera:

```powershell
cd backend
.\mvnw.cmd verify
```

Los contratos HTTP, el esquema y los ejemplos se actualizaran en
`backend/README.md` y en este documento cuando la API quede implementada.

## API inicial

La API protegida usa la identidad del `Principal` autenticado como estudiante:

- `POST /api/evaluaciones/{idIntento}` calcula y almacena el resultado de un
  intento finalizado. La operación es idempotente para el mismo intento.
- `GET /api/evaluaciones/{idIntento}` devuelve la calificacion y el detalle
  por pregunta, incluyendo retroalimentacion.

Un intento `EN_PROGRESO` no puede evaluarse y un estudiante no puede consultar
resultados de otro estudiante.
