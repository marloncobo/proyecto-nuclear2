# Notificaciones: requisitos y desarrollo

Revision local: 08-11-2026. Base: d2c27c6. Rama: notificaciones.

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

El seguimiento docente incorpora avisos operativos para recordar sesiones
proximas, informar su cierre, senalar estudiantes pendientes y anunciar la
disponibilidad de evaluaciones y reportes.

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
Un fallo de Gmail conserva PENDIENTE, incrementa intentos, guarda solo ENVIO_FALLIDO
y reintenta a los 30, 60, 120 segundos, etc., con tope de espera de una hora.
No se almacenan mensajes de excepcion del proveedor ni se registran credenciales.

Las convocatorias y los retiros nuevos se preparan en HTML y texto plano.
Ambas versiones se cifran juntas con AES-256-GCM y un vector aleatorio por
mensaje. El enlace de convocatoria identifica la sesión sin incluir contraseñas
ni códigos. El contenido cifrado se elimina al confirmar o cancelar la entrega.
Los correos pendientes creados con el formato anterior conservan su texto plano.

Al crear una cuenta de docente o estudiante desde el formulario de alta, la misma
transacción encola un correo con el usuario (correo de inicio de sesión), la
contraseña inicial y el enlace de acceso. La contraseña se guarda como hash BCrypt
en la cuenta. El correo de alta fija como destinatario la dirección registrada
en ese momento. La cola cifra el destinatario y el contenido y los elimina al
confirmar la entrega.

El hash de convocatoria permanece
en `sessions`. Retirar cancela convocatorias pendientes; reincorporar rota el
codigo y cancela retiros pendientes obsoletos para no enviar mensajes contradictorios.
Los correos ya aceptados por el proveedor no pueden retirarse.

La API de Gmail no ofrece una clave de idempotencia para el envio. Si Gmail acepta
el correo y se pierde la respuesta o falla la confirmacion en la BD, un reintento
puede duplicarlo. Los eventos y avisos internos se deduplican en la BD.

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
- Un generador docente reconstruye avisos desde el estado durable. Crea recordatorios
  hasta 24 horas antes; al finalizar una sesion informa cierre, pendientes y reporte;
  anuncia evaluaciones cuando todos los intentos terminados tienen resultado. Los
  identificadores deterministas evitan duplicados incluso despues de reiniciar.
- Autenticacion y autorizacion de participacion quedan separadas: un codigo de
  convocatoria no retirada puede autenticar despues del cierre para consultar
  resultados/avisos; el ingreso nuevo conserva la validacion de sesion y horario.

## API

Todas las rutas usan el actor autenticado, sin aceptar id de destinatario.
Las mutaciones requieren JWT como las otras APIs del proyecto.

| Metodo / ruta | Actor / contenido |
| --- | --- |
| GET `/api/notificaciones?limite=20&desplazamiento=0` | Estudiante o docente; solo sus avisos internos entregados, ordenados del mas reciente al mas antiguo. Limite 1..100 y desplazamiento >=0. |
| PUT `/api/notificaciones/{id}/leida` | Destinatario estudiante o docente. Idempotente, 204; ajeno/inexistente 404. |
| PUT `/api/sesiones/{id}/convocados` | Docente responsable. JSON con listas `agregar` y `retirar` de UUID. Devuelve solo convocatorias afectadas y codigo nuevo solo para altas. |
| POST `/api/evaluaciones/{idIntento}/valoraciones` | Docente responsable. JSON `mensaje`, entre 1 y 4000 caracteres; 201. |
| GET `/api/evaluaciones/{idIntento}/valoraciones` | Estudiante propietario o docente responsable; historial. |

Ejemplo de cambio: `{"agregar":["UUID_ESTUDIANTE"],"retirar":[]}`.
Ejemplo de valoracion: `{"mensaje":"Revisa la perspectiva de los participantes"}`.
Los avisos internos retornan id, idSesion, idIntento, tipo, mensaje,
fechaCreacion y leida. No exponen el contenido cifrado de los correos ni codigos.

## Configuracion local

Se mantienen las variables MySQL y de identidad del README. Antes de registrar
cuentas con contraseña o programar convocatorias, configurar una clave AES
persistente de **32 bytes en Base64**:
`PSICOAPP_NOTIFICACIONES_CLAVE_CIFRADO`. No se incluye una clave de produccion ni
se genera otra automaticamente al reiniciar, pues perderia los pendientes.
Una clave ausente/invalida impide encolar correos; la transaccion productora se
revierte sin guardar el alta o la convocatoria. No cambiarla mientras existan pendientes:
para rotarla, entregar/cancelar los pendientes o migrarlos con ambas claves.
No registrar solicitudes, respuestas, tokens OAuth ni cuerpos de eventos de Gmail.

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
| PSICOAPP_NOTIFICACIONES_URL_ACCESO | URL HTTP(S) de ingreso usada en correos de alta y convocatoria. Por defecto, http://localhost:3000/ingreso. |
| PSICOAPP_NOTIFICACIONES_REMITENTE | Direccion de la cuenta Gmail que autorizo el envio. |
| PSICOAPP_GMAIL_ID_CLIENTE | ID del cliente OAuth de Google Cloud. |
| PSICOAPP_GMAIL_SECRETO_CLIENTE | Secreto de ese cliente OAuth. |
| PSICOAPP_GMAIL_TOKEN_RENOVACION | Token de renovacion de la cuenta remitente con permiso `gmail.send`. |
| PSICOAPP_NOTIFICACIONES_DESPACHO | Por defecto `false`. Usar `true` para procesar las entregas pendientes. |
| PSICOAPP_NOTIFICACIONES_INTERVALO_MS | Por defecto 30000. |
| PSICOAPP_NOTIFICACIONES_AVISOS_DOCENTE | Activa (`true`) o pausa (`false`) la generacion de nuevos avisos docentes. Pausarla no elimina el historial ni las solicitudes existentes. Por defecto `true`. |
| PSICOAPP_NOTIFICACIONES_RECORDATORIO_HORAS | Ventana previa al inicio de una sesion en la que se crea el recordatorio docente. Se expresa en horas, admite de 1 a 168 y usa 24 por defecto. |
| PSICOAPP_NOTIFICACIONES_AVISOS_DOCENTE_INTERVALO_MS | Frecuencia del barrido que revisa sesiones, intentos, evaluaciones y reportes para generar avisos. Se expresa en milisegundos y usa 60000 por defecto. |
| PSICOAPP_NOTIFICACIONES_AVISOS_DOCENTE_DEMORA_INICIAL_MS | Espera entre el arranque del backend y el primer barrido docente. Se expresa en milisegundos y usa 1000 por defecto. |

Los avisos docentes se revisan cada 60 segundos y el primer barrido ocurre un
segundo despues del arranque. Las variables opcionales
`PSICOAPP_NOTIFICACIONES_RECORDATORIO_HORAS`,
`PSICOAPP_NOTIFICACIONES_AVISOS_DOCENTE_INTERVALO_MS`,
`PSICOAPP_NOTIFICACIONES_AVISOS_DOCENTE_DEMORA_INICIAL_MS` y
`PSICOAPP_NOTIFICACIONES_AVISOS_DOCENTE` permiten ajustar esos valores o pausar
el generador sin borrar avisos existentes. Generar y entregar son operaciones
distintas: si `PSICOAPP_NOTIFICACIONES_DESPACHO=false`, las solicitudes nuevas
quedan en estado pendiente y todavia no aparecen como entregadas en el frontend.

### Autorizar la cuenta remitente

1. Habilitar la [API de Gmail](https://developers.google.com/workspace/gmail/api/guides) en Google Cloud y configurar la pantalla de consentimiento y un cliente OAuth de tipo aplicacion web.
2. Autorizar la cuenta remitente con el alcance `https://www.googleapis.com/auth/gmail.send` y acceso sin conexion (`access_type=offline`). Intercambiar el codigo de autorizacion por el token de renovacion siguiendo el [flujo OAuth de Google](https://developers.google.com/identity/protocols/oauth2/web-server). Como alternativa, usar [OAuth Playground](https://developers.google.com/oauthplayground/) con las credenciales OAuth propias y registrar su URI de redireccion `https://developers.google.com/oauthplayground` en ese cliente.
3. Guardar las credenciales en el gestor de secretos del backend o en el `.env` local. Configurar el remitente, mantener la clave AES persistente y habilitar el despacho con `PSICOAPP_NOTIFICACIONES_DESPACHO=true` cuando se quiera procesar la cola.

Google emite tokens de renovacion de siete dias para aplicaciones externas en modo de prueba con este alcance. Para operacion continua, configurar la audiencia y publicacion segun las politicas de la cuenta. [Vigencia de tokens OAuth](https://developers.google.com/identity/protocols/oauth2#expiration).

El backend obtiene el token de acceso automaticamente y lo reutiliza hasta poco
antes de vencer. Envía correo MIME por HTTPS con tiempos de conexión y lectura
de 5 segundos. Los nuevos mensajes usan `multipart/alternative` para ofrecer
HTML y texto plano. Si faltan credenciales o Google rechaza la
solicitud, la entrega permanece pendiente para reintentar.

Al arrancar, `schema.sql` agrega las tablas nuevas de forma idempotente. No modifica
ni elimina tablas existentes. La aplicacion debe conservar la misma clave y BD
entre reinicios. Los avisos internos se consultan por API. El frontend incluye
bandejas separadas para estudiantes y docentes, contador de no leidos,
actualizacion manual e historial.

## Plan y evidencia de pruebas

| Caso | RF/CU | Comprobacion automatizada |
| --- | --- | --- |
| CP-NO-001 | RF-20/21; CU-14 | Contenido del correo, codigo protegido, Unicode, recuperacion y limpieza del cifrado. |
| CP-NO-002 | RF-27; CU-33 | Nota valida, referencia de intento y un solo aviso al repetir evaluacion. |
| CP-NO-003 | RF-21; CU-34 | Altas/retiros, rotacion de codigo, cancelacion de mensajes obsoletos y cambios atomicos. |
| CP-NO-004 | RF-21/27 | Evento repetido y dos trabajadores concurrentes sin doble entrega confirmada. |
| CP-NO-005 | CU-14/33 | Gmail falla sin revertir convocatoria; error saneado y reintento con espera. |
| CP-NO-006 | RN-08/09; CU-24/27 | Evaluacion automatica voluntaria/por vencimiento, cero sin respuestas y sin aviso anticipado. |
| CP-NO-007 | RF-02/27 | Propietario, rol, JWT, lectura idempotente y consulta posterior al cierre. |
| CP-NO-008 | RF-21/27 | Repositorio reconstruido recupera pendientes; rollback de sesion, finalizacion y valoracion no deja avisos. |

Clases: `PruebaEventosNotificacion`, `PruebaFlujosNotificaciones`,
`PruebaCifradorNotificaciones` y `PruebaCorreoGmail`. La suite de flujos integra
Spring, JDBC, transacciones, seguridad y MockMvc sobre H2 en modo MySQL.
El puerto de correo se sustituye por un doble; el adaptador Gmail se verifica con
un servidor HTTP simulado. No se envian correos externos ni se usan datos reales.
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
La validacion manual con Gmail y MySQL real queda pendiente de disponer
de configuracion de pruebas. No se declara equivalencia entre H2 y MySQL.

## Limites conservados

No se implementan notificaciones push/WebSocket ni reintentos academicos
excepcionales. Los avisos se actualizan al ingresar, al abrir la bandeja o mediante
el planificador del backend. No se modifica el catalogo oficial RF/RN/CU ni se
inventa un catalogo RNF.

## Integracion de seguimiento docente — CP-NO-009

RF-27 y CU-32/33 requieren aviso por nueva valoracion; CU-30 es una consulta y
no dispara avisos. `reports` ahora incluye `valoracionesGenerales` por intento y
permite registrarlas con `POST /api/reportes/participaciones/{idIntento}/valoraciones`.
Usa `GestionValoraciones` de evaluation; la publicacion del evento y la outbox
permanecen en el flujo existente, evitando avisos duplicados. La nota mantiene
su aviso al finalizar o vencer el intento, no al leer el reporte.
Ver [contrato y pruebas de reportes](reportes.md#integracion-con-notificaciones--rf-2627-cu-3233).

Verificacion de integracion (08-11-2026): `mvnw.cmd -q verify`, salida 0 y JAR generado. 231 pruebas pasaron, 0 fallos y 0 errores; 17 recorridos MySQL omitidos por no disponer de las variables de pruebas. CP-NO-009 cubierto con Spring, JDBC, seguridad y outbox reales sobre H2. No se enviaron correos externos.
