# Frontend de Psicoapp

Interfaz para docentes, administradores y estudiantes en Angular 21, TypeScript y CSS.

Consulta la [configuración de skills, Figma y validación visual](HERRAMIENTAS-FRONTEND.md) y las [reglas locales del frontend](AGENTS.md). Angular sigue siendo el framework del proyecto.

## Ejecutar

Requiere npm y Node compatible con Angular 21: Node 20.19+, 22.12+ o 24.x. Verificado con Node 22.14.

```powershell
cd frontend
npm ci
npm start
```

Abrir http://localhost:3000. Ejecutar el backend por separado en el puerto 8080 según su guía. `proxy.conf.json` reenvía `/api` sin modificar el backend ni CORS.

El puerto y el proxy están definidos en `angular.json`, por lo que también se aplican al ejecutar `npx ng serve`. El patrón `/api/**` incluye las rutas anidadas de la API. Después de cambiar la configuración del proxy, detener y volver a iniciar el servidor de Angular; recargar el navegador no basta.

Si aparece una respuesta HTML en el acceso, comprobar que `/api/identidad/yo` en el servidor del frontend devuelve una respuesta de la API (normalmente 401 sin credenciales), no `index.html`. El cliente pide JSON y muestra un mensaje de conexión ante HTML o datos inválidos.

## Funciones y alcance

El ingreso llama a `POST /api/identidad/ingreso` con correo y contraseña para todos los roles,
y usa el JWT recibido en `Authorization: Bearer` para consultar identidad y operar.
El token queda únicamente en memoria; la contraseña no se conserva para autenticar
peticiones posteriores. Un 401 de la sesión activa limpia el acceso y vuelve al ingreso;
un 403 no cierra la sesión. No hay renovación automática. Consultar la
[configuración JWT del backend](../backend/README.md#autenticación-jwt).

- Pantalla inicial `/ingreso` para docentes y administradores: correo y contraseña, validación de campos, mostrar/ocultar contraseña, indicador de acceso en curso y errores accesibles. La identidad se comprueba con la API y la navegación se adapta al rol recibido; no se permite elegir un rol en el formulario.
- Inicio con resumen de registros cargados en la pestaña actual.
- Biblioteca con búsqueda por nombre, filtros, detalle, publicación y archivado confirmado.
- Creación y edición de borradores con varios escenarios, preguntas, opciones justificadas y criterios de rúbrica. Cada criterio reparte su ponderación entre sus preguntas.
- Carga de casos y sesiones del docente al conectar la cuenta, con consulta adicional por UUID.
- Creación, consulta y edición de espacios académicos. El backend impide editar espacios asociados a cualquier sesión; el formulario conserva los valores cuando hay conflicto.
- Programación con caso publicado, espacio, fechas, duración, navegación y lugar. Los docentes pueden buscar por correo exacto o seleccionar estudiantes activos por nombre o correo, registrar estudiantes y editar convocados. El servidor gestiona las convocatorias.
- Inicio y finalización de sesiones, con confirmación y validaciones del servidor.
- Reportes navegables desde la sesión y el nombre del estudiante, con calificación, respuestas, retroalimentación y publicación de valoraciones. Una evaluación pendiente se muestra como pendiente y la exportación CSV por sesión incluye identidades, intentos, notas y ausencias consolidadas cuando la sesión está cerrada.
- Administración con navegación propia según rol: listado de usuarios con búsqueda por nombre/correo en el servidor y filtros combinables de rol/estado, detalle y edición de docentes en una ventana modal, y activación/desactivación confirmada. La ventana se cierra con Escape, la cruz, el fondo o «Volver al listado» y devuelve el foco al botón de origen; durante el guardado permanece abierta. Los errores de edición se muestran dentro de ella. La cuenta propia no se puede desactivar.
- Registro de docentes en un modal con nombre, correo y contraseña inicial (8 caracteres como mínimo, hasta 72 bytes). No se almacena la contraseña en el navegador; el formulario se limpia al cancelar o completar el registro. Un error conserva los campos para corregirlos. El nuevo docente se incorpora al listado si coincide con los filtros actuales.
- Cambio de rol desde el detalle, con revisión y confirmación dentro de la misma ventana. Se conserva el estado activo/inactivo y se respetan los filtros del listado. No se permite retirar el rol de la propia cuenta administrativa. El servidor valida el dominio al convertir a estudiante y rechaza cuentas estudiantiles sin contraseña al convertirlas a personal.
- La activación y desactivación de usuarios se confirman en modales. Desde el detalle de cada docente, el administrador puede confirmar la concesión o revocación de su permiso para crear casos.
- El espacio protegido `/estudiante` presenta actividades, simulación, resultados por intento y notificaciones. Un enlace `/ingreso?sesion=` conserva la sesión durante el login y el cambio obligatorio de contraseña inicial. Consultar la actividad no crea intentos: el tiempo comienza únicamente al pulsar «Iniciar actividad». Una actividad en curso recupera pregunta, respuestas y límite del backend. La navegación respeta la modalidad de la sesión y las explicaciones solo aparecen después del cierre.
- El menú docente incluye «Notificaciones» con contador de no leídas e historial. Presenta recordatorios de sesiones próximas, cierres, estudiantes pendientes, evaluaciones disponibles y reportes listos; cada docente consulta y marca únicamente sus propios avisos.

El acceso sin identificar dirige a `/ingreso`. La demostración se abre de forma explícita con «Explorar la demostración» y utiliza datos ficticios **solo en memoria**; los cambios desaparecen al recargar. El parámetro público `demostracion=1` permite recuperar esa navegación sin autenticar ni acceder a datos reales. Al ingresar con una cuenta se descartan esos datos: el docente carga sus espacios, casos y sesiones y el administrador carga usuarios y roles. La administración y la búsqueda estudiantil requieren conexión real. Los casos y sesiones reales consultados permanecen en memoria hasta recargar. El panel de encuentro muestra la primera sesión cargada; el resumen no representa totales globales del servidor.

La contraseña se limpia al completar o fallar el acceso. Al cerrar sesión se eliminan las credenciales y los datos consultados, y se regresa al login; volver con el historial no recupera una sesión cerrada. Recargar la página requiere identificarse de nuevo porque las credenciales no se almacenan en el navegador. No hay recuperación automática de contraseña: la pantalla orienta a contactar al administrador institucional.

El restablecimiento de contraseñas queda fuera del alcance de la interfaz. La autorización, los tiempos y la evaluación pertenecen al backend. El registro, listado y búsqueda de estudiantes son operaciones del docente.

### Contratos administrativos integrados

| Operación                | Endpoint                                                                                           |
| ------------------------ | -------------------------------------------------------------------------------------------------- |
| Ingreso e identidad      | `POST /api/identidad/ingreso`, `GET /api/identidad/yo`                                             |
| Listado y filtros        | `GET /api/identidad/usuarios?rol=...&activo=...&busqueda=...`                                      |
| Detalle                  | `GET /api/identidad/usuarios/{id}`                                                                 |
| Registrar docente        | `POST /api/identidad/docentes`                                                                     |
| Editar docente           | `PUT /api/identidad/docentes/{id}`                                                                 |
| Activar/desactivar       | `PUT /api/identidad/usuarios/{id}/estado`                                                          |
| Permiso de creación      | `GET /api/identidad/usuarios/{idUsuario}`, `PUT /api/identidad/usuarios/{idUsuario}/creacion-casos` |
| Catálogo y cambio de rol | `GET /api/identidad/roles`, `PUT /api/identidad/usuarios/{id}/rol`                                 |

Las escrituras envían el JWT Bearer y solo actualizan los datos visibles tras una respuesta correcta. No hay endpoints administrativos de borrado o recuperación de contraseñas en este contrato.

## Integración

`src/app/servicio-api.ts` centraliza el ingreso JWT, `/api/identidad/yo` y la cabecera Bearer para cada solicitud protegida. No usa HTTP Basic, cookies de autenticación ni CSRF. Credenciales y token solo permanecen en memoria; no se guardan en el almacenamiento del navegador. Los errores y tiempos de espera aparecen en pantalla.

En producción, publicar `dist/psicoapp/browser` mediante HTTPS y configurar fallback a
`index.html`. El despliegue provisional en Firebase usa `npm run build:firebase` y la
variable `PSICOAPP_API_URL` para apuntar al origen HTTPS de Railway; el backend debe
autorizar el origen de Firebase mediante CORS. Consulta la
[guía de despliegue](../docs/despliegue.md). Google Fonts proporciona DM Sans y
Manrope, con fuentes de sistema como respaldo.

Accesibilidad: foco visible, ventanas con contención de foco, cierre con Escape y retorno al control de origen, etiquetas de formularios y movimiento reducido. Verificar además contraste y uso con lector de pantalla antes de una publicación institucional.

## Verificación

```powershell
npm run build
npm test
```

Playwright usa Microsoft Edge instalado (canal `msedge`), levanta el servidor automáticamente y simula la API sin tocar bases de datos ni enviar correos. En otro entorno ajustar el canal o instalar Edge con `npx playwright install msedge`.

La regresión del proxy también consulta `/api/identidad/yo` sin credenciales a través del servidor real de Angular: no realiza escrituras y admite que el backend esté detenido. Verifica que esa ruta no termine entregando el HTML del frontend. Se comprueban además HTML inesperado, JSON inválido y errores de proxy en la pantalla de ingreso.

Pruebas: búsqueda/filtros, navegación, creación/publicación, separación de datos ficticios/reales, Bearer, errores de acceso y desbordamiento móvil. `pruebas/gestion.spec.ts` agrega filtros/detalle/edición administrativa, errores 409 sin perder datos, confirmación de estados, búsqueda estudiantil exacta con correos que contienen `+`, prevención de duplicados, edición de espacios y administración móvil. `pruebas/ingreso.spec.ts` comprueba acceso inicial, validación sin solicitudes inválidas, visibilidad de contraseña, navegación por rol, cierre e historial, recarga sin credenciales, prevención de doble envío y diseño móvil/escritorio. Capturas generadas en `capturas/`, excluidas de Git. La compilación valida tipos y plantillas. Estas pruebas no sustituyen una integración contra Spring/MySQL real.

El presupuesto CSS del componente principal es de 24/30 kB (advertencia/error) porque reúne el diseño completo y sus adaptaciones; el presupuesto inicial total sigue en 500 kB/1 MB.

`npm audit --omit=dev` no reportó vulnerabilidades en las dependencias de producción. La auditoría completa reporta 16 avisos moderados transitivos de herramientas de desarrollo de Angular CLI 21; su solución automática propone cambiar de versión mayor y no se aplicó.

La administración muestra contador y limpieza de filtros, tabla en escritorio y tarjetas en móvil, etiquetas reutilizables de rol/estado y explicación de la desactivación bloqueada de la cuenta propia. El menú móvil tiene un control explícito y accesible. Los avisos de éxito se pueden cerrar y desaparecen automáticamente. Después de cada escritura se consulta de nuevo el listado; si falla esa consulta, se informa sin repetir la operación.
