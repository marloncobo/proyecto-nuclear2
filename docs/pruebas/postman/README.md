# Pruebas de Postman

Importar `Psicoapp.postman_collection.json` y `Psicoapp-local.postman_environment.json` en Postman. Seleccionar el entorno **Psicoapp - local**, completar `adminPassword` con la contraseña local y ejecutar la colección completa en **Runner**, con una iteración y las cookies habilitadas.

El entorno apunta a `http://localhost:8081`, instancia aislada usada para esta validación con MySQL y la base `psicoapp_postman_20261108`. El administrador de pruebas es `admin@unihumboldt.cue.co` y el dominio institucional configurado es `universidad.edu.co`.

Cada ejecución genera correos ficticios y UUID nuevos. Crea dos docentes (responsable y ajeno), estudiante, espacio, caso, sesión, convocatoria e intento; habilita `CREAR_CASOS` para el rol docente; termina la participación, consulta su evaluación, registra una valoración desde reportes, cierra la sesión y archiva el caso. Los registros se conservan para inspeccionarlos en MySQL Workbench. No ejecuta limpieza ni elimina registros. Si una respuesta tiene un estado HTTP inesperado, el Runner se detiene y puede dejar entidades sin cerrar. Los rechazos esperados 400/403/404/409 cuentan como pruebas correctas cuando coinciden con el escenario.

La colección obtiene CSRF con las credenciales de cada actor, conserva cookies, captura los IDs y usa el código de convocatoria como contraseña estudiantil. Las fechas se generan al programar, con precisión de segundos y una ventana que permite iniciar inmediatamente. La evaluación usa al estudiante propietario, de acuerdo con la seguridad vigente. Avanzar y retroceder comprueban los límites de un caso de una sola pregunta; no cubren navegación entre múltiples preguntas.

Se corrigieron el JSON inválido de actualizar caso (signos `+` pegados), los textos con codificación incorrecta, el código de acceso fijo y el orden de archivado/cierre. El archivo original no contenía aserciones; esta copia agrega comprobaciones de HTTP, JSON, IDs, estados, respuesta guardada y nota máxima de 5.

`resultado-local.json` contiene el informe de Newman sin contraseñas, cookies ni códigos de convocatoria. Las estadísticas de peticiones incluyen las consultas CSRF auxiliares; `resultados` enumera las 45 peticiones principales.

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

Validación del **9 de noviembre de 2026** con Newman 6.2.3 y MySQL real: **45 peticiones principales, 114 verificaciones, 0 fallos**. La secuencia original de 27 peticiones también se ejecuta en esta misma colección. No se modificó código del backend para estas pruebas. El alcance no incluye exportación de reportes, ausencias, múltiples intentos ni pruebas específicas de notificaciones o entrega SMTP.

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
$env:PSICOAPP_NOTIFICACIONES_DESPACHO='false'
$clavePruebas=New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($clavePruebas)
$env:PSICOAPP_NOTIFICACIONES_CLAVE_CIFRADO=[Convert]::ToBase64String($clavePruebas)
.\mvnw.cmd verify
java -jar target/psicoapp-0.0.1-SNAPSHOT.jar --server.port=8081 --debug=false --logging.level.root=INFO
```

La clave de cifrado aleatoria sirve para esta instancia desechable con despacho desactivado. Para conservar y entregar correos en otros entornos se necesita una clave persistente según `../notificaciones.md`. Esta validación no comprueba entrega SMTP.

El servidor original del puerto 8080 rechazó ambos dominios probados al registrar estudiantes. Antes de volver a usarlo hay que configurar sus dominios institucionales y reiniciarlo con el código vigente. El JAR anterior no incluía las rutas de participación/evaluación. En la base original quedaron dos cuentas docentes ficticias `docente.postman.*` creadas durante el diagnóstico; no se modificó la cuenta administradora.
