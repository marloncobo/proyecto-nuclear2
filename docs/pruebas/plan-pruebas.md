# Plan de pruebas — Psicoapp: identidad y organización académica

**Versión del documento:** 0.1.0
**Fecha de elaboración:** 30-10-2026
**Estado:** propuesta para revisión; ejecución funcional pendiente.
**Base prevista del software:** `0.0.1-SNAPSHOT`, commit `388b7bf`, rama `feat/identity-academic`. Registrar el hash realmente ejecutado en cada ficha.

Este plan adapta la [plantilla «Documento plan de pruebas»](https://docs.google.com/document/d/1zRXhuL-_D1xWvpCxZwF_iMPJmI1W9963/edit) a Markdown. Conserva sus seis secciones y los campos de información global, precondiciones, pasos, datos de entrada, respuesta esperada, coincidencia, respuesta observada, postcondiciones, defectos, veredicto, observaciones y probador. Se eliminan únicamente las repeticiones y celdas combinadas de presentación de la plantilla.

## Contenido

1. [Introducción](#1-introducción)
2. [Alcance](#2-alcance)
3. [Definiciones, siglas y abreviaturas](#3-definiciones-siglas-y-abreviaturas)
4. [Responsables e involucrados](#4-responsables-e-involucrados)
5. [Plan de pruebas](#5-plan-de-pruebas)
6. [Casos de prueba](#6-casos-de-prueba)

## 1. Introducción

Psicoapp es un simulador académico de psicología social. Estas pruebas evalúan los recorridos disponibles de administración de usuarios, roles y permisos y preparación de espacios académicos. El enfoque principal son los casos de uso y sus alternativas de rechazo; las pruebas técnicas existentes se usan como apoyo y trazabilidad.

La evaluación no incluye diagnósticos clínicos ni utiliza datos personales reales. Todos los nombres, correos, claves y códigos de ejemplo son ficticios.

## 2. Alcance

| Módulo implementado | Caso de uso vigente | Alcance evaluado |
| --- | --- | --- |
| `identity` — identidad | CU-02 Gestionar docentes | Alta de docentes, validación de datos, duplicados y restricción al administrador. |
| `identity` — identidad | CU-03 Asignar roles y permisos | Cambio de rol, permiso CREAR_CASOS del rol docente, revocación y rechazos por autorización o invariantes implementadas. |
| `identity` — identidad | CU-01 Autenticarse en la convocatoria | Rechazo seguro sin adaptador; escenarios reales de aceptación y rechazo planeados, bloqueados por la integración de convocatorias. |
| `identity` — identidad | CU-13 Convocar estudiantes inicialmente | Solo registro y reutilización de la identidad estudiantil como preparación. No se prueba una convocatoria real con esta operación. |
| `academic` — organización académica | CU-12 Programar sesión; apoyo de CU-13 | Alta y consulta del catálogo de espacios. La asociación con sesión y estudiantes se documenta como integración pendiente. |

Se definen **24 casos: 16 de identidad y 8 de organización académica**. De ellos, **21 están disponibles para ejecución** y **3 están bloqueados por integraciones pendientes**. Disponible significa que puede prepararse el recorrido con el backend actual; no significa probado ni aprobado.

No hay CU independientes de «Crear espacio» o «Registrar estudiante» en el catálogo vigente. Se identifican como preparación de CU-12/CU-13 y no se les asignan nuevos números oficiales.

Quedan fuera de esta entrega de documentación la implementación de casos, sesiones, convocatorias, Proxy de participación, navegación, calificación, reportes y notificaciones. Los escenarios bloqueados hacen visible la dependencia, sin ampliar el código ni declarar completos RF-01, RF-04 o RF-20.

Referencias de proyecto:

- [Casos de uso](../requisitos/casos-de-uso.md).
- [Requisitos funcionales](../requisitos/requisitos-funcionales.md).
- [Reglas de negocio](../requisitos/reglas-negocio.md).
- [Alcance del MVP](../alcance.md).
- [Modelo ER](../diseño/ModeloER.drawio).
- [API, configuración y límites del backend](../../backend/README.md).

## 3. Definiciones, siglas y abreviaturas

| Término | Significado |
| --- | --- |
| CU / RF / RN | Caso de uso, requisito funcional y regla de negocio del catálogo vigente. |
| CP-ID / CP-AC | Identificador de caso de prueba de identidad / organización académica. |
| Actor | Perfil que inicia el recorrido. Se obtiene de la autenticación; no se sustituye enviando un identificador en el cuerpo JSON. |
| Escenario positivo / negativo / límite | Recorrido válido, rechazo esperado o evaluación en los bordes de un campo. Un rechazo esperado puede aprobar un caso negativo. |
| HTTP Basic | Autenticación con correo y clave del personal; el estudiante utilizará correo y código cuando exista la integración. |
| CSRF | Protección de solicitudes que modifican datos; requiere cookie y cabecera con token vigente. |
| UUID | Identificador de usuario o espacio, representado como CHAR(36) en MySQL. |
| ER | Modelo entidad-relación del proyecto. |
| Doble de prueba | Sustituto controlado de un colaborador usado en JUnit; no demuestra que la integración real esté construida. |
| Disponible / bloqueado | Condición de preparación del caso. Se registra aparte del veredicto. |
| Pasado / Fallado / No ejecutado | Veredicto tras evaluar todos los pasos, detectar alguna desviación o no ejecutar el caso. |
| N/A | No aplica; debe justificarse y no reemplaza un dato aún pendiente. |

## 4. Responsables e involucrados

No se asignan personas sin confirmación del equipo.

| Nombre | Tipo | Rol y responsabilidad |
| --- | --- | --- |
| Por asignar | Responsable | Responsable de pruebas: revisar alcance, asignar probadores y consolidar evidencias y defectos. |
| Por asignar | Responsable | Probador: preparar datos, registrar versión/fecha y ejecutar cada variante sin completar resultados por anticipado. |
| Por asignar | Involucrado | Desarrollador: apoyar el entorno, analizar defectos y ejecutar pruebas automatizadas; no cambiar el resultado esperado para ocultar una desviación. |
| Por asignar | Involucrado | Revisor académico/funcional: validar trazabilidad de los CU y las dependencias pendientes. |

El administrador y el docente de prueba son actores del sistema, no responsables asignados al equipo. En cada ficha se debe identificar quién realizó la ejecución.

## 5. Plan de pruebas

### 5.1 Módulos y objetivos

| Módulo | Objetivo principal | Casos planeados |
| --- | --- | --- |
| Identidad | Evaluar CU-02/CU-03 y los aportes disponibles a CU-01/CU-13. Separar autenticación de autorización y verificar que las operaciones rechazadas no cambien datos. | [CP-ID-001 a CP-ID-016](casos-identidad.md#casos-planeados-y-trazabilidad). |
| Organización académica | Evaluar la preparación del catálogo para CU-12, sus consultas, restricciones de actor y límites del ER. No confundir catálogo con programación. | [CP-AC-001 a CP-AC-008](casos-academicos.md#casos-planeados-y-trazabilidad). |

Los casos negativos deben aislar el motivo de rechazo: por ejemplo, una prueba de rol requiere credenciales y CSRF válidos para que el rechazo no se deba a otro control. Cada variante de datos se ejecuta y registra por separado.

### 5.2 Preparación común de la ejecución

1. Registrar commit del backend, versión, versión de Java/MySQL, URL base y configuración pertinente. No copiar contraseñas al informe.
2. Preparar una **base MySQL de pruebas aislada** conforme al README del backend; nunca vaciar una base de otra persona ni una base de producción para preparar un caso.
3. Configurar `PSICOAPP_BD_URL`, `PSICOAPP_BD_USUARIO`, `PSICOAPP_BD_CLAVE`, `PSICOAPP_CORREO_ADMINISTRADOR`, `PSICOAPP_CLAVE_ADMINISTRADOR` y `PSICOAPP_DOMINIOS_INSTITUCIONALES=universidad.example` en el entorno local.
4. Arrancar el backend. Confirmar que existe el administrador inicial y que la base contiene las tablas `usuario`, `rol`, `permiso`, `rol_permiso` y `espacio_academico`.
5. Usar un cliente HTTP, ya que el frontend aún no está implementado. Todas las rutas de las fichas son relativas a esa URL base. Fuera de localhost, usar HTTPS.
6. Autenticarse mediante HTTP Basic con la cuenta indicada. Para cada POST/PUT, salvo en un caso que compruebe su omisión, consultar primero `GET /api/identidad/csrf`, conservar la cookie y enviar el `token` en la cabecera indicada por `nombreCabecera`. No reutilizar el token después de perder o renovar la cookie.
7. Preparar docentes mediante CP-ID-001 y estudiantes mediante CP-ID-010 cuando lo requiera una ficha. Sustituir los UUID de ejemplo por identificadores realmente obtenidos.
8. Registrar el estado inicial afectado. Usar correos distintos si un caso requiere una cuenta inexistente. Los datos del plan no deben coincidir con identidades reales.
9. Restaurar roles y permisos después de cada prueba que los modifique. La concesión de CREAR_CASOS afecta a **todo el rol docente**, no solo a una cuenta.
10. Las comprobaciones de persistencia se hacen por la API cuando sea posible. Si se necesita una consulta controlada de base de datos, usar acceso autorizado y lecturas; evidenciar únicamente los campos mínimos, sin claves, hashes o códigos.

Para ejecutar solo las pruebas automatizadas existentes:

```powershell
cd backend
.\mvnw.cmd verify
```

En Linux/macOS: `cd backend` y `sh mvnw verify`. Sus informes se generan en `backend/target/surefire-reports/`. Esas pruebas usan dobles de los puertos y no sustituyen los recorridos funcionales con MySQL ni requieren disponer del adaptador real de convocatorias.

### 5.3 Procedimiento y evidencias

1. Copiar la ficha para cada ejecución, conservando su identificador y añadiendo un número de ejecución si se repite.
2. Completar versión, fecha, probador y estado inicial.
3. Ejecutar cada paso y cada variante. Registrar respuesta HTTP, cuerpo observado saneado y cambios relevantes; marcar solo una columna «Coincide».
4. Adjuntar captura o transcripción saneada de cada paso y evidencia de las postcondiciones. La plantilla solicita capturas; al no haber interfaz gráfica, se admite evidencia del cliente HTTP o del informe automatizado que permita verificar el resultado. No se adjuntan capturas ficticias.
5. Registrar defectos y desviaciones con referencia a la ficha, variante y paso. Usar N/A solo si el campo no aplica, no como sinónimo de «sin ejecutar».
6. Asignar el veredicto y restaurar los datos indicados.

Para ejecuciones exclusivamente JUnit, registrar clase/método, comando e informe generado. Si una prueba solo cubre un fragmento del recorrido, no marcar por ello como pasado el caso funcional completo. La columna de apoyo automatizado describe cobertura existente, no un resultado de esta ejecución.

### 5.4 Criterios de evaluación

- **Pasado:** todas las variantes ejecutadas coinciden con lo esperado, se verifican las postcondiciones y hay evidencia.
- **Fallado:** al menos una variante o postcondición difiere de lo esperado. Un HTTP 400/401/403/404/409 esperado no constituye por sí mismo un fallo.
- **No ejecutado:** no se realizó el caso completo. Registrar las variantes observadas y el motivo; no declarar aprobado el resto.
- **Bloqueo:** conservar «No ejecutado» y documentar la dependencia. CP-ID-015, CP-ID-016 y CP-AC-008 requieren adaptadores o módulos todavía ausentes.
- Al cerrar el ciclo, informar cantidades por veredicto y bloqueos. Las cantidades no equivalen a porcentaje de cumplimiento de todos los RF del producto.

## 6. Casos de prueba

| Documento | Fichas | Disponibles | Bloqueadas |
| --- | --- | --- | --- |
| [Identidad](casos-identidad.md) | 16 | 14 | 2 |
| [Organización académica](casos-academicos.md) | 8 | 7 | 1 |
| Total | 24 | 21 | 3 |

Todas las fichas se entregan con versión y fecha de ejecución pendientes, respuesta observada pendiente, columnas de coincidencia sin marcar y veredicto **No ejecutado**. No se han generado evidencias funcionales nuevas para este plan.

Este documento y sus fichas requieren revisión del equipo antes de tratarse como un plan aprobado; no alteran el catálogo oficial de CU/RF/RN.
