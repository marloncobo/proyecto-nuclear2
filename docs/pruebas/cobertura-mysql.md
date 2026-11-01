# Cobertura de integración con MySQL

**Fecha:** 01-11-2026. **Backend:** 0.0.1-SNAPSHOT.
**Base de código:** `37070ef` más los cambios locales de la rama
`test/pruebas-identidad-academica`. **Entorno:** MySQL 9.3.0, Java 24.0.1.

`PruebaPersistenciaMysqlIT` utiliza el contexto completo de Spring Boot, MockMvc,
autenticación HTTP Basic, cookie y token CSRF reales, servicios transaccionales
y los tres adaptadores JDBC del proyecto. No sustituye repositorios ni permisos.
Cada ejecución crea un esquema MySQL aleatorio y comprueba que lo elimina al
terminar. Las credenciales provienen del entorno; no se guardan en el proyecto.

## Casos y evidencia automatizada

| Casos | Métodos de PruebaPersistenciaMysqlIT | Comprobación |
| --- | --- | --- |
| CP-ID-001/002 | registraDocenteYRechazaDuplicadosEnMysql; restriccionUniqueProtegeCorreo | BCrypt, autenticación, correo normalizado, HTTP 409 y UNIQUE real incluso sin pasar por el servicio. |
| CP-ID-003 | datosInvalidosNoPersisten | Cinco variantes HTTP independientes; no se modifican los registros. |
| CP-ID-004/013 | altasNoAutorizadasNoPersisten | Rol docente y ausencia de CSRF no escriben cuentas. |
| CP-ID-005/006 | cambiosDeRolPersisten | Promoción y restitución verificadas en SQL y con autenticación posterior; rechazo del cambio de rol propio. |
| CP-ID-007/008 | permisosPersistenParaDosDocentes | Concesión y revocación idempotentes en rol_permiso, dos identidades y rechazos de rol. |
| CP-ID-009 | registraDocenteYRechazaDuplicadosEnMysql y autenticación de cada flujo | Las credenciales se validan contra usuarios y hashes almacenados en MySQL. Las variantes de contraseña incorrecta y anonimato también están en PruebaFlujosIdentidadAcademica. |
| CP-ID-010/014 | reutilizaEstudiantePersistido | HTTP 201, mismo UUID con mayúsculas, contraseña nula y acceso denegado sin convocatorias. |
| CP-ID-011 | rechazaCorreoEstudianteSinPersistir | Dominio externo, sufijo engañoso y correo del personal conservan las cuentas. |
| CP-ID-012 | promocionInvalidaNoPersiste | Las dos promociones inválidas conservan el registro de estudiante. |
| CP-AC-001/003/006 | espaciosPersistenConSusLimites | Lectura del espacio creado, longitudes persistidas 150/30, rechazos 151/31 y HTTP 404 sin cambios. |
| CP-AC-002 | espaciosInvalidosNoPersisten | Seis variantes de campo vacío, blanco u omitido conservan el catálogo previo. |
| CP-AC-004/005 | catalogoCompartidoRespetaOrdenMysql | Catálogo vacío y compartido entre dos docentes; orden por periodo, nombre e UUID, incluido desempate. |
| CP-AC-007 | accesoNoAutorizadoNoAlteraCatalogo | Consultas y alta de administrador rechazadas; anónimo recibe 401; catálogo intacto. |
| CP-AC-001/CP-ID-007 | datosSobrevivenReinicioDelBackend | Cierra un contexto completo del backend y arranca otro: conserva espacio y permiso. El arranque vuelve a aplicar schema.sql sin duplicar los catálogos. |
| Persistencia transversal | falloTransaccionalRevierteEscrituras | Un fallo controlado revierte el alta docente y la concesión del permiso dentro de la misma transacción. |

## Ejecución

Comando: `.\mvnw.cmd -Ppruebas-mysql verify`, desde `backend/`, con las variables
descritas en el [README](../../backend/README.md#integración-con-mysql-real).

Resultado: **87 pruebas de la suite habitual y 27 de integración con MySQL:
114 ejecuciones, 0 fallos, 0 errores y 0 omitidas**.

Los informes locales se generan en `backend/target/surefire-reports/` y
`backend/target/failsafe-reports/`. El archivo
`com.psicoapp.identity.api.PruebaPersistenciaMysqlIT.txt` contiene el resumen
de integración y su XML contiene los resultados individuales. Los archivos
generados no se versionan.

## Límites

CP-ID-015, CP-ID-016 y CP-AC-008 continúan bloqueados porque las convocatorias
y la programación no están implementadas en esta base de código. Tener MySQL
disponible no completa esas integraciones.

Las fichas conservan el veredicto funcional pendiente: esta evidencia automatizada
no sustituye la asignación de probador, las capturas y la cumplimentación de todos
los campos del procedimiento manual. El reinicio comprobado es del backend;
no se detiene el servicio MySQL del equipo.
