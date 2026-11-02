# Arquitectura del backend

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
Las rutas son `POST /api/casos` y `GET /api/casos/{id}`, con HTTP Basic y CSRF.
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
Un planificador cierra automaticamente al alcanzar fin previsto + 24 horas,
incluidas sesiones nunca iniciadas. Las transiciones usan bloqueo transaccional.

Rutas docentes: `POST /api/sesiones`, `GET /api/sesiones/{id}`,
`POST /api/sesiones/{id}/iniciar` y `POST /api/sesiones/{id}/finalizar`.
Usan HTTP Basic y CSRF en POST. Programar exige una lista no vacia de UUID
`estudiantes` institucionales existentes y devuelve codigos iniciales de convocatoria.
Notificaciones y consolidacion de ausencias siguen pendientes; la gestion de
intentos se implementa en `participation` y la calificacion en `evaluation`.
Por ello CU-12/CU-16 aun no se declaran completos con todas sus integraciones.
Consulta [reglas, tiempo, integraciones y pruebas](../docs/pruebas/sesiones.md).

## Participacion — intentos

`participation` inicia intentos para estudiantes convocados, conserva la
ultima respuesta por pregunta y aplica la modalidad `FLEXIBLE` o `RESTRINGIDA`.
Las rutas son `POST /api/participaciones`, `GET /api/participaciones/{id}`,
`PUT /api/participaciones/{id}/respuestas` y las transiciones
`/avanzar`, `/retroceder`, `/reanudar` y `/finalizar`. La autorizacion de la
convocatoria y la ventana de ingreso se delegan a `sessions`. Un planificador
marca automaticamente como vencidos los intentos cuyo limite individual se
agota.

## Evaluacion — calificacion

`evaluation` calcula la nota de un intento finalizado con una estrategia de
ponderacion, asigna cero a preguntas sin respuesta y conserva la
retroalimentacion por pregunta. Las rutas son `POST /api/evaluaciones/{idIntento}`
y `GET /api/evaluaciones/{idIntento}`. El estudiante autenticado solo puede
consultar su propio intento.
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
permiso `CREAR_CASOS` para el rol docente, registro de estudiantes institucionales
y creación/consulta de espacios académicos con nombre y periodo. La persistencia
usa MySQL y JDBC; las contraseñas se almacenan con BCrypt.

La base de identidad y espacios en `src/main/resources/schema.sql` reproduce `USUARIO`,
`ROL`, `PERMISO`, `ROL_PERMISO` y `ESPACIO_ACADEMICO` del
[modelo ER](../docs/diseño/ModeloER.drawio). Los UUID se representan mediante
`CHAR(36)`. Los permisos pertenecen al rol según el ER: conceder `CREAR_CASOS`
afecta a todos los docentes. No hay permisos individuales ni tablas de membresía
de estudiantes a espacios académicos en ese modelo.

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

Configura estas variables de entorno antes del arranque (sin versionar secretos):

| Variable | Uso / valor predeterminado |
| --- | --- |
| `PSICOAPP_BD_URL` | `jdbc:mysql://localhost:3306/psicoapp` |
| `PSICOAPP_BD_USUARIO` | Usuario MySQL; predeterminado `psicoapp` |
| `PSICOAPP_BD_CLAVE` | Contraseña MySQL; sin valor predeterminado |
| `PSICOAPP_CORREO_ADMINISTRADOR` | Correo del administrador inicial |
| `PSICOAPP_CLAVE_ADMINISTRADOR` | Clave inicial de al menos 8 caracteres y hasta 72 bytes |
| `PSICOAPP_DOMINIOS_INSTITUCIONALES` | Dominios institucionales exactos separados por comas |

El arranque crea las tablas de identidad, espacios, casos y sesiones si no existen e inserta los roles y el permiso
sin duplicarlos. El administrador inicial se crea solo si no existe; no se
sobrescribe su contraseña en cada arranque. Sin variables de administrador no
se crea una cuenta predeterminada. Sin dominios configurados no se admiten
nuevos estudiantes. El usuario MySQL debe poder crear las tablas y consultar y
modificar sus registros. El esquema no migra tablas preexistentes incompatibles.

### API mínima

HTTP Basic utiliza el correo y la contraseña para administrador y docente.
El identificador del actor siempre procede de la autenticación del backend.
Para usarla fuera de localhost, servir la API mediante HTTPS.

| Método y ruta | Actor / resultado |
| --- | --- |
| `GET /api/identidad/yo` | Usuario autenticado: identidad y permiso, sin hashes |
| `GET /api/identidad/csrf` | Usuario autenticado: token y nombre de cabecera CSRF |
| `POST /api/identidad/docentes` | Administrador: `{ "correo": "docente@universidad.edu.co", "nombre": "Docente", "clave": "clave-ficticia" }` |
| `PUT /api/identidad/usuarios/{idUsuario}/rol` | Administrador: `{ "rol": "DOCENTE" }`; también `ADMINISTRADOR` o `ESTUDIANTE` |
| `PUT /api/identidad/roles/DOCENTE/creacion-casos` | Administrador: `{ "habilitado": true }` concede; `false` retira |
| `POST /api/identidad/estudiantes` | Docente: `{ "correo": "estudiante@universidad.edu.co", "nombre": "Estudiante" }`; reutiliza cuentas estudiantiles existentes |
| `POST /api/academico/espacios` | Docente: `{ "nombre": "Psicología social", "periodo": "2026-2" }` |
| `GET /api/academico/espacios` | Docente: espacios disponibles |
| `GET /api/academico/espacios/{id}` | Docente: espacio por UUID |

Antes de POST/PUT, consultar `/api/identidad/csrf`, conservar la cookie recibida
y enviar el token devuelto en la cabecera indicada. Las peticiones incluyen
también HTTP Basic. La API responde 400 por datos inválidos, 401 por credenciales
inválidas, 403 por permisos/CSRF, 404 por registros inexistentes y 409 por conflictos.

`ServicioUsuarios.exigirCreacionCasos` expone la comprobación del permiso
específico para futuros casos de uso de creación de casos. El rol docente solo
no basta. Una cuenta estudiante sin contraseña no puede promoverse a personal;
el administrador crea las cuentas docentes con su contraseña.

### Límite de integración y trazabilidad

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
fachada, credenciales y controles HTTP de rol/CSRF. Los nombres de las pruebas
enlazan RF/RN y el ER. Usan dobles de los puertos y no requieren MySQL.
La prueba de contexto desactiva solo la inicialización SQL y el alta inicial
del administrador; no verifica conectividad ni persistencia.

`PruebaFlujosIdentidadAcademica` complementa las pruebas unitarias y de seguridad
con los recorridos HTTP disponibles de CP-ID-001 a CP-ID-014 y CP-AC-001 a
CP-AC-007. Usa controladores, autenticación, validación y servicios reales;
solo sustituye los puertos de almacenamiento con dobles en memoria reiniciados
antes de cada ejecución. Las modificaciones obtienen cookie y token del endpoint
CSRF real. Comprueba también el estado posterior a los rechazos, la autorización
después de cambiar un rol y los permisos compartidos entre dos docentes.
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
