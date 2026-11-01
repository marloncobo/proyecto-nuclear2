# Correcciones de la revision tecnica — 01-11-2026

Esta pagina reemplaza las limitaciones historicas de las tres primeras entregas
cuando contradigan el comportamiento descrito aqui. Las ramas originales son
acumulativas; las correcciones se registran como commits adicionales en
feat/ciclo-vida-sesiones, sin reescribir historial ni modificar ramas remotas.

## Casos: edicion, publicacion y archivado

- PUT /api/casos/{id} recibe el mismo cuerpo de creacion y sustituye el contenido
  completo del borrador. Los campos o elementos omitidos no se conservan como
  en un PATCH. El editor debe enviar el agregado deseado completo.
- Se mantiene id y docente propietario. Permite cambiar datos, escenarios,
  preguntas, opciones, explicaciones, orden y rubrica mientras sea BORRADOR.
- Exige docente activo, propietario y permiso CREAR_CASOS. Publicados y
  archivados responden 409. UUID/valores/JSON invalidos responden 400.
- La sustitucion borra hijos en orden de claves foraneas, sin borrar el padre,
  y reinserta el contenido en una transaccion. Cualquier fallo restaura el
  contenido previo. No quedan preguntas o vinculos de rubrica huerfanos.
- POST /api/casos/{id}/archivar exige docente propietario. Admite BORRADOR y
  PUBLICADO; repetir ARCHIVADO no vuelve a escribir. Conserva contenido y
  sesiones existentes; no permite nuevas programaciones.
- Decision academica confirmada por el usuario: para publicar, cada pregunta
  debe tener al menos una opcion correcta y una incorrecta, ambas con texto
  y justificacion. El borrador puede permanecer incompleto.
- Edicion, publicacion, archivado y consulta de caso programable bloquean la
  misma fila con SELECT FOR UPDATE. Las operaciones deben mantener la misma
  transaccion de aplicacion al consumir los contratos. Publicacion no valida
  una instantanea anterior a una edicion confirmada. Programar y archivar se
  serializan; si se programa primero, la sesion historica se conserva.

El API convierte las excepciones de dominio y errores de conversion HTTP a
ProblemDetail 400. Los errores acumulados de publicacion conservan respuesta
422. No se exponen detalles SQL de conflictos de integridad.

## Sesiones y convocatorias iniciales

POST /api/sesiones ahora exige estudiantes: lista de UUID de estudiantes
institucionales existentes. Debe contener al menos uno, sin nulos ni duplicados.
DirectorioIdentidad verifica cuenta activa, rol ESTUDIANTE y dominio institucional.
Si hace falta registrar al alumno, se utiliza primero la operacion existente
POST /api/identidad/estudiantes.

Ejemplo adicional al cuerpo anterior:

```json
{"estudiantes":["uuid-del-estudiante-registrado"]}
```

La sesion y todas sus convocatorias se guardan en una sola transaccion.
CONVOCATORIA utiliza las relaciones del ER y unicidad (id_sesion,id_estudiante).
Cada convocatoria recibe un codigo aleatorio de 32 caracteres hexadecimales;
se almacena solamente su SHA-256 de 64 caracteres en codigo_acceso. Su
entropia aleatoria evita que se comporte como una contrasena humana corta.
No se guarda ni se registra el codigo en claro.

La respuesta de creacion mantiene los datos de sesion y agrega
convocatorias:[{id,idEstudiante,codigoAcceso}]. El codigo se entrega solo
en esa respuesta al docente autenticado. GET y transiciones devuelven
id/idEstudiante, con codigoAcceso=null; nunca devuelven el resumen almacenado.
Aun no existe reenvio o regeneracion automatica del codigo.

VerificadorConvocatorias implementa el puerto existente de identity:
correo institucional/usuario + codigo valido de una convocatoria autorizada
a una sesion EN_CURSO y no vencida. Las contrasenas BCrypt siguen siendo
exclusivas de las cuentas de personal. Las credenciales no dan autorizacion
general a cualquier sesion.

AutorizacionConvocatorias.exigirAcceso valida estudiante, codigo y sesion
concreta bajo bloqueo, estado y ventana inicial de RN-15: desde inicio previsto
hasta la mitad del intervalo programado, ambos limites inclusivos. Su
transaccion es MANDATORY: participation debera consumir el contrato dentro de
la transaccion que crea el intento. Autenticacion por si sola no crea intentos,
no comprueba reintentos y no reemplaza esta autorizacion especifica.
La comprobacion de credenciales no aplica el corte tardio, pues las futuras
reanudaciones no son ingresos iniciales.

## Tiempo y operacion

Decision confirmada: no se permite iniciar antes de inicioPrevisto.
El inicio exacto se permite. Sigue rechazandose cierre manual desde SIN_INICIAR;
el cierre automatico se aplica a ese estado al vencer fin previsto + 24 horas.

El planificador requiere la aplicacion activa y configuracion habilitada.
Tras reinicio el siguiente barrido recupera todos los candidatos vencidos;
revalida cada uno bajo bloqueo. Varias sesiones se cierran en la transaccion
del barrido, ordenadas por id; un fallo revierte el lote y el siguiente barrido
lo reintenta. No se necesita que el servidor estuviera activo al vencer.
Las participaciones siguen denegadas en el dominio al superar el limite,
aunque el planificador aun no haya escrito FINALIZADO.

La URL MySQL predeterminada fija connectionTimeZone=UTC y
forceConnectionTimeZoneToSession=true. Si se proporciona PSICOAPP_BD_URL,
debe conservar esas opciones. El recorrido de integracion comprueba el viaje
09:00-05:00 a 14:00Z y el cierre posterior.

Los textos TEXT admiten hasta 16000 unidades UTF-16 en Java. En UTF-8 su
peor caso no excede 48000 bytes, por debajo de 65535 bytes de TEXT; el
recorrido integrado prueba 16000 caracteres Unicode de tres bytes.
TIMESTAMP mantiene la precision de segundos y el rango documentado.

## Verificaciones

Resultado del 01-11-2026: mvnw verify completo; 133 pruebas descubiertas,
132 aprobadas, cero fallos/errores y una omitida (PruebaFlujoMySql por no
configurar conexion). JAR generado y git diff --check sin incidencias.

PruebaFlujoReal usa Spring Boot, filtros de seguridad, HTTP Basic con claves
reales BCrypt, repositorios JDBC y H2. Solo se sustituye el reloj.
Recorre administrador, docente y estudiante; edita y publica caso; programa
convocatorias, autentica con codigo, impide acceso a soluciones, archiva sin
romper sesiones y recupera multiples cierres tras reconstruir el servicio.

Las pruebas de regresion incluyen sustitucion sin huerfanos, rollback,
propiedad, estados, HTTP 400 y CSRF, opciones correctas/incorrectas, limite
inicial, vinculacion del codigo al alumno/sesion y mitad del horario.

PruebaFlujoMySql ejecuta el mismo recorrido contra MySQL si se configuran:

```powershell
$env:PSICOAPP_PRUEBAS_MYSQL_URL = "jdbc:mysql://127.0.0.1:3306/"
$env:PSICOAPP_PRUEBAS_MYSQL_USUARIO = "usuario-de-pruebas"
$env:PSICOAPP_PRUEBAS_MYSQL_CLAVE = "clave-local"
cd backend
.\mvnw.cmd -Dtest=PruebaFlujoMySql verify
```

La URL solo indica servidor/puerto y termina en /. La prueba crea un esquema
psicoapp_prueba_<UUID> con utf8mb4 y elimina solamente ese esquema generado
al terminar. Requiere credenciales de pruebas con permiso CREATE/DROP DATABASE;
no utiliza una base existente ni cambia datos de desarrollo. Sin esa variable
JUnit omite explicitamente la prueba. Las credenciales no se versionan.

En esta sesion no hay un servidor MySQL confirmado y no se configuran esas
variables: la prueba MySQL queda omitida, no aprobada. La consola restringida
tampoco permite enumerar servicios de Windows; no se certifica su ausencia.

## Limites conservados

No se incorpora aun editor visual (personajes, fondos, objetos, audio o texto
posicionado): la revision pide decidir su alcance antes de ampliar el modelo.
No se inventan estas entidades sin especificacion aprobada.

Faltan envio por correo (CU-14), cambios/retiros de convocados (CU-34),
participation y evaluation: intentos, respuestas, reanudacion, reintentos,
notas y ausencias. Por tanto CU-12/CU-13 quedan integrados para persistencia
y credenciales iniciales, pero no se declaran completos con su notificacion;
CU-16/CU-28 aun no incluyen CU-35. El Google Sheets anterior es una fotografia
de 72e2d14 y sus pronosticos no representan estas nuevas correcciones.
