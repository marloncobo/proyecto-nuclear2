# Publicacion de casos — segunda entrega (Template Method)

## Alcance y trazabilidad

Implementa CU-09 y CU-10 para el modelo academico disponible: RF-09 y
RN-03/RN-05, con apoyo de RF-05/06/07/08/17 y RN-02/RN-11.
La rubrica permanece estructurada dentro del caso para el MVP, segun lo
confirmado en la revision de Builder; no se implementa carga de archivos.

El flujo es crear un borrador completo con Builder y solicitar su publicacion.
Los borradores incompletos siguen siendo validos como trabajo en preparacion,
pero no pueden publicarse. La edicion, el archivado y la programacion de sesiones
siguen pendientes. `EstadoCaso` reconoce ARCHIVADO para reconstruir ese valor
del esquema; esta entrega no proporciona una operacion para archivar.

## Patron y responsabilidades

`ControladorPublicacionCasos -> ServicioPublicacionCaso ->
PlantillaValidacionPublicacion <- ValidacionCasoAcademico`.

El metodo publico `final validar(Caso)` de la clase abstracta fija el orden:

1. `validarEstructura`.
2. `validarPreguntas`.
3. `validarRubrica`.

Los metodos protegidos se implementan en una sola subclase concreta para el
caso academico existente. No se inventan tipos adicionales para justificar
herencia. Las tres etapas agregan errores a un resultado inmutable con
`esValido()`, sin cambiar el caso ni consultar base de datos, Spring o HTTP.

Cada error contiene `codigo`, `ruta` y `mensaje`. Los indices de las rutas
corresponden a las colecciones normalizadas devueltas por GET, no necesariamente
al orden original del cuerpo de creacion.

## Reglas de publicacion

- Nombre, desarrollo situacional, contexto y tiempo estimado completos.
- Escenarios con titulo y contexto; orden consecutivo desde 1.
- Cada escenario contiene preguntas con enunciado y orden consecutivo desde 1.
- Cada pregunta tiene opciones con texto, marca de correccion y justificacion.
- Rubrica con nombre, escala positiva, criterios con nombre y preguntas
  asociadas; todas las preguntas deben tener cobertura en la rubrica.
- **Decision del MVP confirmada el 01-11-2026:** las ponderaciones son fracciones
  entre 0 y 1, y su suma exacta debe ser 1,00. Se calcula con BigDecimal,
  sin tolerancias de coma flotante. Un criterio puede agrupar varias preguntas;
  una pregunta puede contribuir a varios criterios, como admite el ER.

Las invariantes del Builder ya rechazan identificadores/posiciones duplicados,
valores negativos, decimales fuera de almacenamiento y referencias ajenas.
La comprobacion de cobertura de la rubrica es una validacion tecnica derivada
del vinculo CRITERIO_PREGUNTA para no publicar preguntas sin criterio de
evaluacion. No define aun el algoritmo de calificacion.

No se exige un minimo de dos opciones, una opcion correcta obligatoria,
una escena inicial, puntajes por opcion ni versionado: esas partes del DSL
siguen pendientes de reconciliacion/aprobacion y algunas no estan representadas
en el ER vigente. Esta entrega valida el agregado disponible; no declara
implementada la importacion JSON Schema/OCL ni toda la propuesta del DSL.

## Autorizacion, persistencia y concurrencia

Se reutiliza `DirectorioIdentidad.exigirCreacionCasos` para verificar usuario
activo, rol docente y permiso especifico. Luego se exige que sea el propietario
del caso. El identificador del actor procede de la autenticacion.

La validacion y el cambio de estado se ejecutan en la misma transaccion de
aplicacion. `RepositorioCasos.publicar` actualiza exclusivamente el estado
cuando coinciden identificador, propietario y estado BORRADOR. Si otra
solicitud ya publico, la actualizacion no afecta filas y se responde 409.
Una publicacion repetida tambien responde 409. No se vuelven a insertar hijos,
se conserva el contenido y se puede recuperar el estado PUBLICADO desde JDBC.

El contenido no puede editarse con las operaciones actuales. Si se implementa
edicion concurrente en otra entrega, debera incorporarse bloqueo o control
de version del agregado para no publicar una instantanea desactualizada;
la condicion de estado por si sola no protege futuras modificaciones de hijos.

## API

`POST /api/casos/{id}/publicar`, sin cuerpo.
Usa el mismo HTTP Basic, cookie y cabecera CSRF de identidad.
El docente primero consulta `GET /api/identidad/csrf` y conserva la cookie.

Respuesta 200:

```json
{"id":"identificador-del-caso","estado":"PUBLICADO"}
```

Respuesta 422 (ProblemDetail) si el contenido es incompleto:

```json
{
  "status": 422,
  "detail": "El caso no cumple las condiciones de publicacion",
  "errores": [
    {"codigo":"TEXTO_REQUERIDO","ruta":"nombre","mensaje":"Debe completar este contenido"},
    {"codigo":"RUBRICA_REQUERIDA","ruta":"rubrica","mensaje":"Debe asociar una rubrica"}
  ]
}
```

La respuesta real incluye todos los errores encontrados. No modifica el estado.

Otros codigos: 400 UUID invalido; 401 sin autenticar; 403 rol, permiso,
propiedad o CSRF; 404 caso inexistente; 409 estado distinto de BORRADOR
o publicacion concurrente. La consulta GET del caso sigue limitada a su
docente responsable, tambien despues de publicar; no se habilita acceso
estudiantil a justificaciones ni opciones correctas.

## Pruebas y revision

| Clase | Comportamiento comprobado |
| --- | --- |
| `PruebaValidacionCasoAcademico` | Orden del Template Method, acumulacion de errores, validacion completa, fracciones y cobertura |
| `PruebaServicioPublicacionCaso` | Permiso, propietario, existencia, estado previo, conservacion del borrador y conflicto de actualizacion |
| `PruebaControladorPublicacionCasos` | Rol/CSRF, publicacion HTTP, consulta posterior, errores 422 y rechazos 403/404/409 |
| `PruebaRepositorioCasosJdbc` | Publicacion condicional, contenido intacto, lectura posterior y rollback transaccional |

Ejecutar desde `backend/`: `.\mvnw.cmd verify`.
Resultado del 01-11-2026: verificacion completa con 94 pruebas, cero fallos
y cero errores (22 pruebas nuevas respecto al primer commit). Tras ajustar
las rutas de errores de cobertura, se repitieron las 25 pruebas de validacion,
servicio de publicacion, controlador y repositorio: todas pasaron; JAR regenerado.
`git diff --check` sin incidencias.
Las pruebas JDBC usan H2 en modo MySQL; no sustituyen una prueba contra
MySQL real. Se mantiene el ajuste local de agente Byte Buddy descrito en la
[primera entrega](casos-construccion.md#verificacion) para este entorno.

Mensaje propuesto, pendiente de revision antes del commit:
`feat(cases): validar y publicar casos con Template Method`.
