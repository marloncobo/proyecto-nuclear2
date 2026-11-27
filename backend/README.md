# Arquitectura del backend

## Ejecución con Docker

La imagen usa una construcción multi-stage: Maven y el JDK 21 solo están presentes
durante la compilación; la imagen final contiene el JRE 21, el JAR ejecutable y un
usuario sin privilegios. Desde la raíz del repositorio:

```powershell
Copy-Item .env.example .env
docker compose up --build -d
docker compose ps
docker compose logs -f backend
```

Compose inicia MySQL, espera a que acepte conexiones y luego arranca el backend en
`http://localhost:8080`. El esquema se inicializa mediante `schema.sql`. Los datos
se conservan en el volumen `psicoapp_mysql` aunque se recree el contenedor.

Para reconstruir únicamente la aplicación:

```powershell
docker compose build backend
docker compose up -d backend
```

Para detener los servicios conservando la base use `docker compose down`. El comando
`docker compose down --volumes` elimina también el volumen y todos sus datos.

Los valores `PSICOAPP_DOCKER_*` de `.env.example` solo facilitan el desarrollo local.
No deben reutilizarse en producción. Configure claves Base64 persistentes para JWT y
notificaciones, credenciales distintas y HTTPS en el entorno de despliegue. El puerto
del host puede cambiarse mediante `PSICOAPP_BACKEND_PUERTO`.

La [referencia completa de endpoints](../docs/endpoints.md) documenta las 47
rutas implementadas, sus roles, entradas y resultados por módulo.

## Autenticación JWT

`POST /api/identidad/ingreso` recibe `{"correo":"usuario@example.test","clave":"contraseña"}`
y devuelve `token`, `tipo` (`Bearer`), `venceEn` (fecha UTC) y `cambioClaveInicial`. No requiere autenticación
ni CSRF; las credenciales incorrectas devuelven 401 y los campos inválidos 400.
La respuesta usa `Cache-Control: no-store`. Enviar luego `Authorization: Bearer <token>`.
El frontend ya utiliza este flujo y conserva el token únicamente en memoria.

Los tokens HS256 duran 900 segundos, validan firma, emisor, audiencia y caducidad.
Cada petición consulta la cuenta: desactivarla, eliminarla o cambiar su rol invalida
el token. Cambiar o recuperar la contraseña también invalida los JWT emitidos con
la contraseña anterior. Los JWT emitidos antes de incorporar esta verificación
requieren volver a iniciar sesión. Los estudiantes también usan una contraseña BCrypt. Si es inicial, el JWT
solo permite consultar la identidad y ejecutar `PUT /api/identidad/clave-inicial`.
El estudiante puede elegir una contraseña nueva o confirmar que conserva la inicial.
Al completar el flujo, el token temporal deja de ser válido. La autorización de la sesión concreta
se comprueba contra la convocatoria vigente de la cuenta autenticada.

Configurar `PSICOAPP_JWT_CLAVE` con al menos 32 bytes aleatorios codificados en Base64,
sin versionarla y usando una clave distinta de la de notificaciones. Alternativamente
usar `psicoapp.identidad.jwt.clave` mediante configuración externa. Sin clave se genera
una aleatoria por proceso: los tokens dejan de funcionar al reiniciar. Para despliegues
persistentes o con varias instancias, proporcionar la misma clave mediante un gestor de
secretos. El perfil Spring `prod` o `production` impide automáticamente el arranque sin
una clave persistente; `PSICOAPP_JWT_CLAVE_REQUERIDA=true` aplica la misma regla en
cualquier otro perfil. Cambiarla invalida todos los tokens.
`psicoapp.identidad.jwt.duracion-segundos`
admite entre 1 y 3600 segundos (predeterminado: 900). Usar HTTPS fuera del entorno local.

No hay renovación automática ni lista de revocación por token. Cerrar sesión elimina
el token del cliente; una copia sigue siendo válida hasta caducar o invalidar la cuenta,
el rol, la convocatoria o la clave de firma. La contraseña del personal se comprueba
al emitir el token, no en cada petición.

HTTP Basic está deshabilitado y la API no expone autenticación por cookies ni el endpoint
`/api/identidad/csrf`. Todas las rutas protegidas aceptan exclusivamente Bearer; por ello
CSRF está deshabilitado. La API no crea sesiones HTTP. Las dependencias `spring-security-oauth2-resource-server`
y `spring-security-oauth2-jose` proporcionan el filtro Bearer y la firma/validación Nimbus,
con versiones gestionadas por Spring Boot, siguiendo la
[documentación de Spring Security](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html).

Pruebas RF-02 / CP-ID-009: `PruebaAutenticacionJwt` verifica ingreso, Bearer, roles,
credenciales incorrectas, firma alterada, tokens malformados, desactivación, cambio de
rol y retiro de convocatoria. `PruebaServicioJwt` verifica caducidad, emisor, audiencia,
expiración obligatoria, claves inválidas y aislamiento de claves efímeras.

## Correcciones posteriores a la revision tecnica

La [guia vigente de correcciones](../docs/pruebas/correcciones-revision.md)
actualiza los limites historicos de las entregas: agrega `PUT /api/casos/{id}`
para sustituir borradores, archivado, bloqueo de casos compartido con programacion,
opciones correctas e incorrectas obligatorias al publicar, estudiantes y
convocatorias iniciales con codigos, autenticacion estudiantil por convocatoria,
y prohibicion de inicio anticipado. Contiene los contratos de integracion,
el alcance pendiente y las instrucciones para ejecutar la prueba MySQL optativa.

## Construccion de casos — Builder

`cases` permite crear y consultar borradores con escenarios, preguntas,
opciones y rubrica. Reutiliza `DirectorioIdentidad` para exigir el permiso
`CREAR_CASOS`; solo su docente responsable puede consultar el borrador.
La persistencia JDBC agrega las siete tablas del contenido academico del ER.
Las rutas incluyen `GET /api/casos` para listar casos del docente,
`POST /api/casos` y `GET /api/casos/{id}`, con JWT Bearer.
`POST /api/casos/{id}/publicar` valida estructura, preguntas y rubrica con
Template Method antes de cambiar BORRADOR a PUBLICADO. Devuelve 422 con errores
acumulados si el contenido es invalido; exige permiso de creacion y propiedad.
Las ponderaciones de la rubrica son fracciones que suman 1,00, segun la decision
del MVP. PUT permite editar borradores; POST /api/casos/{id}/archivar conserva
el contenido y evita nuevas programaciones. Cada pregunta publicada exige
al menos una opcion correcta y una incorrecta.
Consulta el [alcance, ejemplo y pruebas de construccion](../docs/pruebas/casos-construccion.md).
Consulta tambien las [reglas y pruebas de publicacion](../docs/pruebas/casos-publicacion.md).

## Sesiones — State

`sessions` programa casos publicados propios en espacios academicos existentes,
con duracion no mayor al tiempo estimado. El docente responsable inicia/finaliza
la sesion mediante los estados `SinIniciar`, `EnCurso` y `Finalizado`.
Un planificador abre las sesiones al llegar al inicio previsto. El docente
responsable puede adelantar la apertura. Otro planificador cierra las sesiones
al alcanzar el fin previsto,
incluidas sesiones nunca iniciadas. Las transiciones usan bloqueo transaccional.

Rutas docentes: `GET /api/sesiones`, `POST /api/sesiones`, `GET /api/sesiones/{id}`,
`POST /api/sesiones/{id}/iniciar` y `POST /api/sesiones/{id}/finalizar`.
Usan JWT Bearer. Programar exige una lista no vacia de UUID
`estudiantes` institucionales existentes. La autorización se asocia directamente a cada cuenta.
La programacion registra correos pendientes en su misma transaccion.
`PUT /api/sesiones/{id}/convocados` recibe listas `agregar` y `retirar`,
actualiza autorizaciones y registra avisos por correo.
El reporte de una sesión cerrada incluye como ausentes con nota cero a los
estudiantes convocados que no iniciaron ningún intento.
Consulta [reglas, tiempo, integraciones y pruebas](../docs/pruebas/sesiones.md).

## Participacion — intentos

`participation` inicia intentos para estudiantes convocados, conserva la
ultima respuesta por pregunta y aplica la modalidad `FLEXIBLE` o `RESTRINGIDA`.
Las rutas son `POST /api/participaciones`, `GET /api/participaciones/{id}`,
`PUT /api/participaciones/{id}/respuestas` y las transiciones
`/avanzar`, `/retroceder`, `/reanudar` y `/finalizar`. La autorizacion de la
convocatoria y la ventana de ingreso se delegan a `sessions`. Un planificador
marca automaticamente como vencidos los intentos cuyo limite individual se
agota. La finalizacion voluntaria y por vencimiento solicita evaluacion y
registro del aviso de resultado en la misma transaccion mediante eventos.
`GET /api/estudiante/actividades/{idSesion}` recupera la actividad y el intento existente
sin crear uno. `POST /api/estudiante/actividades/{idSesion}/iniciar` es la acción explícita
que crea el intento y comienza el tiempo. Las preguntas activas no incluyen claves de
corrección. `GET /api/estudiante/resultados` expone soluciones, explicaciones y valoraciones
únicamente para intentos finalizados.

## Evaluacion — calificacion

`evaluation` calcula la nota de un intento finalizado con una estrategia de
ponderacion, asigna cero a preguntas sin respuesta y conserva la
retroalimentacion por pregunta. Las rutas son `POST /api/evaluaciones/{idIntento}`
y `GET /api/evaluaciones/{idIntento}`. El estudiante autenticado solo puede
consultar su propio intento. El POST permanece idempotente; la finalizacion de
participacion ahora evalua automaticamente. `POST /api/evaluaciones/{idIntento}/valoraciones`
permite al docente responsable registrar una valoracion de hasta 4000 caracteres,
y `GET` consulta el historial para ese docente o el estudiante propietario.

## Notificaciones — Observer

`notifications` observa convocatoria inicial, altas/retiros, resultados y
valoraciones docentes. Persiste las solicitudes antes del commit y las entrega
por separado, con deduplicacion, bloqueo por entrega y reintentos exponenciales.
Los correos se guardan cifrados con AES-256-GCM; el contenido se borra al entregarlo
o cancelarlo. Los avisos de resultado/valoracion se conservan para su destinatario.
Un error del canal no revierte la operacion academica.

Rutas para estudiantes y docentes: `GET /api/notificaciones?limite=20&desplazamiento=0`
y `PUT /api/notificaciones/{id}/leida`. No se acepta un destinatario
ajeno en el cuerpo. La autenticacion identifica la cuenta; iniciar o continuar
participacion conserva sus
controles independientes de convocatoria, estado y ventana temporal.

Se agregan las tablas `notificacion` y `valoracion_docente` al esquema.
El adaptador de infraestructura implementa el puerto de correo mediante la API HTTPS de Gmail.
Al dar de alta a un docente o estudiante, se encola un correo con la contraseña
inicial y el usuario en la dirección registrada durante el alta.
Los correos de convocatoria y retiro tienen una versión HTML y una alternativa de texto plano.
La convocatoria muestra la apertura y el cierre en hora de Colombia, el tiempo para resolver,
el lugar y el mensaje del docente cuando se proporcionan, y el botón de acceso a la actividad.
Antes de registrar usuarios o convocar estudiantes, configura la clave AES
persistente, la URL de acceso y Gmail. [Guía de notificaciones](../docs/pruebas/notificaciones.md).
El correo identifica la sesión mediante el enlace de ingreso. El estudiante se autentica con su cuenta institucional.
Los avisos docentes se reconstruyen de forma idempotente desde sesiones, intentos
y evaluaciones persistidos: recordatorio próximo, finalización, estudiantes que no
completaron, evaluaciones disponibles y reporte disponible.

## Reportes — seguimiento docente

`reports` implementa la consulta minima de RF-15 / CU-30 mediante Facade:
`GET /api/reportes/casos/{idCaso}/estudiantes/{idEstudiante}`. Requiere JWT Bearer
y rol DOCENTE; solo el responsable del caso puede consultar el historial.
Devuelve cada intento finalizado o vencido con sus respuestas y la evaluacion
almacenada, si existe (`resultado: null` indica evaluacion pendiente).
Consume contratos publicos de `participation` y `evaluation`, sin consultar
repositorios ajenos ni calcular notas. `GET /api/reportes/sesiones/{id}`
consulta el estado de participación y las notas por sesión, identifica a cada
estudiante por nombre y correo y alimenta la exportación Excel. Al cerrar manual o
automaticamente una sesion, registra por separado las ausencias con nota cero y
cero puntos por pregunta; no crea intentos ficticios (CU-35).
La respuesta incluye `valoracionesGenerales` por intento. El docente responsable
puede agregar una con `POST /api/reportes/participaciones/{idIntento}/valoraciones`,
JSON `mensaje`. Se reutiliza `GestionValoraciones` de evaluation: guarda
la valoracion y solicita el aviso interno al estudiante una sola vez (RF-27,
CU-32/33). Consultar el reporte no genera notificaciones.
Consulta el [contrato, diseno y pruebas de reportes](../docs/pruebas/reportes.md).

Un único proyecto Maven y una aplicación Spring Boot, organizados por módulos
funcionales. `PsicoappApplication` está en el paquete raíz para que Spring
descubra los componentes de todos los módulos.

```text
src/main/java/com/psicoapp/
├── PsicoappApplication.java
├── identity/
├── academic/
├── cases/
├── sessions/
├── participation/
├── evaluation/
├── notifications/
├── reports/
└── shared/
    ├── configuration/
    ├── security/
    ├── exceptions/
    └── events/
```

Cada uno de los ocho módulos funcionales contiene estas cuatro capas:

```text
<modulo>/
├── api/
├── application/
├── domain/
└── infrastructure/
```

Los archivos `package-info.java` documentan las responsabilidades y permiten
versionar los paquetes incluso antes de añadir sus primeras clases.

## Responsabilidades

| Módulo | Responsabilidad prevista |
| --- | --- |
| `identity` | Usuarios, roles e identidad |
| `academic` | Organización académica |
| `cases` | Gestión de casos |
| `sessions` | Sesiones de atención |
| `participation` | Participación y asistencia |
| `evaluation` | Evaluaciones e instrumentos |
| `notifications` | Envío de notificaciones |
| `reports` | Generación y consulta de reportes |

`identity` y `academic` implementan la primera base funcional descrita abajo.
`cases` implementa la construccion de borradores y su validacion/publicacion,
descritas al inicio de esta guia.
`sessions` implementa programacion base y ciclo de vida descritos arriba.
Los demas modulos conservan unicamente su estructura inicial.

## Capas y dependencias

- `api`: controladores HTTP, DTO y validación de solicitudes. Delega en
  `application`; no accede directamente a persistencia.
- `application`: casos de uso y coordinación de operaciones. Depende de
  `domain` y define los puertos de salida necesarios para sus casos de uso.
- `domain`: entidades, objetos de valor y reglas de negocio. No depende de
  Spring, HTTP, persistencia ni de las otras capas del módulo.
- `infrastructure`: adaptadores de persistencia e integraciones. Implementa
  los puertos y depende de `application` y `domain`.

El flujo habitual es `api -> application -> domain`. Los adaptadores de
`infrastructure` se conectan a través de interfaces, mediante inyección de
dependencias. Las interfaces que expresan conceptos propios del dominio pueden
pertenecer a `domain`.

Entre módulos, consumir contratos públicos de `application` o eventos
compartidos; evitar acceder a entidades internas y adaptadores de otro módulo.
Mantener las dependencias entre módulos sin ciclos.
Estas son convenciones de desarrollo; todavía no hay validación automática de
las dependencias entre capas.

## Código transversal

- `shared.configuration`: configuración común y ensamblado de componentes.
- `shared.security`: mecanismos de seguridad comunes. Los usuarios y roles
  pertenecen a `identity`.
- `shared.exceptions`: contratos de error y traducción común a respuestas HTTP.
- `shared.events`: contratos de eventos usados por varios módulos.

`shared` no debe depender de los módulos funcionales. El código específico de
un único módulo debe permanecer en ese módulo.

## Identidad y organización académica — MVP

Se implementan usuarios, los tres roles, asignación de roles por administrador,
permiso individual `CREAR_CASOS` para cuentas docentes, registro de estudiantes institucionales
y creación/consulta de espacios académicos con nombre y periodo. La persistencia
usa MySQL y JDBC; las contraseñas se almacenan con BCrypt.

La base de identidad y espacios en `src/main/resources/schema.sql` reproduce `USUARIO`,
`ROL`, `PERMISO`, `ROL_PERMISO` y `ESPACIO_ACADEMICO` del
[modelo ER](../docs/diseño/ModeloER.drawio). Los UUID se representan mediante
`CHAR(36)`. La tabla `permiso_creacion_casos` guarda una concesión por cuenta.
Solo los docentes pueden recibirla y se retira al salir del rol docente.
Al inicializar el esquema, si existía una concesión general, se copia una vez
a los docentes existentes y se retira del rol. Los docentes nuevos empiezan
sin permiso. Si la inicialización SQL está desactivada, aplica el
[esquema actualizado](src/main/resources/schema.sql) antes de levantar el backend.

La fachada acotada `FachadaAdministracion` coordina autorización mediante
el contrato público `DirectorioIdentidad` y operaciones de espacios académicos.
Los repositorios son puertos implementados en `infrastructure`; los dominios
son Java independiente de Spring y JDBC. No se introducen Proxy, Builder ni
otros patrones sin un caso de uso que los necesite.

### Nombres y compatibilidad

Los identificadores propios se escriben en español según `CONTRIBUTING.md`.
La fachada se llama `FachadaAdministracion`, el servicio de identidad
`ServicioUsuarios` y los puertos `DirectorioIdentidad`, `RepositorioUsuarios`
y `RepositorioEspaciosAcademicos`. Las pruebas se llaman `Prueba<Clase>`.
Se mantienen los paquetes existentes y las firmas requeridas por las bibliotecas.

Esta refactorización cambia los contratos públicos anteriores: las rutas usan
`/api/identidad` y `/api/academico/espacios`, los campos JSON usan nombres como
`correo`, `nombre`, `clave`, `rol` y `periodo`, y las variables propias de entorno
son las indicadas abajo. Los clientes deben actualizar rutas, campos y variables;
no se mantienen alias en inglés. El esquema MySQL y el alcance funcional no cambian.

### Configuración local

Requiere MySQL 8.0 o superior y una base de datos creada previamente:

```sql
CREATE DATABASE psicoapp CHARACTER SET utf8mb4;
```

La configuración de ejecución se centraliza en `.env` en la raíz del repositorio.
Se carga automáticamente al ejecutar Maven desde `backend/` o desde la raíz.
Créalo una sola vez desde la plantilla y no lo versiones:

```powershell
Copy-Item .env.example .env
```

Completa la conexión a MySQL y las claves antes de iniciar el backend. Las variables
definidas en el sistema conservan prioridad sobre `.env`, así que producción puede
inyectarlas sin montar ese archivo. La plantilla documenta los valores de desarrollo
para correo, notificaciones, JWT e intervalos programados.

Configura estas variables en `.env` (sin versionar secretos):

| Variable | Uso / valor predeterminado |
| --- | --- |
| `PSICOAPP_BD_URL` | `jdbc:mysql://localhost:3306/psicoapp` |
| `PSICOAPP_BD_USUARIO` | Usuario MySQL; predeterminado `psicoapp` |
| `PSICOAPP_BD_CLAVE` | Contraseña MySQL; sin valor predeterminado |
| `PSICOAPP_CORREO_ADMINISTRADOR` | Correo del administrador inicial |
| `PSICOAPP_CLAVE_ADMINISTRADOR` | Clave inicial de al menos 8 caracteres y hasta 72 bytes |
| `PSICOAPP_DOMINIOS_INSTITUCIONALES` | Dominios institucionales exactos separados por comas |
| `PSICOAPP_JWT_CLAVE` | Clave HS256 Base64 de 32 bytes o más; vacía genera una clave efímera |
| `PSICOAPP_CORS_ORIGENES` | Orígenes web exactos permitidos, separados por comas; vacío deshabilita CORS |
| `PSICOAPP_NOTIFICACIONES_CLAVE_CIFRADO` | Clave AES-256-GCM Base64 para cifrar notificaciones |
| `PSICOAPP_NOTIFICACIONES_REMITENTE` | Dirección de la cuenta Gmail autorizada |
| `PSICOAPP_GMAIL_ID_CLIENTE`, `PSICOAPP_GMAIL_SECRETO_CLIENTE`, `PSICOAPP_GMAIL_TOKEN_RENOVACION` | Credenciales OAuth 2.0 de la cuenta remitente |
| `PSICOAPP_NOTIFICACIONES_DESPACHO` | `true` habilita la entrega; `false` conserva los correos pendientes |
| `PSICOAPP_NOTIFICACIONES_AVISOS_DOCENTE` | Habilita o pausa la generación de nuevos avisos docentes; predeterminado `true` |
| `PSICOAPP_NOTIFICACIONES_RECORDATORIO_HORAS` | Anticipación del recordatorio de sesión, en horas; rango 1–168, predeterminado `24` |
| `PSICOAPP_NOTIFICACIONES_AVISOS_DOCENTE_INTERVALO_MS` | Intervalo entre revisiones de avisos docentes, en milisegundos; predeterminado `60000` |
| `PSICOAPP_NOTIFICACIONES_AVISOS_DOCENTE_DEMORA_INICIAL_MS` | Demora del primer barrido después del arranque, en milisegundos; predeterminado `1000` |

El arranque crea las tablas de identidad, espacios, casos y sesiones si no existen e inserta los roles y el permiso
sin duplicarlos. El arranque crea la cuenta configurada si no hay ningún
administrador activo y mantiene los datos de las cuentas existentes. Sin variables de administrador no
se crea una cuenta predeterminada. Sin dominios configurados no se admiten
nuevos estudiantes. El usuario MySQL debe poder crear las tablas y consultar y
modificar sus registros. El esquema no migra tablas preexistentes incompatibles.

### Actualizar una base existente

Antes de arrancar esta versión con una base creada sin `intento.fecha_fin`, aplica
una vez [001-fecha-fin-intento.sql](migraciones/001-fecha-fin-intento.sql) en esa base.
Aplica después [002-autorizacion-reintento.sql](migraciones/002-autorizacion-reintento.sql)
si la base aún no tiene la tabla `autorizacion_reintento` y
[003-clave-inicial-estudiante.sql](migraciones/003-clave-inicial-estudiante.sql)
para incorporar el indicador de cambio obligatorio. Las cuentas estudiantiles antiguas
sin contraseña se conservan y un docente o administrador puede asignarles una inicial
mediante la API, sin duplicarlas. Las instalaciones nuevas ya incluyen los cambios en
`schema.sql`. Los intentos
cerrados antes del cambio conservan su fecha de fin como desconocida.

Aplica una vez [004-fecha-creacion-sesion.sql](migraciones/004-fecha-creacion-sesion.sql)
antes de iniciar esta versión con una base existente. Las sesiones creadas a partir
de esta actualización se listan por fecha de creación descendente. Las sesiones
anteriores aparecen después, ordenadas por inicio programado descendente.

Si el despliegue administra el esquema de forma manual, ejecuta
[005-recuperacion-clave.sql](migraciones/005-recuperacion-clave.sql) antes de
habilitar la recuperación. Con la inicialización SQL activa, el esquema crea
la tabla automáticamente.

## API mínima

La ampliacion de gestion del 18-11-2026 agrega las consultas y ediciones de la
tabla siguiente. No requiere migraciones: utiliza las tablas y columnas existentes.

`POST /api/identidad/ingreso` utiliza el correo y la contraseña para emitir JWT.
El identificador del actor siempre procede de la autenticación del backend.
Para usarla fuera de localhost, servir la API mediante HTTPS.

| Método y ruta | Actor / resultado |
| --- | --- |
| `GET /api/identidad/yo` | Usuario autenticado: identidad y permiso, sin hashes |
| `POST /api/identidad/docentes` | Administrador: `{ "correo": "docente@universidad.edu.co", "nombre": "Docente", "clave": "clave-ficticia" }` |
| `PUT /api/identidad/usuarios/{idUsuario}/rol` | Administrador: `{ "rol": "DOCENTE" }`; también `ADMINISTRADOR` o `ESTUDIANTE` |
| `PUT /api/identidad/usuarios/{idUsuario}/creacion-casos` | Administrador: `{ "habilitado": true }` concede y `false` retira para ese docente. Devuelve el perfil actualizado |
| `POST /api/identidad/estudiantes` | Docente: `{ "correo": "estudiante@universidad.edu.co", "nombre": "Estudiante" }`; reutiliza cuentas estudiantiles existentes |
| `POST /api/academico/espacios` | Docente: `{ "nombre": "Psicología social", "periodo": "2026-2" }` |
| `GET /api/academico/espacios` | Docente: espacios disponibles |
| `GET /api/academico/espacios/{id}` | Docente: espacio por UUID |
| `GET /api/identidad/usuarios?rol=DOCENTE&activo=true&busqueda=ana` | Administrador: arreglo de usuarios; filtros opcionales combinables; búsqueda parcial por nombre o correo; orden por nombre, correo y UUID |
| `GET /api/identidad/usuarios/{idUsuario}` | Administrador: detalle, incluyendo `activo`, sin credenciales |
| `PUT /api/identidad/docentes/{idUsuario}` | Administrador: `{ "correo": "docente@universidad.edu.co", "nombre": "Docente actualizado" }`; exige rol DOCENTE |
| `PUT /api/identidad/usuarios/{idUsuario}/estado` | Administrador: `{ "activo": false }` desactiva; `true` reactiva |
| `GET /api/identidad/roles` | Administrador: arreglo de los tres nombres de rol |
| `GET /api/identidad/estudiantes?correo=alumno@universidad.edu.co` | Docente: un estudiante activo por correo institucional exacto, normalizado; 404 si no existe |
| `PUT /api/academico/espacios/{id}` | Docente: `{ "nombre": "Psicologia social", "periodo": "2027-1" }`; 409 si alguna sesion usa el espacio |

Las consultas y ediciones nuevas responden 200. Las representaciones administrativas
incluyen `id`, `correo`, `nombre`, `rol`, `puedeCrearCasos` y `activo`; el contrato
de `/yo` permanece vigente. Editar docentes conserva UUID, rol, clave y fecha de
creacion; correo duplicado o cuenta sin rol DOCENTE produce 409. No existe
restablecimiento de claves ni eliminacion fisica. Desactivar conserva el historial
y deniega la siguiente autenticacion; reactivar conserva sus credenciales.
El administrador no puede desactivar su propia cuenta (409).

Los espacios siguen siendo compartidos, sin propietario ni matriculas. Su edicion
se bloquea incluso si la sesion asociada esta finalizada. Programacion y edicion
bloquean la misma fila durante su transaccion; el adaptador de `sessions`
implementa el puerto `ConsultaUsoEspacios` de `academic`, sin dependencia circular.
La busqueda estudiantil no permite listar todo el directorio a los docentes.

Después del ingreso, todas las peticiones protegidas envían `Authorization: Bearer <token>`.
No se usan cookies de autenticación ni tokens CSRF. La API responde 400 por datos
inválidos, 401 por token ausente o inválido, 403 por permisos, 404 por registros
inexistentes y 409 por conflictos.

`ServicioUsuarios.exigirCreacionCasos` expone la comprobación del permiso
específico para futuros casos de uso de creación de casos. El rol docente solo
no basta. Una cuenta estudiante sin contraseña no puede promoverse a personal;
el administrador crea las cuentas docentes con su contraseña.

### Límite de integración y trazabilidad

Esta seccion describe la entrega inicial. Para el estado vigente de autenticacion
por convocatoria y programacion, consultar las secciones de sesiones y las
[correcciones posteriores](../docs/pruebas/correcciones-revision.md).

- RF-02, RF-03, RN-10 y CU-02/CU-03: administración de docentes, roles y permisos.
- RF-01: validación del correo institucional y autenticación estudiantil mediante
  el puerto `VerificadorAccesoEstudiante`. El futuro módulo propietario de las
  convocatorias debe implementar ese puerto verificando correo/usuario y código.
  **Sin adaptador de convocatorias, todo acceso estudiantil se deniega**; esta
  entrega no declara completo RF-01 ni RN-04/RF-20.
- RF-04: catálogo mínimo de espacios y registro/reutilización de estudiantes.
  La asociación espacio-sesión y las convocatorias requieren `SESION` y
  `CONVOCATORIA` del ER, fuera de estos dos módulos. No se declara completo RF-04.
- Programación, ventanas de ingreso, intentos, notificaciones, resultados y
  permisos de participación concreta quedan en sus módulos propietarios.

### Pruebas

El [plan de pruebas por casos de uso](../docs/pruebas/plan-pruebas.md) reúne las
fichas funcionales de identidad y organización académica, su trazabilidad y
los escenarios pendientes de integración. Las fichas están planeadas; sus
veredictos se completan al ejecutarlas.

Desde `backend/`, ejecutar `./mvnw.cmd verify` (PowerShell) o `sh mvnw verify`.
Las pruebas JUnit cubren reglas de usuarios y espacios, coordinación de la
fachada, credenciales y controles HTTP de JWT/rol. Los nombres de las pruebas
enlazan RF/RN y el ER. Usan dobles de los puertos y no requieren MySQL.
La prueba de contexto desactiva solo la inicialización SQL y el alta inicial
del administrador; no verifica conectividad ni persistencia.

`PruebaFlujosIdentidadAcademica` complementa las pruebas unitarias y de seguridad
con los recorridos HTTP disponibles de CP-ID-001 a CP-ID-014 y CP-AC-001 a
CP-AC-007. Usa controladores, autenticación, validación y servicios reales;
solo sustituye los puertos de almacenamiento con dobles en memoria reiniciados
antes de cada ejecución. Las modificaciones obtienen un JWT del endpoint real de
ingreso. Comprueba también el estado posterior a los rechazos, la autorización
después de cambiar un rol y el aislamiento del permiso entre docentes.
Esta clase no verifica JDBC, transacciones, restricciones ni orden de MySQL, ni
supervivencia de los datos al reiniciar; esas comprobaciones están en la suite
de integración siguiente. Los flujos de convocatorias y programación incorporados
en develop tienen sus pruebas en `com.psicoapp.integracion`.

#### Integración con MySQL real

El perfil `pruebas-mysql` añade `PruebaPersistenciaMysqlIT` mediante Maven
Failsafe. Ejecuta HTTP, seguridad, servicios y adaptadores JDBC reales, sin
dobles. Crea un esquema aleatorio `psicoapp_pruebas_<uuid>` con el esquema del
backend y lo elimina al finalizar. No utiliza ni vacía la base de desarrollo.
El usuario de pruebas necesita permisos para crear y eliminar ese esquema.

```powershell
cd backend
$env:PSICOAPP_PRUEBAS_MYSQL_USUARIO = '<usuario-local>'
$env:PSICOAPP_PRUEBAS_MYSQL_CLAVE = '<clave-local>'
# Opcional; debe ser una URL de servidor sin base de datos ni parámetros:
$env:PSICOAPP_PRUEBAS_MYSQL_URL = 'jdbc:mysql://localhost:3306/'
.\mvnw.cmd -Ppruebas-mysql verify
```

Las credenciales se leen del entorno y no tienen valores predeterminados.
El perfil falla si faltan o MySQL no está disponible. Sin el perfil, `verify`
conserva las pruebas que no requieren base de datos; los archivos `*IT.java`
se reservan para Failsafe. Los informes de integración quedan en
`target/failsafe-reports/`. Consulta la [cobertura MySQL y sus resultados](../docs/pruebas/cobertura-mysql.md).

Las pruebas se ubican en `src/test/java/com/psicoapp`, siguiendo los mismos
módulos y capas cuando se incorporen funcionalidades. Las instrucciones de
arranque y compilación están en el [README principal](../README.md).

### Filtrado administrativo de usuarios

`busqueda` admite hasta 180 caracteres, ignora espacios exteriores y diferencias entre mayúsculas y minúsculas. Una consulta vacía conserva el listado habitual. Los caracteres `%`, `_` y `!` se buscan literalmente; los valores se enlazan como parámetros SQL. No se garantiza equivalencia de tildes entre motores. Una búsqueda demasiado larga devuelve 400; se mantienen la autorización administrativa y el formato de respuesta. No requiere migraciones.

La separación sigue SOLID: `FiltroUsuarios` normaliza los criterios sin depender del framework; el controlador recibe HTTP, el servicio verifica permisos y depende del puerto `RepositorioUsuarios`, y el adaptador JDBC construye la consulta parametrizada. Las pruebas cubren filtros combinados, comodines literales, límites y autorización con Spring/JDBC sobre H2; no sustituyen una ejecución contra MySQL.
