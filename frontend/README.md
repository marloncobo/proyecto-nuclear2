# Frontend de Psicoapp

Interfaz para docentes y administradores en Angular 21, TypeScript y CSS. Componentes standalone, estado mediante signals e identificadores propios en español. Diseño adaptable con el logo proporcionado, azul profundo, verde salvia, tonos cálidos y formas orgánicas.

## Ejecutar

Requiere npm y Node compatible con Angular 21: Node 20.19+, 22.12+ o 24.x. Verificado con Node 22.14.

```powershell
cd frontend
npm ci
npm start
```

Abrir http://localhost:3000. Ejecutar el backend por separado en el puerto 8080 según su guía. `proxy.conf.json` reenvía `/api` sin modificar el backend ni CORS.

## Funciones y alcance

- Inicio con resumen de registros cargados en la pestaña actual.
- Biblioteca con búsqueda por nombre, filtros, detalle, publicación y archivado confirmado.
- Creación de un borrador con un escenario, una pregunta, dos opciones con justificación y una rúbrica inicial: un criterio al 100 %, escala máxima 5.
- Consulta de casos y sesiones por UUID. La API existente no ofrece listados; conservar los identificadores para recuperar registros tras recargar.
- Creación, consulta y edición de espacios académicos. El backend impide editar espacios asociados a cualquier sesión; el formulario conserva los valores cuando hay conflicto.
- Programación con caso publicado, espacio, fechas, duración, navegación, lugar y UUID de estudiantes existentes. Búsqueda estudiantil por correo institucional exacto para agregar identidades activas sin duplicados. El servidor gestiona las convocatorias.
- Inicio y finalización de sesiones, con confirmación y validaciones del servidor.
- Reportes por caso y estudiante: calificación, retroalimentación y valoraciones existentes. Una evaluación pendiente no se convierte en cero; una lista vacía no se interpreta como ausencia.
- Administración con navegación propia según rol: listado de usuarios con filtros de rol/estado, consulta de detalle, edición del nombre/correo de docentes y activación/desactivación confirmada. La cuenta propia no se puede desactivar.
- Consulta de roles y permisos de creación de casos. Los cambios de permiso requieren confirmación y afectan a todo el rol docente.

La demostración utiliza datos ficticios **solo en memoria**; los cambios desaparecen al recargar. Al conectar una cuenta se descartan esos datos: el docente carga los espacios reales y el administrador carga usuarios, roles y permisos. La administración y la búsqueda estudiantil requieren conexión real. Los casos y sesiones reales consultados permanecen en memoria hasta recargar. El panel de encuentro muestra la primera sesión cargada; el resumen no representa totales globales del servidor.

La interfaz no incluye acceso estudiantil, alta de docentes o estudiantes, cambio de rol de usuarios, restablecimiento de contraseñas, editor de múltiples escenarios, sustitución de borradores, rúbricas avanzadas, cambios de convocados, escritura de valoraciones ni exportación de reportes. No declara cumplimiento completo de los RF. La autorización, tiempos y evaluación pertenecen al backend.

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

Pruebas: búsqueda/filtros, navegación, creación/publicación, separación de datos ficticios/reales, CSRF, errores de acceso, Escape y desbordamiento móvil. `pruebas/gestion.spec.ts` agrega filtros/detalle/edición administrativa, errores 409 sin perder datos, confirmación de estados y permisos, búsqueda estudiantil exacta con correos que contienen `+`, prevención de duplicados, edición de espacios y administración móvil. Capturas generadas en `capturas/`, excluidas de Git. La compilación valida tipos y plantillas. Estas pruebas no sustituyen una integración contra Spring/MySQL real.

El presupuesto CSS del componente principal es de 24/30 kB (advertencia/error) porque reúne el diseño completo y sus adaptaciones; el presupuesto inicial total sigue en 500 kB/1 MB.

`npm audit --omit=dev` no reportó vulnerabilidades en las dependencias de producción. La auditoría completa reporta 16 avisos moderados transitivos de herramientas de desarrollo de Angular CLI 21; su solución automática propone cambiar de versión mayor y no se aplicó.
