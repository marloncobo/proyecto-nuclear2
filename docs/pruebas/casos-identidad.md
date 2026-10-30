# Casos de prueba — identidad

**Proyecto:** Psicoapp · **Versión del documento:** 0.1.0 · **Fecha de elaboración:** 30-10-2026.
**Estado:** planeado, pendiente de revisión y ejecución. No representa un acta de resultados.

El objetivo principal es evaluar **CU-02 Gestionar docentes y CU-03 Asignar roles y permisos**. CU-01 se documenta con su limitación actual: la autenticación estudiantil requiere un adaptador de convocatorias. El registro de estudiantes es preparación de CU-13, no una convocatoria completa.

Este archivo desarrolla las fichas de la sección 6 del [plan de pruebas](plan-pruebas.md), adaptado de la [plantilla de referencia](https://docs.google.com/document/d/1zRXhuL-_D1xWvpCxZwF_iMPJmI1W9963/edit). Los CU, RF y RN proceden del [catálogo vigente](../requisitos/casos-de-uso.md); «preparación» y «soporte» identifican aportes parciales, sin crear nuevos CU oficiales.

## Casos planeados y trazabilidad

| Identificador | Caso de prueba | CU relacionado | Disponibilidad |
| --- | --- | --- | --- |
| [CP-ID-001](#cp-id-001) | CU-02 — Registrar docente | CU-02 | Disponible |
| [CP-ID-002](#cp-id-002) | CU-02 — Rechazar correo docente duplicado | CU-02 | Disponible |
| [CP-ID-003](#cp-id-003) | CU-02 — Validar datos de registro | CU-02 | Disponible |
| [CP-ID-004](#cp-id-004) | CU-02 — Impedir alta docente por otro docente | CU-02 | Disponible |
| [CP-ID-005](#cp-id-005) | CU-03 — Asignar y restituir rol | CU-03 | Disponible |
| [CP-ID-006](#cp-id-006) | CU-03 — Evitar que el administrador retire su propio rol | CU-03 | Disponible |
| [CP-ID-007](#cp-id-007) | CU-03 — Conceder y retirar permiso al rol docente | CU-03 | Disponible |
| [CP-ID-008](#cp-id-008) | CU-03 — Rechazar administración de permisos no autorizada | CU-03 | Disponible |
| [CP-ID-009](#cp-id-009) | Soporte de CU-02/CU-03 — Autenticar personal | CU-02, CU-03 (precondición de acceso; no es CU-01) | Disponible |
| [CP-ID-010](#cp-id-010) | Soporte de CU-13 — Registrar y reutilizar identidad estudiantil | CU-13 (preparación de identidad; convocatoria pendiente) | Disponible |
| [CP-ID-011](#cp-id-011) | Soporte de CU-13 — Rechazar correo externo o perteneciente al personal | CU-13 (preparación de identidad) | Disponible |
| [CP-ID-012](#cp-id-012) | CU-03 — Rechazar promoción de estudiante sin contraseña | CU-03 | Disponible |
| [CP-ID-013](#cp-id-013) | CU-02 — Rechazar modificación sin protección CSRF | CU-02 (control transversal) | Disponible |
| [CP-ID-014](#cp-id-014) | CU-01 — Denegar acceso estudiantil sin adaptador de convocatorias | CU-01 | Disponible |
| [CP-ID-015](#cp-id-015) | CU-01 — Autenticar estudiante con código válido | CU-01 | Bloqueado |
| [CP-ID-016](#cp-id-016) | CU-01 — Rechazar código ajeno, incorrecto o estudiante no convocado | CU-01; CU-22 como límite de autorización | Bloqueado |

Los enlaces de cada identificador usan anclas explícitas. En las fichas, «—» significa sin evaluar; el probador marca una sola columna de coincidencia después de observar el resultado.

<a id="cp-id-001"></a>

## CP-ID-001 — CU-02 — Registrar docente

### Información global del caso de prueba

| Campo | Valor |
| --- | --- |
| Caso de prueba n.º | CP-ID-001 |
| Versión de ejecución | Pendiente de registrar versión y hash realmente evaluados; base prevista: `0.0.1-SNAPSHOT`, `388b7bf` |
| Fecha de ejecución | Pendiente |
| Caso de uso | CU-02 |
| Módulo del sistema | `identity` — identidad |
| Requisitos / reglas | RF-03; RN-10 |
| Actor | Administrador |
| Disponibilidad | Disponible para ejecutar |
| Descripción del caso de prueba | Comprobar que un administrador registra una cuenta docente y que esta puede identificarse. |

### Caso de prueba

**Precondiciones:** Administrador activo, correo docente no registrado y preparación común del plan completada. Se aplican las condiciones comunes del [plan](plan-pruebas.md#52-preparación-común-de-la-ejecución).

| Paso | Acción | Datos de entrada — campo y valor | Tipo de escenario | Respuesta esperada de la aplicación | Coincide: sí | Coincide: no | Respuesta del sistema |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | Enviar POST /api/identidad/docentes con autenticación de administrador y CSRF vigente. | correo=docente.nuevo@universidad.example; nombre=Docente de prueba; clave=ClaveFicticia2026! | Positivo | HTTP 201; UUID generado, correo normalizado, rol DOCENTE; la respuesta no incluye clave ni hashClave. | — | — | Pendiente |
| 2 | Consultar GET /api/identidad/yo con las credenciales del nuevo docente. | Correo y clave del docente recién registrado. | Positivo | HTTP 200; el UUID coincide con el del alta y el rol es DOCENTE. | — | — | Pendiente |

**Postcondiciones:** Existe una sola cuenta docente activa. La clave almacenada debe ser un hash BCrypt; comprobarlo mediante consulta controlada de usuario sin registrar su valor en evidencias.

**Evidencias:** pendientes. Adjuntar captura o transcripción saneada de cada paso y de la postcondición. No adjuntar credenciales, cabeceras de autorización, códigos reales ni hashes.

### Resultados de la prueba

| Campo | Registro |
| --- | --- |
| Defectos y desviaciones | Pendiente de evaluar; registrar incidencia o «N/A» después de ejecutar |
| Veredicto | No ejecutado |
| Observaciones | Registrar resultados de cada variante; si alguna falla, el caso no puede declararse «Pasado». |
| Probador | Por asignar |
| Apoyo automatizado existente | PruebaServicioUsuarios.administradorRegistraDocenteConClaveCifrada; PruebaSeguridadIdentidad.administradorRegistraDocenteYRechazaDatosInvalidos. El recorrido completo con MySQL es manual. |

<a id="cp-id-002"></a>

## CP-ID-002 — CU-02 — Rechazar correo docente duplicado

### Información global del caso de prueba

| Campo | Valor |
| --- | --- |
| Caso de prueba n.º | CP-ID-002 |
| Versión de ejecución | Pendiente de registrar versión y hash realmente evaluados; base prevista: `0.0.1-SNAPSHOT`, `388b7bf` |
| Fecha de ejecución | Pendiente |
| Caso de uso | CU-02 |
| Módulo del sistema | `identity` — identidad |
| Requisitos / reglas | RF-03; RN-10 |
| Actor | Administrador |
| Disponibilidad | Disponible para ejecutar |
| Descripción del caso de prueba | Impedir que variantes de mayúsculas creen identidades duplicadas por la API. |

### Caso de prueba

**Precondiciones:** Existe docente.nuevo@universidad.example; administrador activo. Se aplican las condiciones comunes del [plan](plan-pruebas.md#52-preparación-común-de-la-ejecución).

| Paso | Acción | Datos de entrada — campo y valor | Tipo de escenario | Respuesta esperada de la aplicación | Coincide: sí | Coincide: no | Respuesta del sistema |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | Repetir el alta mediante POST /api/identidad/docentes. | correo=DOCENTE.NUEVO@UNIVERSIDAD.EXAMPLE; nombre=Duplicado; clave=ClaveFicticia2026! | Negativo | HTTP 409; no se sobrescribe la cuenta existente. | — | — | Pendiente |
| 2 | Autenticarse como el docente original y consultar /api/identidad/yo. | Credenciales originales. | Comprobación | HTTP 200; conserva UUID, nombre y rol originales. | — | — | Pendiente |

**Postcondiciones:** El número de cuentas con el correo normalizado no aumenta.

**Evidencias:** pendientes. Adjuntar captura o transcripción saneada de cada paso y de la postcondición. No adjuntar credenciales, cabeceras de autorización, códigos reales ni hashes.

### Resultados de la prueba

| Campo | Registro |
| --- | --- |
| Defectos y desviaciones | Pendiente de evaluar; registrar incidencia o «N/A» después de ejecutar |
| Veredicto | No ejecutado |
| Observaciones | Registrar resultados de cada variante; si alguna falla, el caso no puede declararse «Pasado». |
| Probador | Por asignar |
| Apoyo automatizado existente | PruebaServicioUsuarios.rechazaCorreoDuplicado; la normalización y la restricción UNIQUE de MySQL deben verificarse juntas mediante la API. |

<a id="cp-id-003"></a>

## CP-ID-003 — CU-02 — Validar datos de registro

### Información global del caso de prueba

| Campo | Valor |
| --- | --- |
| Caso de prueba n.º | CP-ID-003 |
| Versión de ejecución | Pendiente de registrar versión y hash realmente evaluados; base prevista: `0.0.1-SNAPSHOT`, `388b7bf` |
| Fecha de ejecución | Pendiente |
| Caso de uso | CU-02 |
| Módulo del sistema | `identity` — identidad |
| Requisitos / reglas | RF-03; RN-10 |
| Actor | Administrador |
| Disponibilidad | Disponible para ejecutar |
| Descripción del caso de prueba | Rechazar datos incompletos o fuera de los límites del usuario y de BCrypt. |

### Caso de prueba

**Precondiciones:** Administrador activo; usar un correo distinto aún no registrado por cada variante. Se aplican las condiciones comunes del [plan](plan-pruebas.md#52-preparación-común-de-la-ejecución).

| Paso | Acción | Datos de entrada — campo y valor | Tipo de escenario | Respuesta esperada de la aplicación | Coincide: sí | Coincide: no | Respuesta del sistema |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | Repetir POST /api/identidad/docentes por cada variante, conservando válidos los otros campos. | nombre vacío o de 121 caracteres; correo="sin-correo"; clave="corta"; clave=40 caracteres ñ (80 bytes UTF-8). | Negativo / límites | HTTP 400 en cada variante. Para el último caso se comprueba el límite de 72 bytes, además de la cantidad de caracteres. | — | — | Pendiente |
| 2 | Buscar los correos usados mediante una consulta controlada de usuario. | Correos de cada variante. | Comprobación | No existe una cuenta creada por las solicitudes rechazadas. | — | — | Pendiente |

**Postcondiciones:** Las altas inválidas no dejan usuarios parciales.

**Evidencias:** pendientes. Adjuntar captura o transcripción saneada de cada paso y de la postcondición. No adjuntar credenciales, cabeceras de autorización, códigos reales ni hashes.

### Resultados de la prueba

| Campo | Registro |
| --- | --- |
| Defectos y desviaciones | Pendiente de evaluar; registrar incidencia o «N/A» después de ejecutar |
| Veredicto | No ejecutado |
| Observaciones | Registrar resultados de cada variante; si alguna falla, el caso no puede declararse «Pasado». |
| Probador | Por asignar |
| Apoyo automatizado existente | PruebaServicioUsuarios.rechazaClavesInvalidas; PruebaUsuario.validaNombreCorreoYClaveDelPersonal; PruebaSeguridadIdentidad.administradorRegistraDocenteYRechazaDatosInvalidos. Cobertura parcial de las variantes HTTP. |

<a id="cp-id-004"></a>

## CP-ID-004 — CU-02 — Impedir alta docente por otro docente

### Información global del caso de prueba

| Campo | Valor |
| --- | --- |
| Caso de prueba n.º | CP-ID-004 |
| Versión de ejecución | Pendiente de registrar versión y hash realmente evaluados; base prevista: `0.0.1-SNAPSHOT`, `388b7bf` |
| Fecha de ejecución | Pendiente |
| Caso de uso | CU-02 |
| Módulo del sistema | `identity` — identidad |
| Requisitos / reglas | RF-02, RF-03; RN-10 |
| Actor | Docente |
| Disponibilidad | Disponible para ejecutar |
| Descripción del caso de prueba | Confirmar que la gestión de cuentas docentes está reservada al administrador. |

### Caso de prueba

**Precondiciones:** Docente activo autenticado; solicitud válida y token CSRF vigente. Se aplican las condiciones comunes del [plan](plan-pruebas.md#52-preparación-común-de-la-ejecución).

| Paso | Acción | Datos de entrada — campo y valor | Tipo de escenario | Respuesta esperada de la aplicación | Coincide: sí | Coincide: no | Respuesta del sistema |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | Enviar POST /api/identidad/docentes como docente. | correo=otro.docente@universidad.example; nombre=Otro docente; clave=ClaveFicticia2026! | Negativo / autorización | HTTP 403; la operación no llega al alta de usuarios. | — | — | Pendiente |

**Postcondiciones:** No se crea otro.docente@universidad.example.

**Evidencias:** pendientes. Adjuntar captura o transcripción saneada de cada paso y de la postcondición. No adjuntar credenciales, cabeceras de autorización, códigos reales ni hashes.

### Resultados de la prueba

| Campo | Registro |
| --- | --- |
| Defectos y desviaciones | Pendiente de evaluar; registrar incidencia o «N/A» después de ejecutar |
| Veredicto | No ejecutado |
| Observaciones | Registrar resultados de cada variante; si alguna falla, el caso no puede declararse «Pasado». |
| Probador | Por asignar |
| Apoyo automatizado existente | PruebaServicioUsuarios.docenteNoAdministraUsuariosNiPermisos; PruebaSeguridadIdentidad.docenteNoRegistraOtroDocente. |

<a id="cp-id-005"></a>

## CP-ID-005 — CU-03 — Asignar y restituir rol

### Información global del caso de prueba

| Campo | Valor |
| --- | --- |
| Caso de prueba n.º | CP-ID-005 |
| Versión de ejecución | Pendiente de registrar versión y hash realmente evaluados; base prevista: `0.0.1-SNAPSHOT`, `388b7bf` |
| Fecha de ejecución | Pendiente |
| Caso de uso | CU-03 |
| Módulo del sistema | `identity` — identidad |
| Requisitos / reglas | RF-02, RF-03; RN-10 |
| Actor | Administrador |
| Disponibilidad | Disponible para ejecutar |
| Descripción del caso de prueba | Comprobar que el administrador cambia el rol de una cuenta con contraseña y que el cambio se aplica en la siguiente solicitud. |

### Caso de prueba

**Precondiciones:** Administrador y docente de prueba activos, con UUID conocidos. La cuenta docente tiene contraseña propia. Se aplican las condiciones comunes del [plan](plan-pruebas.md#52-preparación-común-de-la-ejecución).

| Paso | Acción | Datos de entrada — campo y valor | Tipo de escenario | Respuesta esperada de la aplicación | Coincide: sí | Coincide: no | Respuesta del sistema |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | Enviar PUT /api/identidad/usuarios/{idUsuario}/rol para la cuenta docente. | rol=ADMINISTRADOR | Positivo | HTTP 200; mismo UUID y rol ADMINISTRADOR. | — | — | Pendiente |
| 2 | Consultar /api/identidad/yo con las credenciales de esa cuenta. | Credenciales originales de la cuenta modificada. | Positivo | HTTP 200; refleja el rol ADMINISTRADOR sin exigir cambiar la contraseña. | — | — | Pendiente |
| 3 | Restituir DOCENTE utilizando el administrador inicial. | rol=DOCENTE | Restauración | HTTP 200; la cuenta vuelve a DOCENTE. | — | — | Pendiente |

**Postcondiciones:** La cuenta conserva su UUID y su clave; termina con el rol original.

**Evidencias:** pendientes. Adjuntar captura o transcripción saneada de cada paso y de la postcondición. No adjuntar credenciales, cabeceras de autorización, códigos reales ni hashes.

### Resultados de la prueba

| Campo | Registro |
| --- | --- |
| Defectos y desviaciones | Pendiente de evaluar; registrar incidencia o «N/A» después de ejecutar |
| Veredicto | No ejecutado |
| Observaciones | Registrar resultados de cada variante; si alguna falla, el caso no puede declararse «Pasado». |
| Probador | Por asignar |
| Apoyo automatizado existente | PruebaServicioUsuarios.administradorCambiaRol. La lectura autenticada posterior se comprueba manualmente. |

<a id="cp-id-006"></a>

## CP-ID-006 — CU-03 — Evitar que el administrador retire su propio rol

### Información global del caso de prueba

| Campo | Valor |
| --- | --- |
| Caso de prueba n.º | CP-ID-006 |
| Versión de ejecución | Pendiente de registrar versión y hash realmente evaluados; base prevista: `0.0.1-SNAPSHOT`, `388b7bf` |
| Fecha de ejecución | Pendiente |
| Caso de uso | CU-03 |
| Módulo del sistema | `identity` — identidad |
| Requisitos / reglas | RF-03; RN-10 |
| Actor | Administrador |
| Disponibilidad | Disponible para ejecutar |
| Descripción del caso de prueba | Validar la salvaguarda implementada contra la pérdida del acceso administrativo propio. |

### Caso de prueba

**Precondiciones:** Administrador activo; conocer su UUID desde /api/identidad/yo. Se aplican las condiciones comunes del [plan](plan-pruebas.md#52-preparación-común-de-la-ejecución).

| Paso | Acción | Datos de entrada — campo y valor | Tipo de escenario | Respuesta esperada de la aplicación | Coincide: sí | Coincide: no | Respuesta del sistema |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | Enviar PUT /api/identidad/usuarios/{idUsuario}/rol usando el propio UUID. | rol=DOCENTE | Negativo / salvaguarda del MVP | HTTP 409. | — | — | Pendiente |
| 2 | Volver a consultar /api/identidad/yo. | Credenciales del administrador. | Comprobación | HTTP 200; mantiene ADMINISTRADOR. | — | — | Pendiente |

**Postcondiciones:** No se modifica el rol del administrador. Esta salvaguarda concreta es una decisión de implementación, no una RN adicional del catálogo.

**Evidencias:** pendientes. Adjuntar captura o transcripción saneada de cada paso y de la postcondición. No adjuntar credenciales, cabeceras de autorización, códigos reales ni hashes.

### Resultados de la prueba

| Campo | Registro |
| --- | --- |
| Defectos y desviaciones | Pendiente de evaluar; registrar incidencia o «N/A» después de ejecutar |
| Veredicto | No ejecutado |
| Observaciones | Registrar resultados de cada variante; si alguna falla, el caso no puede declararse «Pasado». |
| Probador | Por asignar |
| Apoyo automatizado existente | PruebaServicioUsuarios.impideRetirarRolPropioDeAdministrador. |

<a id="cp-id-007"></a>

## CP-ID-007 — CU-03 — Conceder y retirar permiso al rol docente

### Información global del caso de prueba

| Campo | Valor |
| --- | --- |
| Caso de prueba n.º | CP-ID-007 |
| Versión de ejecución | Pendiente de registrar versión y hash realmente evaluados; base prevista: `0.0.1-SNAPSHOT`, `388b7bf` |
| Fecha de ejecución | Pendiente |
| Caso de uso | CU-03 |
| Módulo del sistema | `identity` — identidad |
| Requisitos / reglas | RF-02, RF-03; RN-10 |
| Actor | Administrador; dos docentes como comprobadores |
| Disponibilidad | Disponible para ejecutar |
| Descripción del caso de prueba | Verificar la asignación de CREAR_CASOS al rol según ROL_PERMISO, con efecto sobre todos los docentes. |

### Caso de prueba

**Precondiciones:** Administrador y dos docentes activos. Registrar el valor inicial del permiso para restaurarlo al terminar. Se aplican las condiciones comunes del [plan](plan-pruebas.md#52-preparación-común-de-la-ejecución).

| Paso | Acción | Datos de entrada — campo y valor | Tipo de escenario | Respuesta esperada de la aplicación | Coincide: sí | Coincide: no | Respuesta del sistema |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | Enviar PUT /api/identidad/roles/DOCENTE/creacion-casos como administrador. | habilitado=true | Positivo | HTTP 204. | — | — | Pendiente |
| 2 | Consultar /api/identidad/yo con cada uno de los dos docentes. | Credenciales de ambos docentes. | Positivo | HTTP 200; puedeCrearCasos=true en ambos. | — | — | Pendiente |
| 3 | Retirar el permiso y consultar nuevamente ambas identidades. | habilitado=false | Positivo / revocación | PUT devuelve 204 y las dos consultas muestran puedeCrearCasos=false. | — | — | Pendiente |
| 4 | Restituir el valor inicial del permiso. | Valor guardado en las precondiciones. | Restauración | El rol queda con su configuración original. | — | — | Pendiente |

**Postcondiciones:** Solo cambia la relación ROL_PERMISO. No se crean permisos individuales. No se evalúa una creación real de caso porque ese módulo está pendiente.

**Evidencias:** pendientes. Adjuntar captura o transcripción saneada de cada paso y de la postcondición. No adjuntar credenciales, cabeceras de autorización, códigos reales ni hashes.

### Resultados de la prueba

| Campo | Registro |
| --- | --- |
| Defectos y desviaciones | Pendiente de evaluar; registrar incidencia o «N/A» después de ejecutar |
| Veredicto | No ejecutado |
| Observaciones | Registrar resultados de cada variante; si alguna falla, el caso no puede declararse «Pasado». |
| Probador | Por asignar |
| Apoyo automatizado existente | PruebaServicioUsuarios.administradorGestionaPermisosDelRol; exigePermisoEspecificoParaCasos; PruebaSeguridadIdentidad.administradorConfiguraPermisosConCamposEnEspanol. El efecto sobre dos cuentas con MySQL es manual. |

<a id="cp-id-008"></a>

## CP-ID-008 — CU-03 — Rechazar administración de permisos no autorizada

### Información global del caso de prueba

| Campo | Valor |
| --- | --- |
| Caso de prueba n.º | CP-ID-008 |
| Versión de ejecución | Pendiente de registrar versión y hash realmente evaluados; base prevista: `0.0.1-SNAPSHOT`, `388b7bf` |
| Fecha de ejecución | Pendiente |
| Caso de uso | CU-03 |
| Módulo del sistema | `identity` — identidad |
| Requisitos / reglas | RF-02, RF-03; RN-10 |
| Actor | Docente y administrador en escenarios independientes |
| Disponibilidad | Disponible para ejecutar |
| Descripción del caso de prueba | Rechazar al actor sin rol administrativo y la asignación del permiso de creación de casos a un rol no permitido. |

### Caso de prueba

**Precondiciones:** Docente y administrador activos. Anotar el permiso inicial del rol docente. Se aplican las condiciones comunes del [plan](plan-pruebas.md#52-preparación-común-de-la-ejecución).

| Paso | Acción | Datos de entrada — campo y valor | Tipo de escenario | Respuesta esperada de la aplicación | Coincide: sí | Coincide: no | Respuesta del sistema |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | Enviar PUT /api/identidad/roles/DOCENTE/creacion-casos como docente. | habilitado=true | Negativo / autorización | HTTP 403; el permiso no cambia. | — | — | Pendiente |
| 2 | Enviar PUT /api/identidad/roles/ESTUDIANTE/creacion-casos como administrador. | habilitado=true | Negativo / regla implementada | HTTP 400; CREAR_CASOS solo se configura para DOCENTE en este MVP. | — | — | Pendiente |

**Postcondiciones:** No se alteran los permisos de ninguno de los roles.

**Evidencias:** pendientes. Adjuntar captura o transcripción saneada de cada paso y de la postcondición. No adjuntar credenciales, cabeceras de autorización, códigos reales ni hashes.

### Resultados de la prueba

| Campo | Registro |
| --- | --- |
| Defectos y desviaciones | Pendiente de evaluar; registrar incidencia o «N/A» después de ejecutar |
| Veredicto | No ejecutado |
| Observaciones | Registrar resultados de cada variante; si alguna falla, el caso no puede declararse «Pasado». |
| Probador | Por asignar |
| Apoyo automatizado existente | PruebaServicioUsuarios.docenteNoAdministraUsuariosNiPermisos; administradorGestionaPermisosDelRol. La segunda variante no tiene una prueba HTTP específica. |

<a id="cp-id-009"></a>

## CP-ID-009 — Soporte de CU-02/CU-03 — Autenticar personal

### Información global del caso de prueba

| Campo | Valor |
| --- | --- |
| Caso de prueba n.º | CP-ID-009 |
| Versión de ejecución | Pendiente de registrar versión y hash realmente evaluados; base prevista: `0.0.1-SNAPSHOT`, `388b7bf` |
| Fecha de ejecución | Pendiente |
| Caso de uso | CU-02, CU-03 (precondición de acceso; no es CU-01) |
| Módulo del sistema | `identity` — identidad |
| Requisitos / reglas | RF-02; RN-10 |
| Actor | Administrador o docente |
| Disponibilidad | Disponible para ejecutar |
| Descripción del caso de prueba | Comprobar autenticación HTTP Basic y rechazo uniforme de credenciales incorrectas. |

### Caso de prueba

**Precondiciones:** Cuenta de personal activa con correo y contraseña conocidos. Se aplican las condiciones comunes del [plan](plan-pruebas.md#52-preparación-común-de-la-ejecución).

| Paso | Acción | Datos de entrada — campo y valor | Tipo de escenario | Respuesta esperada de la aplicación | Coincide: sí | Coincide: no | Respuesta del sistema |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | Consultar GET /api/identidad/yo con credenciales válidas. | Correo del personal y su clave configurada. | Positivo | HTTP 200 con id, correo, nombre, rol y puedeCrearCasos; sin claves ni hashes. | — | — | Pendiente |
| 2 | Repetir con contraseña incorrecta y después sin autenticación. | Clave incorrecta; luego sin cabecera Authorization. | Negativo | HTTP 401 en ambos escenarios. | — | — | Pendiente |

**Postcondiciones:** No se modifican datos del usuario. No confundir este acceso del personal con la autenticación estudiantil de CU-01.

**Evidencias:** pendientes. Adjuntar captura o transcripción saneada de cada paso y de la postcondición. No adjuntar credenciales, cabeceras de autorización, códigos reales ni hashes.

### Resultados de la prueba

| Campo | Registro |
| --- | --- |
| Defectos y desviaciones | Pendiente de evaluar; registrar incidencia o «N/A» después de ejecutar |
| Veredicto | No ejecutado |
| Observaciones | Registrar resultados de cada variante; si alguna falla, el caso no puede declararse «Pasado». |
| Probador | Por asignar |
| Apoyo automatizado existente | PruebaProveedorAutenticacionIdentidad.autenticaPersonalSinExponerCredenciales; rechazaClaveIncorrectaDelPersonal; PruebaSeguridadIdentidad.autenticaCredencialesBasicasReales; solicitudAnonimaNoEstaAutorizada. |

<a id="cp-id-010"></a>

## CP-ID-010 — Soporte de CU-13 — Registrar y reutilizar identidad estudiantil

### Información global del caso de prueba

| Campo | Valor |
| --- | --- |
| Caso de prueba n.º | CP-ID-010 |
| Versión de ejecución | Pendiente de registrar versión y hash realmente evaluados; base prevista: `0.0.1-SNAPSHOT`, `388b7bf` |
| Fecha de ejecución | Pendiente |
| Caso de uso | CU-13 (preparación de identidad; convocatoria pendiente) |
| Módulo del sistema | `identity` — identidad |
| Requisitos / reglas | RF-01, RF-04; RN-04 (sin verificar participación) |
| Actor | Docente |
| Disponibilidad | Disponible para ejecutar |
| Descripción del caso de prueba | Registrar al estudiante institucional y reutilizar su identidad sin duplicarla. |

### Caso de prueba

**Precondiciones:** Docente activo; dominio universidad.example configurado; el correo estudiantil no existe al inicio. Se aplican las condiciones comunes del [plan](plan-pruebas.md#52-preparación-común-de-la-ejecución).

| Paso | Acción | Datos de entrada — campo y valor | Tipo de escenario | Respuesta esperada de la aplicación | Coincide: sí | Coincide: no | Respuesta del sistema |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | Enviar POST /api/identidad/estudiantes. | correo=estudiante.prueba@universidad.example; nombre=Estudiante de prueba | Positivo | HTTP 201; se devuelve un UUID con rol ESTUDIANTE y sin contraseña. | — | — | Pendiente |
| 2 | Repetir la solicitud con el correo en mayúsculas. | correo=ESTUDIANTE.PRUEBA@UNIVERSIDAD.EXAMPLE; nombre=Estudiante de prueba | Positivo / reutilización | HTTP 201 según el contrato actual; retorna el mismo UUID y no crea otra cuenta. | — | — | Pendiente |

**Postcondiciones:** Existe una sola identidad estudiantil. No se crea una convocatoria ni una autorización para participar.

**Evidencias:** pendientes. Adjuntar captura o transcripción saneada de cada paso y de la postcondición. No adjuntar credenciales, cabeceras de autorización, códigos reales ni hashes.

### Resultados de la prueba

| Campo | Registro |
| --- | --- |
| Defectos y desviaciones | Pendiente de evaluar; registrar incidencia o «N/A» después de ejecutar |
| Veredicto | No ejecutado |
| Observaciones | Registrar resultados de cada variante; si alguna falla, el caso no puede declararse «Pasado». |
| Probador | Por asignar |
| Apoyo automatizado existente | PruebaServicioUsuarios.registraEstudianteInstitucional; reutilizaEstudianteExistente. La persistencia y el estado HTTP de reutilización se comprueban manualmente. |

<a id="cp-id-011"></a>

## CP-ID-011 — Soporte de CU-13 — Rechazar correo externo o perteneciente al personal

### Información global del caso de prueba

| Campo | Valor |
| --- | --- |
| Caso de prueba n.º | CP-ID-011 |
| Versión de ejecución | Pendiente de registrar versión y hash realmente evaluados; base prevista: `0.0.1-SNAPSHOT`, `388b7bf` |
| Fecha de ejecución | Pendiente |
| Caso de uso | CU-13 (preparación de identidad) |
| Módulo del sistema | `identity` — identidad |
| Requisitos / reglas | RF-01, RF-04; RN-04 (sin verificar participación) |
| Actor | Docente |
| Disponibilidad | Disponible para ejecutar |
| Descripción del caso de prueba | Impedir estudiantes con dominios ajenos o correos ya asignados al personal. |

### Caso de prueba

**Precondiciones:** Docente activo; solo universidad.example permitido; existe otro docente con correo docente.existente@universidad.example. Se aplican las condiciones comunes del [plan](plan-pruebas.md#52-preparación-común-de-la-ejecución).

| Paso | Acción | Datos de entrada — campo y valor | Tipo de escenario | Respuesta esperada de la aplicación | Coincide: sí | Coincide: no | Respuesta del sistema |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | Registrar estudiantes con las dos variantes externas. | correo=alumno@externo.example; correo=alumno@universidad.example.externo.example; nombre=Prueba | Negativo / dominio | HTTP 400 en cada variante; el dominio debe coincidir exactamente. | — | — | Pendiente |
| 2 | Registrar estudiante con el correo del docente existente. | correo=docente.existente@universidad.example; nombre=Prueba | Negativo / identidad | HTTP 409; no transforma la cuenta docente en estudiante. | — | — | Pendiente |

**Postcondiciones:** No se crean cuentas externas ni se cambia la cuenta del personal.

**Evidencias:** pendientes. Adjuntar captura o transcripción saneada de cada paso y de la postcondición. No adjuntar credenciales, cabeceras de autorización, códigos reales ni hashes.

### Resultados de la prueba

| Campo | Registro |
| --- | --- |
| Defectos y desviaciones | Pendiente de evaluar; registrar incidencia o «N/A» después de ejecutar |
| Veredicto | No ejecutado |
| Observaciones | Registrar resultados de cada variante; si alguna falla, el caso no puede declararse «Pasado». |
| Probador | Por asignar |
| Apoyo automatizado existente | PruebaServicioUsuarios.rechazaCorreoNoInstitucional; rechazaCorreoDelPersonalParaEstudiante. |

<a id="cp-id-012"></a>

## CP-ID-012 — CU-03 — Rechazar promoción de estudiante sin contraseña

### Información global del caso de prueba

| Campo | Valor |
| --- | --- |
| Caso de prueba n.º | CP-ID-012 |
| Versión de ejecución | Pendiente de registrar versión y hash realmente evaluados; base prevista: `0.0.1-SNAPSHOT`, `388b7bf` |
| Fecha de ejecución | Pendiente |
| Caso de uso | CU-03 |
| Módulo del sistema | `identity` — identidad |
| Requisitos / reglas | RF-03; RN-10 |
| Actor | Administrador |
| Disponibilidad | Disponible para ejecutar |
| Descripción del caso de prueba | Comprobar que una identidad estudiantil sin contraseña no se convierte en cuenta de personal inválida. |

### Caso de prueba

**Precondiciones:** Administrador activo y estudiante registrado mediante CP-ID-010, sin contraseña. Se aplican las condiciones comunes del [plan](plan-pruebas.md#52-preparación-común-de-la-ejecución).

| Paso | Acción | Datos de entrada — campo y valor | Tipo de escenario | Respuesta esperada de la aplicación | Coincide: sí | Coincide: no | Respuesta del sistema |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | Enviar PUT /api/identidad/usuarios/{idUsuario}/rol para el estudiante. | rol=DOCENTE; repetir en ejecución independiente con rol=ADMINISTRADOR | Negativo / invariante de usuario | HTTP 400 en cada variante; el personal requiere contraseña. | — | — | Pendiente |
| 2 | Comprobar el registro por una consulta controlada de usuario. | UUID del estudiante. | Comprobación | Conserva ESTUDIANTE y contraseña nula. | — | — | Pendiente |

**Postcondiciones:** No se modifica el estudiante; las cuentas docentes deben crearse mediante el caso de alta.

**Evidencias:** pendientes. Adjuntar captura o transcripción saneada de cada paso y de la postcondición. No adjuntar credenciales, cabeceras de autorización, códigos reales ni hashes.

### Resultados de la prueba

| Campo | Registro |
| --- | --- |
| Defectos y desviaciones | Pendiente de evaluar; registrar incidencia o «N/A» después de ejecutar |
| Veredicto | No ejecutado |
| Observaciones | Registrar resultados de cada variante; si alguna falla, el caso no puede declararse «Pasado». |
| Probador | Por asignar |
| Apoyo automatizado existente | PruebaUsuario.validaNombreCorreoYClaveDelPersonal verifica la invariante; el cambio de rol del estudiante por HTTP no tiene prueba específica. |

<a id="cp-id-013"></a>

## CP-ID-013 — CU-02 — Rechazar modificación sin protección CSRF

### Información global del caso de prueba

| Campo | Valor |
| --- | --- |
| Caso de prueba n.º | CP-ID-013 |
| Versión de ejecución | Pendiente de registrar versión y hash realmente evaluados; base prevista: `0.0.1-SNAPSHOT`, `388b7bf` |
| Fecha de ejecución | Pendiente |
| Caso de uso | CU-02 (control transversal) |
| Módulo del sistema | `identity` — identidad |
| Requisitos / reglas | RF-02, RF-03; RN-10 |
| Actor | Administrador |
| Disponibilidad | Disponible para ejecutar |
| Descripción del caso de prueba | Verificar que autenticarse no permite omitir el control CSRF en un alta. |

### Caso de prueba

**Precondiciones:** Administrador activo. Usar datos válidos y correo no registrado. Se aplican las condiciones comunes del [plan](plan-pruebas.md#52-preparación-común-de-la-ejecución).

| Paso | Acción | Datos de entrada — campo y valor | Tipo de escenario | Respuesta esperada de la aplicación | Coincide: sí | Coincide: no | Respuesta del sistema |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | Enviar POST /api/identidad/docentes con HTTP Basic, omitiendo cookie y cabecera CSRF. | correo=sin.csrf@universidad.example; nombre=Prueba; clave=ClaveFicticia2026! | Negativo / seguridad | HTTP 403; no se registra la cuenta. | — | — | Pendiente |

**Postcondiciones:** No existe sin.csrf@universidad.example.

**Evidencias:** pendientes. Adjuntar captura o transcripción saneada de cada paso y de la postcondición. No adjuntar credenciales, cabeceras de autorización, códigos reales ni hashes.

### Resultados de la prueba

| Campo | Registro |
| --- | --- |
| Defectos y desviaciones | Pendiente de evaluar; registrar incidencia o «N/A» después de ejecutar |
| Veredicto | No ejecutado |
| Observaciones | Registrar resultados de cada variante; si alguna falla, el caso no puede declararse «Pasado». |
| Probador | Por asignar |
| Apoyo automatizado existente | PruebaSeguridadIdentidad.rechazaModificacionSinCsrf cubre el mismo control sobre alta de espacios; este recorrido de alta docente es un caso manual complementario. |

<a id="cp-id-014"></a>

## CP-ID-014 — CU-01 — Denegar acceso estudiantil sin adaptador de convocatorias

### Información global del caso de prueba

| Campo | Valor |
| --- | --- |
| Caso de prueba n.º | CP-ID-014 |
| Versión de ejecución | Pendiente de registrar versión y hash realmente evaluados; base prevista: `0.0.1-SNAPSHOT`, `388b7bf` |
| Fecha de ejecución | Pendiente |
| Caso de uso | CU-01 |
| Módulo del sistema | `identity` — identidad |
| Requisitos / reglas | RF-01, RF-20; RN-04 |
| Actor | Estudiante |
| Disponibilidad | Disponible para ejecutar |
| Descripción del caso de prueba | Comprobar el rechazo seguro actual cuando no existe integración de códigos de convocatoria. |

### Caso de prueba

**Precondiciones:** Estudiante institucional registrado; no hay un componente que implemente VerificadorAccesoEstudiante, como en el estado base. Se aplican las condiciones comunes del [plan](plan-pruebas.md#52-preparación-común-de-la-ejecución).

| Paso | Acción | Datos de entrada — campo y valor | Tipo de escenario | Respuesta esperada de la aplicación | Coincide: sí | Coincide: no | Respuesta del sistema |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | Consultar GET /api/identidad/yo mediante HTTP Basic del estudiante. | Correo registrado; código ficticio CodigoNoVerificable2026. | Negativo / integración ausente | HTTP 401; tener una identidad estudiantil no basta para autenticarse. | — | — | Pendiente |

**Postcondiciones:** No hay autenticación exitosa ni acceso a participación. Este rechazo no demuestra que se valide una convocatoria real.

**Evidencias:** pendientes. Adjuntar captura o transcripción saneada de cada paso y de la postcondición. No adjuntar credenciales, cabeceras de autorización, códigos reales ni hashes.

### Resultados de la prueba

| Campo | Registro |
| --- | --- |
| Defectos y desviaciones | Pendiente de evaluar; registrar incidencia o «N/A» después de ejecutar |
| Veredicto | No ejecutado |
| Observaciones | Registrar resultados de cada variante; si alguna falla, el caso no puede declararse «Pasado». |
| Probador | Por asignar |
| Apoyo automatizado existente | PruebaProveedorAutenticacionIdentidad.rechazaEstudianteSinConvocatoriaVerificada; usa dobles de integración. |

<a id="cp-id-015"></a>

## CP-ID-015 — CU-01 — Autenticar estudiante con código válido

### Información global del caso de prueba

| Campo | Valor |
| --- | --- |
| Caso de prueba n.º | CP-ID-015 |
| Versión de ejecución | Pendiente de registrar versión y hash realmente evaluados; base prevista: `0.0.1-SNAPSHOT`, `388b7bf` |
| Fecha de ejecución | Pendiente |
| Caso de uso | CU-01 |
| Módulo del sistema | `identity` — identidad |
| Requisitos / reglas | RF-01, RF-20; RN-04 |
| Actor | Estudiante |
| Disponibilidad | Bloqueado por integración pendiente |
| Descripción del caso de prueba | Validar el recorrido real de correo institucional y código de convocatoria cuando exista su adaptador. |

### Caso de prueba

**Precondiciones:** Pendiente: adaptador real de VerificadorAccesoEstudiante y convocatoria autorizada para el estudiante, con código conocido y datos persistidos. Se aplican las condiciones comunes del [plan](plan-pruebas.md#52-preparación-común-de-la-ejecución).

| Paso | Acción | Datos de entrada — campo y valor | Tipo de escenario | Respuesta esperada de la aplicación | Coincide: sí | Coincide: no | Respuesta del sistema |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | Preparar una convocatoria real autorizada por el módulo propietario. | Estudiante institucional y código emitido por la convocatoria. | Positivo / integración pendiente | La convocatoria queda asociada al estudiante; no basta un doble que devuelva true. | — | — | Pendiente |
| 2 | Consultar /api/identidad/yo con correo y código de esa convocatoria. | Correo institucional y código válido. | Positivo / integración pendiente | HTTP 200 y rol ESTUDIANTE, si la integración futura conserva el contrato actual. La operación de iniciar participación debe validar además CU-22/CU-23. | — | — | Pendiente |

**Postcondiciones:** El estudiante queda identificado; no se da por autorizado para cualquier sesión.

**Evidencias:** pendientes. Adjuntar captura o transcripción saneada de cada paso y de la postcondición. No adjuntar credenciales, cabeceras de autorización, códigos reales ni hashes.

### Resultados de la prueba

| Campo | Registro |
| --- | --- |
| Defectos y desviaciones | Pendiente de evaluar; registrar incidencia o «N/A» después de ejecutar |
| Veredicto | No ejecutado |
| Observaciones | Bloqueo conocido: integración aún no implementada. No marcar «Fallado» solo por este bloqueo. |
| Probador | Por asignar |
| Apoyo automatizado existente | PruebaProveedorAutenticacionIdentidad.autenticaEstudianteSoloConCodigoVerificado solo prueba delegación con un doble. El recorrido funcional está bloqueado por falta de convocatorias. |

<a id="cp-id-016"></a>

## CP-ID-016 — CU-01 — Rechazar código ajeno, incorrecto o estudiante no convocado

### Información global del caso de prueba

| Campo | Valor |
| --- | --- |
| Caso de prueba n.º | CP-ID-016 |
| Versión de ejecución | Pendiente de registrar versión y hash realmente evaluados; base prevista: `0.0.1-SNAPSHOT`, `388b7bf` |
| Fecha de ejecución | Pendiente |
| Caso de uso | CU-01; CU-22 como límite de autorización |
| Módulo del sistema | `identity` — identidad |
| Requisitos / reglas | RF-01, RF-20; RN-04 |
| Actor | Estudiante |
| Disponibilidad | Bloqueado por integración pendiente |
| Descripción del caso de prueba | Evaluar el rechazo por credenciales de convocatoria incorrectas mediante el adaptador real futuro. |

### Caso de prueba

**Precondiciones:** Pendiente: convocatorias reales y adaptador. Dos estudiantes A/B y un código emitido para A. Las reglas de sesión concreta se evaluarán en participación. Se aplican las condiciones comunes del [plan](plan-pruebas.md#52-preparación-común-de-la-ejecución).

| Paso | Acción | Datos de entrada — campo y valor | Tipo de escenario | Respuesta esperada de la aplicación | Coincide: sí | Coincide: no | Respuesta del sistema |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | Intentar autenticación con correo de A y código incorrecto. | Correo de A; código distinto del emitido. | Negativo / integración pendiente | HTTP 401; la validación debe fallar por el código, no por ausencia del adaptador. | — | — | Pendiente |
| 2 | Intentar con correo de B y código emitido para A, o con una identidad sin convocatoria. | Correo de B/no convocado; código de A. | Negativo / integración pendiente | HTTP 401; el código no puede identificar a otro estudiante. | — | — | Pendiente |

**Postcondiciones:** No se obtiene identidad autenticada mediante un código ajeno. La autorización de inicio por sesión y el Proxy quedan fuera de esta prueba de identidad.

**Evidencias:** pendientes. Adjuntar captura o transcripción saneada de cada paso y de la postcondición. No adjuntar credenciales, cabeceras de autorización, códigos reales ni hashes.

### Resultados de la prueba

| Campo | Registro |
| --- | --- |
| Defectos y desviaciones | Pendiente de evaluar; registrar incidencia o «N/A» después de ejecutar |
| Veredicto | No ejecutado |
| Observaciones | Bloqueo conocido: integración aún no implementada. No marcar «Fallado» solo por este bloqueo. |
| Probador | Por asignar |
| Apoyo automatizado existente | PruebaProveedorAutenticacionIdentidad.rechazaEstudianteSinConvocatoriaVerificada cubre un doble que rechaza; no sustituye la integración real. |
