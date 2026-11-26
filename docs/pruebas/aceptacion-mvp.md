# Aceptación técnica del MVP

**Fecha de ejecución** 22 de noviembre de 2026

**Versión evaluada** `29949fd`

**Ambiente** Angular y Spring Boot locales, Microsoft Edge, MySQL 8.0 aislado y
un buzón SMTP de pruebas en la misma máquina. Se usaron cuentas y casos ficticios.
Esta evidencia es anterior a la migración del adaptador de correo a Gmail.

## Caso de demostración

«Convivencia en el equipo» presenta dos escenarios consecutivos con una
pregunta y dos opciones justificadas en cada uno. Un criterio reparte su peso
entre las dos preguntas en una escala de 0 a 5. Una selección correcta y una
pendiente deben producir 2,5. La convocatoria incluye una estudiante que
participa y otra que permanece ausente.

## Recorrido reproducible

1. Ingresar como administrador, dar de alta al docente y habilitarle la creación
   de casos. Ingresar como docente y registrar el espacio y ambas estudiantes.
2. Guardar y publicar el caso. Programar una sesión flexible de un minuto con
   ambas estudiantes, iniciarla y abrir la URL del correo de convocatoria.
3. Resolver la primera pregunta, pasar a la segunda y volver a ingresar con el
   mismo código. Comprobar el progreso de una de dos y que las explicaciones
   aparezcan al entregar. Dejar la segunda pregunta pendiente y comprobar 2,5.
4. Autorizar un reintento desde Resultados y volver a abrir la convocatoria. El
   nuevo intento empieza en cero preguntas respondidas. Dejarlo vencer con la
   pantalla abierta y comprobar que aparece la calificación 0.
5. Finalizar la sesión y descargar el Excel. Comprobar las filas de intento
   finalizado, intento vencido y ausencia, con sus avances y notas disponibles.
6. Iniciar una sesión restringida, verificar que se deba responder antes de
   avanzar y que el botón Anterior permanezca deshabilitado. Cerrar la sesión
   mientras el intento sigue activo y comprobar la nota en la pantalla abierta.

## Resultado observado

Pasaron los seis pasos con el backend y MySQL reales. Se recibieron ambos
correos y se abrió el simulador desde el enlace del buzón. El CSV registró el
primer intento con 2,5 y una de dos preguntas respondidas, el reintento vencido
con 0 y la ausencia con dos preguntas calificadas en cero. Los datos iniciales
se prepararon por la API y el navegador se usó para el ingreso, la participación,
la autorización docente y la descarga.

## Verificación automatizada y base de datos

- `backend/mvnw.cmd -Ppruebas-mysql verify` completó 288 pruebas de Surefire y
  27 de Failsafe sin fallos ni omisiones. Incluye cierre automático de sesiones,
  roles, persistencia, reportes, reintentos y notificaciones.
- `npm run build` compiló el frontend y advirtió que el CSS del componente
  principal supera su presupuesto de advertencia. `npm test` pasó 42 pruebas
  con una API simulada.
- Se inicializó una base MySQL temporal con el esquema de `c7a63a6` y se
  aplicaron [001](../../backend/migraciones/001-fecha-fin-intento.sql) y
  [002](../../backend/migraciones/002-autorizacion-reintento.sql). Se verificaron
  las fechas del intento con precisión de microsegundos y la tabla de permisos.

## Decisiones de aceptación pendientes

- Aprobar el contrato portable del DSL y resolver las diferencias con el modelo
  de evaluación actual. [Correspondencia del DSL](../dise%C3%B1o/dsl.md#correspondencia-con-la-implementación-actual)
- Confirmar la interpretación de RF-22 con el responsable académico. La app
  aplica la mitad del intervalo programado según RN-15.
- Aceptar institucionalmente Gmail y acordar el catálogo de requisitos no
  funcionales. El adaptador actual se comprueba con un servidor HTTP simulado.
