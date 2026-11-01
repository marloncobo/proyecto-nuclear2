# Casos de prueba — organización académica

**Proyecto:** Psicoapp · **Versión del documento:** 0.1.0 · **Fecha de elaboración:** 30-10-2026.
**Estado:** planeado, pendiente de revisión y ejecución. No representa un acta de resultados.

El objetivo es evaluar el catálogo de espacios que **apoya CU-12 Programar sesión y CU-13 Convocar estudiantes inicialmente**. El catálogo no tiene un CU independiente en la matriz vigente; crear o consultar espacios no prueba la programación completa. No se inventan grupos, matrículas ni propietarios de espacios.

Este archivo desarrolla las fichas de la sección 6 del [plan de pruebas](plan-pruebas.md), adaptado de la [plantilla de referencia](https://docs.google.com/document/d/1zRXhuL-_D1xWvpCxZwF_iMPJmI1W9963/edit). Los CU, RF y RN proceden del [catálogo vigente](../requisitos/casos-de-uso.md); «preparación» y «soporte» identifican aportes parciales, sin crear nuevos CU oficiales.

## Casos planeados y trazabilidad

| Identificador | Caso de prueba | CU relacionado | Disponibilidad |
| --- | --- | --- | --- |
| [CP-AC-001](#cp-ac-001) | Preparación de CU-12 — Crear espacio académico | CU-12 (preparación del catálogo; programación pendiente) | Disponible |
| [CP-AC-002](#cp-ac-002) | Preparación de CU-12 — Rechazar espacio incompleto | CU-12 (preparación del catálogo) | Disponible |
| [CP-AC-003](#cp-ac-003) | Preparación de CU-12 — Evaluar longitudes límite del espacio | CU-12 (preparación del catálogo) | Disponible |
| [CP-AC-004](#cp-ac-004) | Preparación de CU-12 — Consultar catálogo vacío | CU-12 (consulta preparatoria) | Disponible |
| [CP-AC-005](#cp-ac-005) | Preparación de CU-12 — Listar espacios disponibles | CU-12 (consulta preparatoria) | Disponible |
| [CP-AC-006](#cp-ac-006) | Preparación de CU-12 — Consultar espacio y rechazar UUID inexistente | CU-12 (consulta preparatoria) | Disponible |
| [CP-AC-007](#cp-ac-007) | Preparación de CU-12 — Restringir la administración académica a docentes | CU-12 (precondición de actor) | Disponible |
| [CP-AC-008](#cp-ac-008) | CU-12/CU-13 — Asociar espacio y estudiantes al programar | CU-12, CU-13 | Bloqueado |

Los enlaces de cada identificador usan anclas explícitas. En las fichas, «—» significa sin evaluar; el probador marca una sola columna de coincidencia después de observar el resultado.

<a id="cp-ac-001"></a>

## CP-AC-001 — Preparación de CU-12 — Crear espacio académico

### Información global del caso de prueba

| Campo | Valor |
| --- | --- |
| Caso de prueba n.º | CP-AC-001 |
| Versión de ejecución | Pendiente de registrar versión y hash realmente evaluados; base prevista: `0.0.1-SNAPSHOT`, `388b7bf` |
| Fecha de ejecución | Pendiente |
| Caso de uso | CU-12 (preparación del catálogo; programación pendiente) |
| Módulo del sistema | `academic` — organización académica |
| Requisitos / reglas | RF-04; No hay RN exclusiva del catálogo; límites del ER. |
| Actor | Docente |
| Disponibilidad | Disponible para ejecutar |
| Descripción del caso de prueba | Registrar nombre y periodo del espacio que una programación futura podrá referenciar. |

### Caso de prueba

**Precondiciones:** Docente activo autenticado; preparación común completada. Se aplican las condiciones comunes del [plan](plan-pruebas.md#52-preparación-común-de-la-ejecución).

| Paso | Acción | Datos de entrada — campo y valor | Tipo de escenario | Respuesta esperada de la aplicación | Coincide: sí | Coincide: no | Respuesta del sistema |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | Enviar POST /api/academico/espacios con CSRF vigente. | nombre=" Psicología social "; periodo=" 2026-2 " | Positivo | HTTP 201; devuelve id, nombre=Psicología social y periodo=2026-2 sin espacios externos; no devuelve campos name o period. | — | — | Pendiente |
| 2 | Consultar GET /api/academico/espacios/{id} con el UUID recibido. | UUID del espacio creado. | Positivo | HTTP 200 con los mismos datos. | — | — | Pendiente |
| 3 | Reiniciar únicamente la aplicación usando la misma base de pruebas y consultar de nuevo el UUID. | Misma configuración MySQL y credenciales docentes. | Persistencia | HTTP 200; el espacio se conserva. | — | — | Pendiente |

**Postcondiciones:** Existe el espacio en espacio_academico; no existe una sesión creada como efecto del alta.

**Evidencias:** pendientes. Adjuntar captura o transcripción saneada de cada paso y de la postcondición. No adjuntar credenciales, cabeceras de autorización, códigos reales ni hashes.

### Resultados de la prueba

| Campo | Registro |
| --- | --- |
| Defectos y desviaciones | Pendiente de evaluar; registrar incidencia o «N/A» después de ejecutar |
| Veredicto | No ejecutado |
| Observaciones | Registrar resultados de cada variante; si alguna falla, el caso no puede declararse «Pasado». |
| Probador | Por asignar |
| Apoyo automatizado existente | PruebaFachadaAdministracion.creaEspacioDespuesDeAutorizarDocente; consultaEspacioExistente; PruebaSeguridadIdentidad.docenteCreaYConsultaEspacioConCamposEnEspanol. El reinicio y MySQL se verifican manualmente. |

<a id="cp-ac-002"></a>

## CP-AC-002 — Preparación de CU-12 — Rechazar espacio incompleto

### Información global del caso de prueba

| Campo | Valor |
| --- | --- |
| Caso de prueba n.º | CP-AC-002 |
| Versión de ejecución | Pendiente de registrar versión y hash realmente evaluados; base prevista: `0.0.1-SNAPSHOT`, `388b7bf` |
| Fecha de ejecución | Pendiente |
| Caso de uso | CU-12 (preparación del catálogo) |
| Módulo del sistema | `academic` — organización académica |
| Requisitos / reglas | RF-04; Restricciones de datos del ER. |
| Actor | Docente |
| Disponibilidad | Disponible para ejecutar |
| Descripción del caso de prueba | Impedir espacios sin nombre o periodo. |

### Caso de prueba

**Precondiciones:** Docente activo; registrar el listado de espacios previo. Se aplican las condiciones comunes del [plan](plan-pruebas.md#52-preparación-común-de-la-ejecución).

| Paso | Acción | Datos de entrada — campo y valor | Tipo de escenario | Respuesta esperada de la aplicación | Coincide: sí | Coincide: no | Respuesta del sistema |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | Repetir POST /api/academico/espacios con cada variante, conservando válido el otro campo. | nombre="" o " "; periodo="" o " "; omitir uno de los campos en otra ejecución | Negativo / datos obligatorios | HTTP 400 en todas las variantes. | — | — | Pendiente |
| 2 | Consultar GET /api/academico/espacios. | Credenciales del docente. | Comprobación | El conjunto de espacios coincide con el previo; no hay registros parciales. | — | — | Pendiente |

**Postcondiciones:** No se guarda ningún espacio inválido.

**Evidencias:** pendientes. Adjuntar captura o transcripción saneada de cada paso y de la postcondición. No adjuntar credenciales, cabeceras de autorización, códigos reales ni hashes.

### Resultados de la prueba

| Campo | Registro |
| --- | --- |
| Defectos y desviaciones | Pendiente de evaluar; registrar incidencia o «N/A» después de ejecutar |
| Veredicto | No ejecutado |
| Observaciones | Registrar resultados de cada variante; si alguna falla, el caso no puede declararse «Pasado». |
| Probador | Por asignar |
| Apoyo automatizado existente | PruebaEspacioAcademico.exigeNombre; exigePeriodo; PruebaFachadaAdministracion.rechazaEspacioAcademicoIncompleto. Las variantes HTTP son complementarias. |

<a id="cp-ac-003"></a>

## CP-AC-003 — Preparación de CU-12 — Evaluar longitudes límite del espacio

### Información global del caso de prueba

| Campo | Valor |
| --- | --- |
| Caso de prueba n.º | CP-AC-003 |
| Versión de ejecución | Pendiente de registrar versión y hash realmente evaluados; base prevista: `0.0.1-SNAPSHOT`, `388b7bf` |
| Fecha de ejecución | Pendiente |
| Caso de uso | CU-12 (preparación del catálogo) |
| Módulo del sistema | `academic` — organización académica |
| Requisitos / reglas | RF-04; ER: nombre VARCHAR(150), periodo VARCHAR(30). |
| Actor | Docente |
| Disponibilidad | Disponible para ejecutar |
| Descripción del caso de prueba | Comprobar aceptación del máximo y rechazo del exceso de longitud, con caracteres ASCII para aislar la longitud. |

### Caso de prueba

**Precondiciones:** Docente activo; generar cadenas sin espacios externos. Se aplican las condiciones comunes del [plan](plan-pruebas.md#52-preparación-común-de-la-ejecución).

| Paso | Acción | Datos de entrada — campo y valor | Tipo de escenario | Respuesta esperada de la aplicación | Coincide: sí | Coincide: no | Respuesta del sistema |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | Crear un espacio usando los máximos admitidos. | nombre=150 letras A; periodo=30 letras B. | Positivo / límite | HTTP 201; conserva las longitudes 150 y 30. | — | — | Pendiente |
| 2 | Repetir con nombre de 151 caracteres; luego con periodo de 31 y nombre válido. | nombre=151 letras A; en otra ejecución periodo=31 letras B. | Negativo / límite | HTTP 400 en ambas ejecuciones; no se guardan los registros rechazados. | — | — | Pendiente |

**Postcondiciones:** Solo existe el espacio del escenario positivo.

**Evidencias:** pendientes. Adjuntar captura o transcripción saneada de cada paso y de la postcondición. No adjuntar credenciales, cabeceras de autorización, códigos reales ni hashes.

### Resultados de la prueba

| Campo | Registro |
| --- | --- |
| Defectos y desviaciones | Pendiente de evaluar; registrar incidencia o «N/A» después de ejecutar |
| Veredicto | No ejecutado |
| Observaciones | Registrar resultados de cada variante; si alguna falla, el caso no puede declararse «Pasado». |
| Probador | Por asignar |
| Apoyo automatizado existente | PruebaEspacioAcademico.limitaLongitudDelNombre y limitaLongitudDelPeriodo cubren el exceso. La aceptación exacta de máximos queda como prueba manual planeada. |

<a id="cp-ac-004"></a>

## CP-AC-004 — Preparación de CU-12 — Consultar catálogo vacío

### Información global del caso de prueba

| Campo | Valor |
| --- | --- |
| Caso de prueba n.º | CP-AC-004 |
| Versión de ejecución | Pendiente de registrar versión y hash realmente evaluados; base prevista: `0.0.1-SNAPSHOT`, `388b7bf` |
| Fecha de ejecución | Pendiente |
| Caso de uso | CU-12 (consulta preparatoria) |
| Módulo del sistema | `academic` — organización académica |
| Requisitos / reglas | RF-04; No aplica una RN adicional. |
| Actor | Docente |
| Disponibilidad | Disponible para ejecutar |
| Descripción del caso de prueba | Consultar un catálogo sin espacios sin confundirlo con un error. |

### Caso de prueba

**Precondiciones:** Base de pruebas aislada con un docente válido y sin registros en espacio_academico. No eliminar datos de otras ejecuciones ni de producción. Se aplican las condiciones comunes del [plan](plan-pruebas.md#52-preparación-común-de-la-ejecución).

| Paso | Acción | Datos de entrada — campo y valor | Tipo de escenario | Respuesta esperada de la aplicación | Coincide: sí | Coincide: no | Respuesta del sistema |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | Consultar GET /api/academico/espacios. | Credenciales del docente; sin filtros. | Positivo / conjunto vacío | HTTP 200 y arreglo JSON []. | — | — | Pendiente |

**Postcondiciones:** La consulta no crea datos.

**Evidencias:** pendientes. Adjuntar captura o transcripción saneada de cada paso y de la postcondición. No adjuntar credenciales, cabeceras de autorización, códigos reales ni hashes.

### Resultados de la prueba

| Campo | Registro |
| --- | --- |
| Defectos y desviaciones | Pendiente de evaluar; registrar incidencia o «N/A» después de ejecutar |
| Veredicto | No ejecutado |
| Observaciones | Registrar resultados de cada variante; si alguna falla, el caso no puede declararse «Pasado». |
| Probador | Por asignar |
| Apoyo automatizado existente | No hay una prueba dedicada al catálogo vacío; la fachada usa el listado del repositorio. Caso funcional manual planeado. |

<a id="cp-ac-005"></a>

## CP-AC-005 — Preparación de CU-12 — Listar espacios disponibles

### Información global del caso de prueba

| Campo | Valor |
| --- | --- |
| Caso de prueba n.º | CP-AC-005 |
| Versión de ejecución | Pendiente de registrar versión y hash realmente evaluados; base prevista: `0.0.1-SNAPSHOT`, `388b7bf` |
| Fecha de ejecución | Pendiente |
| Caso de uso | CU-12 (consulta preparatoria) |
| Módulo del sistema | `academic` — organización académica |
| Requisitos / reglas | RF-04; No aplica una RN adicional; orden del adaptador actual. |
| Actor | Docente |
| Disponibilidad | Disponible para ejecutar |
| Descripción del caso de prueba | Obtener el catálogo persistido para preparar una programación. |

### Caso de prueba

**Precondiciones:** Base de pruebas con espacios conocidos en periodos 2026-1 y 2026-2. Registrar sus UUID. Dos docentes activos. Se aplican las condiciones comunes del [plan](plan-pruebas.md#52-preparación-común-de-la-ejecución).

| Paso | Acción | Datos de entrada — campo y valor | Tipo de escenario | Respuesta esperada de la aplicación | Coincide: sí | Coincide: no | Respuesta del sistema |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | Consultar GET /api/academico/espacios con el primer docente. | Espacios preparados: periodo 2026-2/nombre B; 2026-1/nombre Z; 2026-2/nombre A. | Positivo | HTTP 200; incluye los UUID preparados. El adaptador actual ordena por periodo, nombre e id, conforme a la intercalación MySQL. | — | — | Pendiente |
| 2 | Repetir con el segundo docente. | Credenciales de otro docente activo. | Positivo / catálogo compartido | Obtiene el mismo catálogo. El ER actual no asigna propietario a espacio_academico. | — | — | Pendiente |

**Postcondiciones:** No cambian datos ni aparecen membresías de estudiantes; no se evalúan grupos inexistentes.

**Evidencias:** pendientes. Adjuntar captura o transcripción saneada de cada paso y de la postcondición. No adjuntar credenciales, cabeceras de autorización, códigos reales ni hashes.

### Resultados de la prueba

| Campo | Registro |
| --- | --- |
| Defectos y desviaciones | Pendiente de evaluar; registrar incidencia o «N/A» después de ejecutar |
| Veredicto | No ejecutado |
| Observaciones | Registrar resultados de cada variante; si alguna falla, el caso no puede declararse «Pasado». |
| Probador | Por asignar |
| Apoyo automatizado existente | PruebaFachadaAdministracion.listaEspaciosAcademicos valida la delegación. El orden SQL y el acceso de dos docentes son comprobaciones manuales. |

<a id="cp-ac-006"></a>

## CP-AC-006 — Preparación de CU-12 — Consultar espacio y rechazar UUID inexistente

### Información global del caso de prueba

| Campo | Valor |
| --- | --- |
| Caso de prueba n.º | CP-AC-006 |
| Versión de ejecución | Pendiente de registrar versión y hash realmente evaluados; base prevista: `0.0.1-SNAPSHOT`, `388b7bf` |
| Fecha de ejecución | Pendiente |
| Caso de uso | CU-12 (consulta preparatoria) |
| Módulo del sistema | `academic` — organización académica |
| Requisitos / reglas | RF-04; No aplica una RN adicional. |
| Actor | Docente |
| Disponibilidad | Disponible para ejecutar |
| Descripción del caso de prueba | Distinguir un espacio existente de uno que no se encuentra. |

### Caso de prueba

**Precondiciones:** Docente activo; un espacio conocido y un UUID válido que se haya comprobado ausente en la base de pruebas. Se aplican las condiciones comunes del [plan](plan-pruebas.md#52-preparación-común-de-la-ejecución).

| Paso | Acción | Datos de entrada — campo y valor | Tipo de escenario | Respuesta esperada de la aplicación | Coincide: sí | Coincide: no | Respuesta del sistema |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | Consultar GET /api/academico/espacios/{id} para el espacio existente. | UUID persistido. | Positivo | HTTP 200 con el UUID, nombre y periodo del registro solicitado. | — | — | Pendiente |
| 2 | Repetir con el UUID ausente. | UUID con formato válido y sin registro. | Negativo / inexistencia | HTTP 404; no se crea un espacio de reemplazo. | — | — | Pendiente |

**Postcondiciones:** La base no cambia.

**Evidencias:** pendientes. Adjuntar captura o transcripción saneada de cada paso y de la postcondición. No adjuntar credenciales, cabeceras de autorización, códigos reales ni hashes.

### Resultados de la prueba

| Campo | Registro |
| --- | --- |
| Defectos y desviaciones | Pendiente de evaluar; registrar incidencia o «N/A» después de ejecutar |
| Veredicto | No ejecutado |
| Observaciones | Registrar resultados de cada variante; si alguna falla, el caso no puede declararse «Pasado». |
| Probador | Por asignar |
| Apoyo automatizado existente | PruebaFachadaAdministracion.consultaEspacioExistente e informaEspacioInexistente; PruebaSeguridadIdentidad.docenteCreaYConsultaEspacioConCamposEnEspanol cubre la lectura HTTP positiva. |

<a id="cp-ac-007"></a>

## CP-AC-007 — Preparación de CU-12 — Restringir la administración académica a docentes

### Información global del caso de prueba

| Campo | Valor |
| --- | --- |
| Caso de prueba n.º | CP-AC-007 |
| Versión de ejecución | Pendiente de registrar versión y hash realmente evaluados; base prevista: `0.0.1-SNAPSHOT`, `388b7bf` |
| Fecha de ejecución | Pendiente |
| Caso de uso | CU-12 (precondición de actor) |
| Módulo del sistema | `academic` — organización académica |
| Requisitos / reglas | RF-02, RF-04; No aplica una RN adicional del catálogo. |
| Actor | Administrador y usuario anónimo en escenarios independientes |
| Disponibilidad | Disponible para ejecutar |
| Descripción del caso de prueba | Impedir que actores distintos del docente usen el catálogo por la API actual. |

### Caso de prueba

**Precondiciones:** Administrador activo; un espacio existente; para POST del administrador usar CSRF válido para aislar el control de rol. Se aplican las condiciones comunes del [plan](plan-pruebas.md#52-preparación-común-de-la-ejecución).

| Paso | Acción | Datos de entrada — campo y valor | Tipo de escenario | Respuesta esperada de la aplicación | Coincide: sí | Coincide: no | Respuesta del sistema |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | Consultar el listado y un espacio; intentar crear otro espacio como administrador. | GET listado; GET UUID existente; POST con nombre=No autorizado y periodo=2026-2. | Negativo / rol | HTTP 403 en las tres solicitudes; no se crea el espacio. | — | — | Pendiente |
| 2 | Consultar GET /api/academico/espacios sin autenticación. | Sin cabecera Authorization. | Negativo / autenticación | HTTP 401. | — | — | Pendiente |

**Postcondiciones:** No cambian los espacios. La variante ESTUDIANTE solo puede probarse en JUnit con una identidad controlada o funcionalmente cuando exista el adaptador de convocatorias; no se debe inventar una contraseña estudiantil.

**Evidencias:** pendientes. Adjuntar captura o transcripción saneada de cada paso y de la postcondición. No adjuntar credenciales, cabeceras de autorización, códigos reales ni hashes.

### Resultados de la prueba

| Campo | Registro |
| --- | --- |
| Defectos y desviaciones | Pendiente de evaluar; registrar incidencia o «N/A» después de ejecutar |
| Veredicto | No ejecutado |
| Observaciones | Registrar resultados de cada variante; si alguna falla, el caso no puede declararse «Pasado». |
| Probador | Por asignar |
| Apoyo automatizado existente | PruebaFachadaAdministracion.rechazaUsuarioSinPermisosAntesDeConsultarPersistencia; PruebaSeguridadIdentidad.estudianteNoAdministraEspaciosAcademicos. Las variantes de administrador y anónimo del catálogo son manuales planeadas. |

<a id="cp-ac-008"></a>

## CP-AC-008 — CU-12/CU-13 — Asociar espacio y estudiantes al programar

### Información global del caso de prueba

| Campo | Valor |
| --- | --- |
| Caso de prueba n.º | CP-AC-008 |
| Versión de ejecución | Pendiente de registrar versión y hash realmente evaluados; base prevista: `0.0.1-SNAPSHOT`, `388b7bf` |
| Fecha de ejecución | Pendiente |
| Caso de uso | CU-12, CU-13 |
| Módulo del sistema | `academic` — organización académica |
| Requisitos / reglas | RF-04, RF-18, RF-20; RN-03, RN-04, RN-13 |
| Actor | Docente |
| Disponibilidad | Bloqueado por integración pendiente |
| Descripción del caso de prueba | Validar el caso de uso completo al que contribuye academic, sin declarar que el catálogo por sí solo lo cumple. |

### Caso de prueba

**Precondiciones:** Pendiente: casos publicados, programación de sesiones y convocatorias reales. Docente autorizado, espacio existente, estudiantes registrados y configuración temporal válida según RN-13. Se aplican las condiciones comunes del [plan](plan-pruebas.md#52-preparación-común-de-la-ejecución).

| Paso | Acción | Datos de entrada — campo y valor | Tipo de escenario | Respuesta esperada de la aplicación | Coincide: sí | Coincide: no | Respuesta del sistema |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | Programar una sesión con un caso PUBLICADO, el espacio académico y al menos un estudiante. | Identificadores reales de caso, espacio y estudiante; fechas y duración válidas. | Positivo / integración pendiente | La sesión conserva la referencia al espacio y al docente; cada estudiante queda vinculado a una convocatoria autorizada. El endpoint y su respuesta HTTP se concretarán al implementar sessions. | — | — | Pendiente |
| 2 | Consultar la sesión y comprobar las relaciones guardadas mediante los contratos del módulo propietario. | Identificador de la sesión creada. | Comprobación / integración pendiente | Se recuperan espacio y estudiantes correctos; consultar un espacio por sí solo no constituye esa asociación. | — | — | Pendiente |

**Postcondiciones:** Existe una sesión programada y sus convocatorias independientes del catálogo de espacios. No se requiere implementar estos módulos para redactar el caso.

**Evidencias:** pendientes. Adjuntar captura o transcripción saneada de cada paso y de la postcondición. No adjuntar credenciales, cabeceras de autorización, códigos reales ni hashes.

### Resultados de la prueba

| Campo | Registro |
| --- | --- |
| Defectos y desviaciones | Pendiente de evaluar; registrar incidencia o «N/A» después de ejecutar |
| Veredicto | No ejecutado |
| Observaciones | Bloqueo conocido: integración aún no implementada. No marcar «Fallado» solo por este bloqueo. |
| Probador | Por asignar |
| Apoyo automatizado existente | No hay prueba integral equivalente. PruebaFachadaAdministracion solo cubre el catálogo. Bloqueado hasta implementar sessions/cases y convocatorias. |
