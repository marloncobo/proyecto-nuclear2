# Frontend de Psicoapp

Interfaz para docentes y administradores en Angular 21, TypeScript y CSS. Componentes standalone, estado mediante signals e identificadores propios en español. Diseño adaptable con el logo proporcionado, azul profundo, verde salvia, tonos cálidos y formas orgánicas.

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

Si aparece una respuesta HTML en el acceso, comprobar que `/api/identidad/yo` en el servidor del frontend devuelve una respuesta de la API (normalmente 401 sin credenciales), no `index.html`. El cliente pide JSON y muestra un mensaje de conexión ante HTML o datos inválidos. El token CSRF y las respuestas de error JSON siguen usando el contrato existente.

## Funciones y alcance

- Pantalla inicial `/ingreso` para docentes y administradores: correo y contraseña, validación de campos, mostrar/ocultar contraseña, indicador de acceso en curso y errores accesibles. La identidad se comprueba con la API y la navegación se adapta al rol recibido; no se permite elegir un rol en el formulario.
- Inicio con resumen de registros cargados en la pestaña actual.
- Biblioteca con búsqueda por nombre, filtros, detalle, publicación y archivado confirmado.
- Creación de un borrador con un escenario, una pregunta, dos opciones con justificación y una rúbrica inicial: un criterio al 100 %, escala máxima 5.
- Consulta de casos y sesiones por UUID. La API existente no ofrece listados; conservar los identificadores para recuperar registros tras recargar.
- Creación, consulta y edición de espacios académicos. El backend impide editar espacios asociados a cualquier sesión; el formulario conserva los valores cuando hay conflicto.
- Programación con caso publicado, espacio, fechas, duración, navegación, lugar y UUID de estudiantes existentes. Búsqueda estudiantil por correo institucional exacto para agregar identidades activas sin duplicados. El servidor gestiona las convocatorias.
- Inicio y finalización de sesiones, con confirmación y validaciones del servidor.
- Reportes por caso y estudiante: calificación, retroalimentación y valoraciones existentes. Una evaluación pendiente no se convierte en cero; una lista vacía no se interpreta como ausencia.
- Administración con navegación propia según rol: listado de usuarios con filtros de rol/estado, detalle y edición de docentes en una ventana modal, y activación/desactivación confirmada. La ventana se cierra con Escape, la cruz, el fondo o «Volver al listado» y devuelve el foco al botón de origen; durante el guardado permanece abierta. Los errores de edición se muestran dentro de ella. La cuenta propia no se puede desactivar.
- Registro de docentes en un modal con nombre, correo y contraseña inicial (8 caracteres como mínimo, hasta 72 bytes). No se almacena la contraseña en el navegador; el formulario se limpia al cancelar o completar el registro. Un error conserva los campos para corregirlos. El nuevo docente se incorpora al listado si coincide con los filtros actuales.
- Cambio de rol desde el detalle, con revisión y confirmación dentro de la misma ventana. Se conserva el estado activo/inactivo y se respetan los filtros del listado. No se permite retirar el rol de la propia cuenta administrativa. El servidor valida el dominio al convertir a estudiante y rechaza cuentas estudiantiles sin contraseña al convertirlas a personal.
- La activación y desactivación de usuarios se confirman en modales. La configuración global del permiso de creación de casos no se ofrece en el frontend; el backend conserva sus validaciones y contratos.

El acceso sin identificar dirige a `/ingreso`. La demostración se abre de forma explícita con «Explorar la demostración» y utiliza datos ficticios **solo en memoria**; los cambios desaparecen al recargar. El parámetro público `demostracion=1` permite recuperar esa navegación sin autenticar ni acceder a datos reales. Al ingresar con una cuenta se descartan esos datos: el docente carga los espacios reales y el administrador carga usuarios y roles. La administración y la búsqueda estudiantil requieren conexión real. Los casos y sesiones reales consultados permanecen en memoria hasta recargar. El panel de encuentro muestra la primera sesión cargada; el resumen no representa totales globales del servidor.

La contraseña se limpia al completar o fallar el acceso. Al cerrar sesión se eliminan las credenciales y los datos consultados, y se regresa al login; volver con el historial no recupera una sesión cerrada. Recargar la página requiere identificarse de nuevo porque las credenciales no se almacenan en el navegador. No hay recuperación automática de contraseña: la pantalla orienta a contactar al administrador institucional.

La interfaz no incluye acceso estudiantil, alta de estudiantes, restablecimiento de contraseñas, editor de múltiples escenarios, sustitución de borradores, rúbricas avanzadas, cambios de convocados, escritura de valoraciones ni exportación de reportes. No declara cumplimiento completo de los RF. La autorización, tiempos y evaluación pertenecen al backend. El registro y la búsqueda de estudiantes son operaciones exclusivas del docente en la API actual; no se ofrecen al administrador.

### Contratos administrativos integrados

| Operación                | Endpoint                                                           |
| ------------------------ | ------------------------------------------------------------------ |
| Identidad y CSRF         | `GET /api/identidad/yo`, `GET /api/identidad/csrf`                 |
| Listado y filtros        | `GET /api/identidad/usuarios?rol=...&activo=...`                   |
| Detalle                  | `GET /api/identidad/usuarios/{id}`                                 |
| Registrar docente        | `POST /api/identidad/docentes`                                     |
| Editar docente           | `PUT /api/identidad/docentes/{id}`                                 |
| Activar/desactivar       | `PUT /api/identidad/usuarios/{id}/estado`                          |
| Catálogo y cambio de rol | `GET /api/identidad/roles`, `PUT /api/identidad/usuarios/{id}/rol` |

Las escrituras obtienen CSRF y solo actualizan los datos visibles tras una respuesta correcta. Se utilizan los contratos existentes sin cambios al backend. No hay endpoints administrativos de borrado o recuperación de contraseñas en este contrato.

## Integración

`src/app/servicio-api.ts` centraliza HTTP Basic, `/api/identidad/yo` y la obtención de CSRF antes de cada escritura. Envía la cabecera indicada por el servidor y conserva cookies de mismo origen. Credenciales solo en memoria; no se guardan claves, tokens o respuestas en almacenamiento del navegador. Los errores y tiempos de espera aparecen en pantalla.

Producción: publicar `dist/psicoapp/browser` mediante HTTPS, configurar fallback a `index.html` y proxy de mismo origen para `/api`. El proxy de Angular funciona únicamente en desarrollo. La interfaz de ingreso estudiantil de los enlaces de convocatoria sigue pendiente. Google Fonts proporciona DM Sans y Manrope, con fuentes de sistema como respaldo.

Accesibilidad: foco visible, ventanas con contención de foco, cierre con Escape y retorno al control de origen, etiquetas de formularios y movimiento reducido. Verificar además contraste y uso con lector de pantalla antes de una publicación institucional.

## Verificación

```powershell
npm run build
npm test
```

Playwright usa Microsoft Edge instalado (canal `msedge`), levanta el servidor automáticamente y simula la API sin tocar bases de datos ni enviar correos. En otro entorno ajustar el canal o instalar Edge con `npx playwright install msedge`.

La regresión del proxy también consulta `/api/identidad/yo` sin credenciales a través del servidor real de Angular: no realiza escrituras y admite que el backend esté detenido. Verifica que esa ruta no termine entregando el HTML del frontend. Se comprueban además HTML inesperado, JSON inválido y errores de proxy en la pantalla de ingreso.

Pruebas: búsqueda/filtros, navegación, creación/publicación, separación de datos ficticios/reales, CSRF, errores de acceso y desbordamiento móvil. `pruebas/gestion.spec.ts` agrega filtros/detalle/edición administrativa, errores 409 sin perder datos, confirmación de estados, búsqueda estudiantil exacta con correos que contienen `+`, prevención de duplicados, edición de espacios y administración móvil. `pruebas/ingreso.spec.ts` comprueba acceso inicial, validación sin solicitudes inválidas, visibilidad de contraseña, navegación por rol, cierre e historial, recarga sin credenciales, prevención de doble envío y diseño móvil/escritorio. Capturas generadas en `capturas/`, excluidas de Git. La compilación valida tipos y plantillas. Estas pruebas no sustituyen una integración contra Spring/MySQL real.

El presupuesto CSS del componente principal es de 24/30 kB (advertencia/error) porque reúne el diseño completo y sus adaptaciones; el presupuesto inicial total sigue en 500 kB/1 MB.

`npm audit --omit=dev` no reportó vulnerabilidades en las dependencias de producción. La auditoría completa reporta 16 avisos moderados transitivos de herramientas de desarrollo de Angular CLI 21; su solución automática propone cambiar de versión mayor y no se aplicó.
