# Sesiones — tercera entrega (State)

> Alcance historico: las [correcciones posteriores](correcciones-revision.md) actualizan las reglas y funciones descritas en esta entrega.

## Alcance

Implementa programacion base (parte de RF-04/RF-18, RN-03/RN-13),
ciclo de vida (RF-19/RN-12) y el contrato de estado para participacion
(apoyo de RF-25). No declara completos CU-12, CU-13 o CU-16: faltan
convocatorias, codigos, notificaciones y consolidacion de ausencias.
Tampoco implementa intentos, ventanas de ingreso tardio, reintentos,
calificacion ni navegacion estudiantil. Esas responsabilidades pertenecen
a participation, evaluation y notifications.

La seleccion de modo FLEXIBLE/RESTRINGIDA se guarda al programar, pero
no implementa por si sola RN-14. Cerrar una sesion no finaliza intentos
ni asigna notas automaticamente en esta entrega.

## State y reglas

`Sesion` es el contexto inmutable. Delega en la interfaz sellada
`EstadoSesion` y sus implementaciones `SinIniciar`, `EnCurso` y `Finalizado`.
Se persiste `TipoEstadoSesion` como identificador estable y se reconstruye
el objeto State al cargar. Las clases de dominio no dependen de Spring,
HTTP, SQL ni del programador temporal.

| Estado | Operacion | Resultado |
| --- | --- | --- |
| SIN_INICIAR | Inicio docente autorizado, antes del cierre automatico | EN_CURSO |
| SIN_INICIAR | Participacion o cierre manual | Rechazo |
| EN_CURSO | Segundo inicio | Rechazo |
| EN_CURSO | Cierre docente autorizado | FINALIZADO |
| SIN_INICIAR / EN_CURSO | Cierre automatico en fin previsto + 24 horas o despues | FINALIZADO |
| FINALIZADO | Inicio o participacion | Rechazo |
| FINALIZADO | Repetir cierre | Misma sesion, sin nueva escritura |

Se mantiene sin habilitar el cierre manual desde SIN_INICIAR, por ser una
decision funcional pendiente en la guia de patrones. No se impone una ventana
nueva al inicio docente: los requisitos seleccionados indican que el docente
lo inicia, pero no establecen un veto antes de la hora programada. Siempre se
rechaza iniciar o participar al alcanzar el limite de cierre automatico,
incluso si el barrido aun no ha actualizado el estado persistido.

La comprobacion de EN_CURSO no sustituye autorizacion de convocatoria,
ingreso tardio ni tiempo individual. No hay endpoint estudiantil nuevo.

## Integracion entre modulos

- `DirectorioIdentidad.exigirDocente`: exige cuenta activa y rol docente.
  Programar sesiones no exige el permiso de crear casos; ese permiso se
  comprueba al crear/publicar en cases.
- `CatalogoCasos.exigirPublicado`, implementado en cases: exige caso
  publicado del docente responsable y devuelve solo id y tiempo estimado.
  Rechaza borradores, archivados, casos ajenos e inexistentes. No expone
  opciones correctas ni justificaciones.
- `FachadaAdministracion.consultarEspacio`: reutiliza el contrato publico
  existente de academic para verificar espacio y acceso docente.
- `ConsultaSesiones.exigirEnCurso`: contrato interno para participation,
  devuelve una instantanea sin exponer la entidad. El consumidor debera
  usarlo dentro de la misma transaccion que registra el ingreso/respuesta,
  despues de comprobar identidad y convocatoria. Sin esa integracion, no se
  declara implementado el control completo de participacion concurrente.

La duracion maxima debe ser positiva y no superar el tiempo estimado del caso.
Las fechas son obligatorias, inicio anterior al fin; lugar y mensaje opcionales.
El actor siempre procede de la autenticacion. Solo el docente responsable
consulta, inicia o finaliza su sesion.

No se accede a repositorios ni entidades internas de otros modulos.
El esquema agrega SESION con las columnas y claves foraneas del ER; no agrega
CONVOCATORIA ni intenta simular sus autorizaciones.

## Tiempo, cierre automatico y concurrencia

Se usa Clock inyectable con reloj UTC en produccion y relojes fijos en pruebas.
La API recibe instantes ISO-8601 con Z u offset explicito y devuelve UTC.
09:00 -05:00 de Colombia corresponde a 14:00Z.

El cierre vence exactamente al alcanzar `finPrevisto + Duration.ofDays(1)`
(24 horas transcurridas). No depende de la zona horaria del equipo ni de
una medianoche local. Los campos TIMESTAMP del ER admiten precision de segundos
y el rango de MySQL 1970-01-01T00:00:01Z a 2038-01-19T03:14:07Z;
se rechazan nanosegundos y fechas fuera de ese rango para no truncar datos.

`PlanificadorCierreSesiones` solicita `cerrarVencidas(reloj.instant())`
cada 60 segundos, con primer barrido despues de ese intervalo. Revisa vencidas
acumuladas tras un reinicio. Puede haber hasta un intervalo de retraso en
actualizar el estado; el dominio igualmente rechaza acceso al vencer.
El docente sigue pudiendo cerrar manualmente una sesion en curso.

Propiedades opcionales:

- `psicoapp.sesiones.intervalo-cierre-ms`: 60000 por defecto.
- `psicoapp.sesiones.cierre-automatico-habilitado`: true por defecto;
  se usa false en la prueba de contexto sin base de datos.

Las transiciones y el contrato de participacion leen mediante SELECT FOR UPDATE
dentro de una transaccion. El cierre automatico consulta candidatos y vuelve
a comprobar estado y vencimiento bajo el mismo bloqueo; si otro proceso ya
cerro no repite la escritura. Los metodos JDBC de bloqueo/actualizacion exigen
una transaccion existente. Se conserva la programacion al actualizar el estado.

La programacion consume el caso publicado actual. Como cases aun no permite
editar ni archivar, su contenido es estable. Antes de incorporar esas
operaciones concurrentes debera coordinarse bloqueo/version entre cases y
programacion. No se atribuye a State la resolucion de esa futura integracion.

## API docente

Requiere HTTP Basic. POST requiere tambien cookie y cabecera CSRF obtenidas
mediante `GET /api/identidad/csrf`, igual que los modulos existentes.

| Metodo | Ruta | Resultado |
| --- | --- | --- |
| POST | /api/sesiones | 201, nueva sesion SIN_INICIAR |
| GET | /api/sesiones/{id} | 200, programacion y estado persistido |
| POST | /api/sesiones/{id}/iniciar | 200, EN_CURSO |
| POST | /api/sesiones/{id}/finalizar | 200, FINALIZADO; repeticion sin efectos |

Cuerpo de programacion:

```json
{
  "idCaso": "uuid-de-un-caso-publicado-propio",
  "idEspacio": "uuid-del-espacio-existente",
  "inicioPrevisto": "2026-11-03T09:00:00-05:00",
  "finPrevisto": "2026-11-03T10:00:00-05:00",
  "duracionMaximaMin": 30,
  "modoNavegacion": "FLEXIBLE",
  "lugar": "Aula 201",
  "mensaje": "Actividad educativa de psicologia social"
}
```

Los valores de id del ejemplo deben sustituirse por UUID reales del entorno.
El caso del ejemplo debe tener tiempo estimado de al menos 30 minutos.
Inicio y finalizacion no requieren cuerpo.

400: datos invalidos; 401: sin autenticar; 403: rol, propiedad o CSRF;
404: caso/espacio/sesion inexistente; 409: caso no programable o transicion
prohibida. No se aceptan del cliente el estado ni el docente responsable.

## Pruebas

| Clase | Comportamiento |
| --- | --- |
| PruebaSesion | Transiciones, inmutabilidad, idempotencia, limite exacto, vencimiento y programacion |
| PruebaServicioGestionSesiones | Contratos entre modulos, permisos, propietario, bloqueo y barrido |
| PruebaControladorSesiones | Rol, CSRF, actor autenticado, ISO con offset, rutas y errores |
| PruebaRepositorioSesionesJdbc | Persistencia, reconstruccion State, vencidas, rollback y bloqueo concurrente real con dos conexiones |
| PruebaPlanificadorCierreSesiones | Delegacion al caso de uso con reloj fijo |
| PruebaServicioConsultaCasos | Publicado propio permitido, borrador/archivado/ajeno rechazado |

Desde backend: `.\mvnw.cmd verify`. Las pruebas JDBC usan H2 en modo MySQL;
resultado del 01-11-2026: 120 pruebas, cero fallos, cero errores y cero
omitidas (26 pruebas nuevas respecto al segundo commit); JAR generado.
`git diff --check` sin incidencias.
la prueba de bloqueo emplea dos conexiones y transacciones concurrentes.
Falta validar contra MySQL real. Para el agente de Mockito en este entorno
se mantiene el ajuste local documentado en la primera entrega.

Mensaje propuesto, pendiente de revision:
`feat(sessions): gestionar ciclo de vida de sesiones con State`.
