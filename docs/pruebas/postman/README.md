# Pruebas de Postman

Importar `Psicoapp.postman_collection.json` y `Psicoapp-local.postman_environment.json` en Postman. Seleccionar el entorno **Psicoapp - local**, completar `adminPassword` con la contraseña local y ejecutar la colección completa en **Runner**, con una iteración y las cookies habilitadas.

El entorno apunta a `http://localhost:8081`, instancia aislada usada para esta validación con MySQL y la base `psicoapp_postman_20261108`. El administrador de pruebas es `admin@unihumboldt.cue.co` y el dominio institucional configurado es `universidad.edu.co`.

Cada ejecución genera correos ficticios y UUID nuevos. Crea dos docentes (responsable y ajeno), dos estudiantes, espacio, caso, sesión, convocatorias e intento; habilita `CREAR_CASOS` para el rol docente; termina la participación, consulta su evaluación, registra una valoración desde reportes, comprueba notificaciones internas, cierra la sesión y archiva el caso. Los registros se conservan para inspeccionarlos en MySQL Workbench. No ejecuta limpieza ni elimina registros. Si una respuesta tiene un estado HTTP inesperado, el Runner se detiene y puede dejar entidades sin cerrar. Los rechazos esperados 400/401/403/404/409 cuentan como pruebas correctas cuando coinciden con el escenario.

La colección obtiene CSRF con las credenciales de cada actor, conserva cookies, captura los IDs y usa el código de convocatoria como contraseña estudiantil. Las fechas se generan al programar, con precisión de segundos y una ventana que permite iniciar inmediatamente. La evaluación usa al estudiante propietario, de acuerdo con la seguridad vigente. Avanzar y retroceder comprueban los límites de un caso de una sola pregunta; no cubren navegación entre múltiples preguntas.

Se corrigieron el JSON inválido de actualizar caso (signos `+` pegados), los textos con codificación incorrecta, el código de acceso fijo y el orden de archivado/cierre. El archivo original no contenía aserciones; esta copia agrega comprobaciones de HTTP, JSON, IDs, estados, respuesta guardada y nota máxima de 5.

`resultado-local.json` contiene el informe de Newman sin contraseñas, cookies ni códigos de convocatoria. Las estadísticas de peticiones incluyen las consultas CSRF auxiliares; `resultados` enumera las 70 peticiones principales.

## Cobertura de reportes

Las 18 peticiones añadidas comprueban las dos rutas del módulo:

- `GET /api/reportes/casos/{idCaso}/estudiantes/{idEstudiante}`.
- `POST /api/reportes/participaciones/{idIntento}/valoraciones`.

| Escenario | Resultado verificado |
| --- | --- |
| Intento activo | Historial vacío (200); valoración rechazada (409). |
| Intento finalizado | Historial con estudiante, sesión, respuestas, nota 5 y retroalimentación correctos. |
| Valoración docente | Alta 201 e historial con el ID, autor, mensaje y fecha; se conserva una sola valoración. |
| Mensaje vacío o mayor de 4000 caracteres | Rechazo 400. |
| POST sin cabecera CSRF, conservando cookies | Rechazo 403. |
| Estudiante o docente ajeno | Consulta y escritura denegadas con 403. |
| Administrador | Consulta denegada con 403. |
| Caso inexistente o UUID inválido | Rechazos 404 y 400, respectivamente. |
| Consultas repetidas y escrituras rechazadas | Se conserva el historial con una única valoración. |
| Caso archivado | El historial continúa disponible y conserva la valoración. |

Validación previa de reportes del **9 de noviembre de 2026** con Newman 6.2.3 y MySQL real: **45 peticiones principales, 114 verificaciones, 0 fallos**. La secuencia original de 27 peticiones también se ejecuta en esta misma colección. No se modificó código del backend para estas pruebas. El alcance no incluye exportación de reportes, ausencias ni múltiples intentos.

## Cobertura de notificaciones

Se añaden 25 peticiones que verifican `GET /api/notificaciones` y `PUT /api/notificaciones/{id}/leida`, además de preparar una segunda cuenta estudiantil con su propio código de convocatoria.

| Escenario | Resultado verificado |
| --- | --- |
| Antes de finalizar el intento | Bandeja interna vacía. |
| Finalización y reevaluación idempotente | Un solo aviso `RESULTADO` con intento, sesión y nota correctos. |
| Valoración desde reportes y consultas repetidas | Exactamente un aviso `RESULTADO` y uno `VALORACION`, inicialmente sin leer, sin duplicados por consultas o escrituras rechazadas. |
| Paginación | Dos páginas de un aviso, sin repeticiones; página posterior vacía. |
| Parámetros de paginación inválidos | Límite 0, límite 101 y desplazamiento negativo devuelven 400. |
| Marcar resultado leído dos veces | Ambas escrituras devuelven 204; la lectura persiste y el aviso de valoración continúa sin leer. |
| Marcar sin CSRF | Rechazo 403 sin cambiar el aviso. |
| Consulta sin autenticación | Rechazo 401. |
| Docente o administrador | Consulta denegada con 403; el docente tampoco puede marcar un aviso. |
| Aviso inexistente o UUID inválido | Rechazos 404 y 400, respectivamente. |
| Segundo estudiante convocado | Bandeja propia vacía; marcar un aviso ajeno devuelve 404 y no altera el estado del propietario. |
| Sesión cerrada y caso archivado | El propietario conserva acceso a sus avisos y su estado de lectura. |

La entrega de convocatorias por correo, reintentos SMTP y cifrado de los mensajes requieren una validación independiente con un servidor de correo de pruebas. En este recorrido el despachador está habilitado para procesar los avisos internos, pero SMTP apunta a `127.0.0.1:1`, sin servicio de correo; no se entregan mensajes externos. Desactivar `PSICOAPP_NOTIFICACIONES_DESPACHO` impide también la entrega de avisos internos y hace fallar estas pruebas.

La colección espera la entrega asíncrona antes de comprobar el resultado y la valoración: consulta cada 250 ms durante un máximo de `notificationWaitMs` (10000 ms en el entorno adjunto). Si se utiliza el intervalo de despacho habitual de 30000 ms, aumentar `notificationWaitMs`, por ejemplo a 60000 ms. La instancia de pruebas documentada abajo despacha cada 250 ms.

Validación completa del **9 de noviembre de 2026** con Newman 6.2.3 y MySQL real: **70 peticiones principales, 167 verificaciones, 0 fallos**. Incluye las pruebas anteriores de reportes y el flujo base; no se modificó código del backend.

## Ejecución

Para repetir por consola, desde la raíz, con Node.js instalado y la instancia de pruebas encendida:

```powershell
npx.cmd --yes newman run docs/pruebas/postman/Psicoapp.postman_collection.json -e docs/pruebas/postman/Psicoapp-local.postman_environment.json --env-var "adminPassword=TU_CLAVE_LOCAL" --bail
```

Para iniciar de nuevo la instancia de pruebas, desde `backend`, configurar las variables locales `PSICOAPP_BD_USUARIO` y `PSICOAPP_BD_CLAVE` antes de estos comandos. La base debe existir; el esquema se inicializa al arrancar.

```powershell
$env:PSICOAPP_BD_URL='jdbc:mysql://localhost:3306/psicoapp_postman_20261108?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true'
$env:PSICOAPP_CORREO_ADMINISTRADOR='admin@unihumboldt.cue.co'
$env:PSICOAPP_CLAVE_ADMINISTRADOR='TU_CLAVE_LOCAL'
$env:PSICOAPP_DOMINIOS_INSTITUCIONALES='universidad.edu.co'
$env:PSICOAPP_NOTIFICACIONES_DESPACHO='true'
$env:PSICOAPP_NOTIFICACIONES_INTERVALO_MS='250'
$env:PSICOAPP_CORREO_HOST='127.0.0.1'
$env:PSICOAPP_CORREO_PUERTO='1'
$clavePruebas=New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($clavePruebas)
$env:PSICOAPP_NOTIFICACIONES_CLAVE_CIFRADO=[Convert]::ToBase64String($clavePruebas)
.\mvnw.cmd verify
java -jar target/psicoapp-0.0.1-SNAPSHOT.jar --server.port=8081 --debug=false --logging.level.root=INFO
```

La clave de cifrado aleatoria sirve para esta instancia desechable con SMTP apuntando al puerto local sin servicio. Los correos pendientes de una ejecución anterior pueden fallar al descifrarse tras regenerarla; no impide procesar los avisos internos. Para conservar y entregar correos en otros entornos se necesita una clave persistente según `../notificaciones.md`. Esta validación no comprueba entrega SMTP.

El servidor original del puerto 8080 rechazó ambos dominios probados al registrar estudiantes. Antes de volver a usarlo hay que configurar sus dominios institucionales y reiniciarlo con el código vigente. El JAR anterior no incluía las rutas de participación/evaluación. En la base original quedaron dos cuentas docentes ficticias `docente.postman.*` creadas durante el diagnóstico; no se modificó la cuenta administradora.
