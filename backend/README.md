# Arquitectura del backend

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
Los demás módulos conservan únicamente su estructura inicial.

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

## Identity y academic — MVP

Se implementan usuarios, los tres roles, asignación de roles por administrador,
permiso `CREAR_CASOS` para el rol docente, registro de estudiantes institucionales
y creación/consulta de espacios académicos con nombre y periodo. La persistencia
usa MySQL y JDBC; las contraseñas se almacenan con BCrypt.

El esquema `src/main/resources/schema.sql` reproduce exclusivamente `USUARIO`,
`ROL`, `PERMISO`, `ROL_PERMISO` y `ESPACIO_ACADEMICO` del
[modelo ER](../docs/diseño/ModeloER.drawio). Los UUID se representan mediante
`CHAR(36)`. Los permisos pertenecen al rol según el ER: conceder `CREAR_CASOS`
afecta a todos los docentes. No hay permisos individuales ni tablas de membresía
de estudiantes a espacios académicos en ese modelo.

La fachada acotada `AcademicAdministrationFacade` coordina autorización mediante
el contrato público `IdentityDirectory` y operaciones de espacios académicos.
Los repositorios son puertos implementados en `infrastructure`; los dominios
son Java independiente de Spring y JDBC. No se introducen Proxy, Builder ni
otros patrones sin un caso de uso que los necesite.

### Configuración local

Requiere MySQL 8.0 o superior y una base de datos creada previamente:

```sql
CREATE DATABASE psicoapp CHARACTER SET utf8mb4;
```

Configura estas variables de entorno antes del arranque (sin versionar secretos):

| Variable | Uso / valor predeterminado |
| --- | --- |
| `PSICOAPP_DB_URL` | `jdbc:mysql://localhost:3306/psicoapp` |
| `PSICOAPP_DB_USER` | Usuario MySQL; predeterminado `psicoapp` |
| `PSICOAPP_DB_PASSWORD` | Contraseña MySQL; sin valor predeterminado |
| `PSICOAPP_ADMIN_EMAIL` | Correo del administrador inicial |
| `PSICOAPP_ADMIN_PASSWORD` | Clave inicial de al menos 8 caracteres y hasta 72 bytes |
| `PSICOAPP_INSTITUTIONAL_DOMAINS` | Dominios institucionales exactos separados por comas |

El arranque crea las cinco tablas si no existen e inserta los roles y el permiso
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
| `GET /api/identity/me` | Usuario autenticado: identidad y permiso, sin hashes |
| `GET /api/identity/csrf` | Usuario autenticado: token y nombre de cabecera CSRF |
| `POST /api/identity/teachers` | Administrador: `{ "email": "docente@universidad.edu.co", "name": "Docente", "password": "clave-ficticia" }` |
| `PUT /api/identity/users/{id}/role` | Administrador: `{ "role": "DOCENTE" }`; también `ADMINISTRADOR` o `ESTUDIANTE` |
| `PUT /api/identity/roles/DOCENTE/case-creation` | Administrador: `{ "enabled": true }` concede; `false` retira |
| `POST /api/identity/students` | Docente: `{ "email": "estudiante@universidad.edu.co", "name": "Estudiante" }`; reutiliza cuentas estudiantiles existentes |
| `POST /api/academic/spaces` | Docente: `{ "name": "Psicología social", "period": "2026-2" }` |
| `GET /api/academic/spaces` | Docente: espacios disponibles |
| `GET /api/academic/spaces/{id}` | Docente: espacio por UUID |

Antes de POST/PUT, consultar `/api/identity/csrf`, conservar la cookie recibida
y enviar el token devuelto en la cabecera indicada. Las peticiones incluyen
también HTTP Basic. La API responde 400 por datos inválidos, 401 por credenciales
inválidas, 403 por permisos/CSRF, 404 por registros inexistentes y 409 por conflictos.

`IdentityService.requireCaseCreation` expone la comprobación del permiso
específico para futuros casos de uso de creación de casos. El rol docente solo
no basta. Una cuenta estudiante sin contraseña no puede promoverse a personal;
el administrador crea las cuentas docentes con su contraseña.

### Límite de integración y trazabilidad

- RF-02, RF-03, RN-10 y CU-02/CU-03: administración de docentes, roles y permisos.
- RF-01: validación del correo institucional y autenticación estudiantil mediante
  el puerto `StudentAccessVerifier`. El futuro módulo propietario de las
  convocatorias debe implementar ese puerto verificando correo/usuario y código.
  **Sin adaptador de convocatorias, todo acceso estudiantil se deniega**; esta
  entrega no declara completo RF-01 ni RN-04/RF-20.
- RF-04: catálogo mínimo de espacios y registro/reutilización de estudiantes.
  La asociación espacio-sesión y las convocatorias requieren `SESION` y
  `CONVOCATORIA` del ER, fuera de estos dos módulos. No se declara completo RF-04.
- Programación, ventanas de ingreso, intentos, notificaciones, resultados y
  permisos de participación concreta quedan en sus módulos propietarios.

### Pruebas

Desde `backend/`, ejecutar `./mvnw.cmd verify` (PowerShell) o `sh mvnw verify`.
Las pruebas JUnit cubren reglas de usuarios y espacios, coordinación de la
fachada, credenciales y controles HTTP de rol/CSRF. Los nombres de las pruebas
enlazan RF/RN y el ER. Usan dobles de los puertos y no requieren MySQL.
La prueba de contexto desactiva solo la inicialización SQL y el alta inicial
del administrador; no verifica conectividad ni persistencia.

Las pruebas se ubican en `src/test/java/com/psicoapp`, siguiendo los mismos
módulos y capas cuando se incorporen funcionalidades. Las instrucciones de
arranque y compilación están en el [README principal](../README.md).
