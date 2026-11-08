# Notificaciones: requisitos y desarrollo

Revision local: 08-11-2026. Base: d2c27c6. Rama: codex/notificaciones.

## Fuentes revisadas

- [Contribucion](../../CONTRIBUTING.md).
- [Observer, seccion 3.6](../diseño/patrones-diseno.md).
- [RF](../requisitos/requisitos-funcionales.md), [RN](../requisitos/reglas-negocio.md) y [CU](../requisitos/casos-de-uso.md).
- [Alcance](../alcance.md), [ER](../diseño/ModeloER.drawio), [backend](../../backend/README.md).
- Plan de pruebas, correcciones de revision y documentacion de sesiones/evaluacion.

## Trazabilidad funcional

| Requisito / caso | Comportamiento requerido |
| --- | --- |
| RF-21; CU-12/13/14 | Correo inicial con informacion del caso, mensaje, URL y codigo. |
| RF-21; CU-34/14 | Notificar altas y retiros a cada afectado; actualizar autorizacion en sessions. |
| RF-20; RN-04 | El codigo corresponde a una convocatoria autorizada; no se obtiene del hash guardado. |
| RF-27; CU-24/27/33 | Avisar resultado de finalizacion voluntaria y por vencimiento. |
| RF-26/27; CU-32/33 | Aviso interno al guardar una nueva valoracion docente. |
| RN-08/09 | Resultado persistido y finalizado; no anticipar explicaciones. |
| RN-06 | Asociar cada aviso al intento correcto, conservando historial. |
| RF-02 | Solo el destinatario autenticado consulta sus avisos. |

## Implementacion

Observer implementado con `NotificacionSujeto` y `SpringNotificacionSujeto`.
Los eventos compartidos no exponen entidades internas. El receptor del framework
selecciona los observadores de correo y avisos internos registrados por Spring.
Estos preparan solicitudes; `RepositorioNotificacionesJdbc` las guarda con el
productor, sin enviar correo desde la transaccion academica.

`notificacion` funciona como outbox transaccional y almacen de avisos internos:
clave unica evento/usuario/canal, estado, intentos, proxima fecha, fecha de envio,
lectura y referencias a sesion e intento. No se necesita serializar un evento
completo: se persiste la solicitud de canal ya preparada por el observador.
Se usa MEDIUMTEXT para admitir la expansion Base64 del mensaje cifrado sin
recortar los 16000 caracteres Unicode permitidos en el mensaje de sesion.

El receptor es sincrono y transaccional para que la solicitud participe del
rollback productor. La entrega se realiza despues, al consultar filas confirmadas,
en transacciones independientes mediante el planificador. Esta es la variante
outbox descrita en la guia de patrones, no un envio en memoria despues del commit.

El despachador consulta hasta 100 entregas por barrido, por defecto cada 30 s.
Cada entrega usa `REQUIRES_NEW` y bloqueo de fila hasta confirmar, permitiendo
trabajadores concurrentes sin entrega duplicada por la misma solicitud confirmada.
Un fallo SMTP conserva PENDIENTE, incrementa intentos, guarda solo ENVIO_FALLIDO
y reintenta a los 30, 60, 120 segundos, etc., con tope de espera de una hora.
No se almacenan mensajes de excepcion del proveedor ni se registran credenciales.

El correo usa texto plano. El cuerpo pendiente se cifra con AES-256-GCM y vector
aleatorio por mensaje; incluye el codigo original requerido por RF-20/21.
Se elimina al confirmar o cancelar la entrega. El hash de convocatoria permanece
en `sessions`. Retirar cancela convocatorias pendientes; reincorporar rota el
codigo y cancela retiros pendientes obsoletos para no enviar mensajes contradictorios.
Los correos ya aceptados por el proveedor no pueden retirarse.

SMTP no permite garantizar exactamente un correo si el proceso cae despues de
que el proveedor lo acepte y antes de confirmar la transaccion local; ese caso
puede repetir el envio. Los eventos y avisos internos si se deduplican en la BD.

### Integraciones implementadas

- `sessions` emite convocatoria inicial y cambios. Exige docente responsable,
  sesion no finalizada/vencida, estudiantes existentes y cambios sin duplicados.
- `identity` expone su consulta existente por `DirectorioIdentidad`.
- `cases` expone nombre por contrato publico para el correo.
- `participation` emite `IntentoFinalizado`; el receptor de `evaluation` calcula
  dentro de la misma transaccion y publica `ResultadosPublicados` al guardar.
  Repetir la evaluacion bloquea el intento y reutiliza su resultado sin otro aviso.
- `evaluation` incorpora valoraciones generales persistentes por intento
  finalizado, autorizadas al docente responsable. Cada nueva valoracion emite aviso.
- `notifications` crea avisos internos de nota/valoracion. No publica justificaciones
  durante la resolucion. Cada intento conserva su propia referencia (RN-06).
- Autenticacion y autorizacion de participacion quedan separadas: un codigo de
  convocatoria no retirada puede autenticar despues del cierre para consultar
  resultados/avisos; el ingreso nuevo conserva la validacion de sesion y horario.

## API

Todas las rutas usan el actor autenticado, sin aceptar id de destinatario.
Las mutaciones requieren CSRF como las otras APIs del proyecto.

| Metodo / ruta | Actor / contenido |
| --- | --- |
| GET `/api/notificaciones?limite=20&desplazamiento=0` | Estudiante; solo sus avisos internos entregados, ordenados del mas reciente al mas antiguo. Limite 1..100 y desplazamiento >=0. |
| PUT `/api/notificaciones/{id}/leida` | Estudiante destinatario. Idempotente, 204; ajeno/inexistente 404. |
| PUT `/api/sesiones/{id}/convocados` | Docente responsable. JSON con listas `agregar` y `retirar` de UUID. Devuelve solo convocatorias afectadas y codigo nuevo solo para altas. |
| POST `/api/evaluaciones/{idIntento}/valoraciones` | Docente responsable. JSON `mensaje`, entre 1 y 4000 caracteres; 201. |
| GET `/api/evaluaciones/{idIntento}/valoraciones` | Estudiante propietario o docente responsable; historial. |

Ejemplo de cambio: `{"agregar":["UUID_ESTUDIANTE"],"retirar":[]}`.
Ejemplo de valoracion: `{"mensaje":"Revisa la perspectiva de los participantes"}`.
Los avisos internos retornan id, idSesion, idIntento, tipo, mensaje,
fechaCreacion y leida. No exponen el contenido cifrado de los correos ni codigos.

## Configuracion local

Se mantienen las variables MySQL y de identidad del README. Antes de programar
convocatorias, configurar una clave AES persistente de **32 bytes en Base64**:
`PSICOAPP_NOTIFICACIONES_CLAVE_CIFRADO`. No se incluye una clave de produccion ni
se genera otra automaticamente al reiniciar, pues perderia los pendientes.
Una clave ausente/invalida impide encolar correos; la transaccion productora se
revierte sin dejar convocatorias a medias. No cambiarla mientras existan pendientes:
para rotarla, entregar/cancelar los pendientes o migrarlos con ambas claves.
No habilitar logs de debug de SMTP ni registrar cuerpos de eventos.

Generacion en PowerShell (guardar el valor en el gestor de secretos del entorno):

```powershell
$claveNotificaciones = [byte[]]::new(32)
$generadorNotificaciones = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$generadorNotificaciones.GetBytes($claveNotificaciones)
$generadorNotificaciones.Dispose()
$env:PSICOAPP_NOTIFICACIONES_CLAVE_CIFRADO = [Convert]::ToBase64String($claveNotificaciones)
```

| Variable | Valor / funcion |
| --- | --- |
| PSICOAPP_NOTIFICACIONES_URL_ACCESO | URL HTTP(S) sin credenciales; por defecto http://localhost:3000/ingreso. Debe apuntar al acceso real del frontend cuando exista. |
| PSICOAPP_NOTIFICACIONES_REMITENTE | Remitente autorizado por el proveedor; obligatorio para entregar. |
| PSICOAPP_CORREO_HOST | Servidor SMTP; por defecto localhost. |
| PSICOAPP_CORREO_PUERTO | Puerto; por defecto 587. |
| PSICOAPP_CORREO_USUARIO / PSICOAPP_CORREO_CLAVE | Credenciales SMTP del entorno; nunca en Git. |
| PSICOAPP_CORREO_AUTENTICACION | Por defecto true. |
| PSICOAPP_CORREO_TLS | STARTTLS habilitado y requerido, por defecto true. |
| PSICOAPP_NOTIFICACIONES_DESPACHO | Por defecto true. false pausa entregas, conservando solicitudes. |
| PSICOAPP_NOTIFICACIONES_INTERVALO_MS | Por defecto 30000. |

Timeouts SMTP de conexion, lectura y escritura: 5 segundos. Las variables SMTP
pueden adaptarse al proveedor; para un capturador local sin autenticacion/TLS,
configurar explicitamente ambos valores en false y su puerto. Sin SMTP disponible,
la convocatoria se guarda y el envio queda pendiente para reintentar.

Al arrancar, `schema.sql` agrega las tablas nuevas de forma idempotente. No modifica
ni elimina tablas existentes. La aplicacion debe conservar la misma clave y BD
entre reinicios. Los avisos internos se consultan por API; no se implemento frontend.

## Plan y evidencia de pruebas

| Caso | RF/CU | Comprobacion automatizada |
| --- | --- | --- |
| CP-NO-001 | RF-20/21; CU-14 | Contenido del correo, codigo protegido, Unicode, recuperacion y limpieza del cifrado. |
| CP-NO-002 | RF-27; CU-33 | Nota valida, referencia de intento y un solo aviso al repetir evaluacion. |
| CP-NO-003 | RF-21; CU-34 | Altas/retiros, rotacion de codigo, cancelacion de mensajes obsoletos y cambios atomicos. |
| CP-NO-004 | RF-21/27 | Evento repetido y dos trabajadores concurrentes sin doble entrega confirmada. |
| CP-NO-005 | CU-14/33 | SMTP falla sin revertir convocatoria; error saneado y reintento con espera. |
| CP-NO-006 | RN-08/09; CU-24/27 | Evaluacion automatica voluntaria/por vencimiento, cero sin respuestas y sin aviso anticipado. |
| CP-NO-007 | RF-02/27 | Propietario, rol, CSRF, lectura idempotente y consulta posterior al cierre. |
| CP-NO-008 | RF-21/27 | Repositorio reconstruido recupera pendientes; rollback de sesion, finalizacion y valoracion no deja avisos. |

Clases: `PruebaEventosNotificacion`, `PruebaFlujosNotificaciones`,
`PruebaCifradorNotificaciones` y `PruebaCorreoSmtp`. La suite de flujos integra
Spring, JDBC, transacciones, seguridad y MockMvc sobre H2 en modo MySQL.
El puerto SMTP se sustituye por un doble; el adaptador SMTP se verifica con un
JavaMailSender simulado. No se envian correos externos ni se usan datos reales.
Los casos amplian el plan del proyecto sin afirmar aprobacion academica del equipo.

Verificacion requerida desde backend: `.\mvnw.cmd verify`.
La suite `PruebaFlujosNotificacionesMySql` reutiliza los mismos recorridos sobre
un esquema temporal aleatorio y lo elimina al finalizar. Se habilita al definir
`PSICOAPP_PRUEBAS_MYSQL_URL=jdbc:mysql://host:puerto/`,
`PSICOAPP_PRUEBAS_MYSQL_USUARIO` y `PSICOAPP_PRUEBAS_MYSQL_CLAVE`.
El usuario de pruebas necesita permiso para crear/eliminar ese esquema; no se
acepta la URL de una base existente. Sin esas variables, queda omitida.
Verificacion del 08-11-2026: `mvnw.cmd -q verify`, salida 0, JAR generado.
Total: 224 pruebas contabilizadas, 0 fallos, 0 errores y 14 omitidas.
Las 18 pruebas de notifications y las 4 de contratos de eventos pasaron.
Las 14 omitidas corresponden a los 13 recorridos MySQL de notificaciones y al
recorrido MySQL previo, porque no estan configuradas las variables de ese servidor.
`git diff --check` y los enlaces locales de la documentacion revisada sin incidencias.
La validacion manual con proveedor SMTP y MySQL real queda pendiente de disponer
de configuracion de pruebas. No se declara equivalencia entre H2 y MySQL.

## Limites conservados

No se implementan frontend, reportes, reintentos academicos excepcionales ni
consolidacion de ausencias sin intento (CU-35). Los eventos aqui corresponden
a intentos efectivamente evaluados. No se modifica el catalogo oficial RF/RN/CU
ni se inventa un catalogo RNF. La entrega atiende RF-21/RF-27 en backend, con
integraciones de CU-34 y CU-32 necesarias para dispararlos.
