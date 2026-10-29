# Alcance del proyecto — MVP de Psicoapp

**Estado:** propuesta para revisión y aprobación del equipo y del responsable académico.  
**Versión:** 0.1  
**Base documental:** rama `develop`, archivos `docs/requisitos/requisitos-funcionales.md` (27 RF), `reglas-negocio.md` (19 RN) y `casos-de-uso.md` (36 CU), consultados el 29 de octubre de 2026.

## 1. Contexto y problema

Psicoapp es un simulador académico para apoyar la enseñanza de psicología social. Su propósito es permitir a los docentes estructurar casos educativos, ofrecer a los estudiantes una experiencia de toma de decisiones y registrar, evaluar y consultar los resultados con base en rúbricas. **No es una herramienta de diagnóstico clínico real** (RN-01).

## 2. Objetivo del MVP

Validar, con un caso académico realista y usuarios de los tres roles, que Psicoapp puede ejecutar **de principio a fin** el flujo: creación del caso → validación y publicación → programación y convocatoria → participación autorizada → almacenamiento de respuestas → calificación por rúbrica → consulta de resultados.

El MVP es una **primera versión funcional verificable**, no una eliminación de los requisitos vigentes. Los elementos pospuestos de la versión completa requerirán aprobación formal del equipo y el responsable académico.

## 3. Actores

- **Administrador:** registra docentes y controla roles y permisos (RF-02, RF-03; RN-10).
- **Docente:** crea y publica casos, asocia rúbricas, programa y gestiona sesiones, convoca estudiantes y consulta sus resultados.
- **Estudiante:** se identifica, accede solo a sesiones autorizadas, responde, termina o reanuda intentos y consulta calificación y retroalimentación.

## 4. Alcance funcional mínimo — primera entrega

| Capacidad | Comportamiento demostrable | Trazabilidad |
| --- | --- | --- |
| Identidad y autorización | Administrador registra docente; separación de permisos y acceso del estudiante mediante correo institucional y código de convocatoria. | RF-01, RF-02, RF-03, RF-20; RN-04, RN-10 |
| Construcción del caso | Docente crea un borrador con información académica, escenarios ordenados, preguntas, opciones correctas/incorrectas y justificación. | RF-05, RF-06, RF-07, RF-08; RN-01, RN-02, RN-11 |
| Rúbrica y publicación | Docente asocia rúbrica; solo puede publicar un caso que cumpla estructura y validaciones del DSL, y únicamente el publicado se agenda. | RF-09, RF-17; RN-03, RN-05 |
| Programación y convocatoria inicial | Docente asigna espacio académico, estudiantes, fechas y duración; se crea una autorización para cada convocado. | RF-04, RF-18, RF-20; RN-03, RN-04, RN-13 |
| Ciclo de vida de la sesión | Docente inicia/finaliza; sesión pasa por SIN_INICIAR, EN_CURSO y FINALIZADO; cierre automático un día después de la **hora programada de finalización** si sigue abierta. | RF-19; RN-12 |
| Participación | Estudiante autorizado inicia dentro de la ventana permitida; ve escenarios en orden fijo; navegación configurable con o sin retroceso; guarda última respuesta y progreso. | RF-10, RF-11, RF-12, RF-22; RN-02, RN-04, RN-14, RN-15, RN-19 |
| Control de tiempo y continuidad | Temporizador individual persiste al salir; reanudación si sigue vigente; al agotarse finaliza el intento, registra pendientes con cero y conserva respuestas. | RF-23, RF-24; RN-06, RN-16, RN-17, RN-18 |
| Evaluación y resultados | Calcula y almacena nota conforme a la rúbrica; explicaciones y nota se muestran **solo tras finalizar**; docente consulta respuestas y calificación. | RF-13, RF-14, RF-15, RF-24; RN-08, RN-09, RN-16, RN-19 |
| Ausencias y cierre | Al finalizar una sesión se consolidan los estudiantes convocados que no iniciaron, con calificación correspondiente conforme a la rúbrica. | RF-24; RN-12, RN-16; CU-35 |

**Condiciones transversales que no pueden omitirse en esta versión:** protección real del acceso en backend; validaciones antes de publicar y permitir participar; persistencia independiente de intentos; respeto del reloj de sesión y de intento; calificación por rúbrica; no permitir participación en sesión finalizada.

## 5. Funcionalidades propuestas para fase posterior (pendientes de aprobación)

Esta priorización **no altera** el catálogo vigente. Si el MVP se evalúa contra todos los RF, las siguientes funciones deberán implementarse antes de declarar cumplimiento total o documentarse como excepciones autorizadas:

| Función de la versión completa | RF / RN | Propuesta |
| --- | --- | --- |
| Descargar reporte académico exportable | RF-16 | Segunda iteración; el MVP mantiene consulta docente de resultados (RF-15). |
| Autorizar un reintento excepcional de forma operativa | RF-25; RN-07 | Segunda iteración, manteniendo la regla de que no haya reintentos arbitrarios. El MVP no habilita nuevos intentos por defecto. |
| Valoración general adicional del docente | RF-26 | Segunda iteración; se conserva retroalimentación por pregunta y resultado final. |
| Agregar o retirar convocados después de programar | RF-21 y relaciones CU-34 | Segunda iteración; la convocatoria inicial sí funciona en el MVP. |
| Notificaciones automáticas por correo de convocatoria y avisos de resultados/valoración | RF-21, RF-27 | **Decisión pendiente:** los RF las exigen. Posponer requiere aprobación explícita; alternativamente incluir envío mínimo por correo en el MVP. |
| Archivado de casos | RF-09; CU-11 | Segunda iteración propuesta; mantener inicialmente BORRADOR/PUBLICADO sin programar casos no publicados. Validar autorización del recorte. |

## 6. Exclusiones y límites

### Fuera del propósito del producto

- Diagnósticos clínicos reales, decisiones asistenciales o emisión de conceptos profesionales sobre personas reales (RN-01).
- Ramificación de escenarios según respuestas: la secuencia vigente es fija (RN-02).

### No comprometido en este MVP por los RF/RN revisados

- Un editor gráfico complejo de escenarios, generación de imágenes o textos con IA, videollamadas, multijugador en tiempo real o integraciones con un sistema académico externo. **Su ausencia de los requisitos actuales no significa que estén prohibidos**; requieren nuevo análisis y aprobación antes de incorporarse al alcance.
- Modalidades de evaluación alternativas no definidas. La rúbrica aprobada es la fuente de cálculo; Strategy es una propuesta técnica, no un requisito de múltiples algoritmos.

## 7. Criterios de aceptación del MVP

El MVP se considerará listo para una demostración de validación cuando se pueda verificar lo siguiente:

1. Un administrador crea un docente, y los permisos impiden que un usuario sin autorización publique casos.
2. Un docente crea y publica un caso completo con **al menos dos escenarios ordenados y varias preguntas**, opciones, explicaciones y una rúbrica. Esta cantidad es un **escenario de prueba propuesto**, no una obligación nueva para todos los casos.
3. Un caso incompleto no puede publicarse ni utilizarse para programar una sesión.
4. El docente programa una sesión con un caso publicado y convoca al menos un estudiante identificado.
5. El estudiante autorizado accede con correo institucional y código; un no convocado no participa y el ingreso posterior al límite de RN-15 se rechaza.
6. El estudiante registra respuestas, conserva progreso tras salir y puede reanudar dentro del tiempo restante.
7. Se verifica el comportamiento de retroceso habilitado y deshabilitado sin cambiar el orden de los escenarios; la última respuesta válida es la evaluada.
8. Al finalizar voluntariamente o por agotamiento del tiempo, se registra la nota calculada por rúbrica, con cero para las preguntas pendientes, y no se permiten modificaciones posteriores.
9. La retroalimentación solo se consulta tras finalizar; el docente puede consultar la participación y su resultado.
10. El docente puede cerrar la sesión; la prueba automatizada valida también el cierre de una sesión que siga abierta un día después de la **finalización programada** y la consolidación de ausencias.

Los criterios describen resultados observables y deberán transformarse en pruebas automatizadas o de aceptación. No implican que la solución esté implementada actualmente.

## 8. Entregables técnicos propuestos

- Backend Spring Boot funcional y persistente, manteniendo el monolito modular documentado.
- Interfaz mínima para los recorridos de administrador, docente y estudiante.
- Esquema de base de datos y migraciones coherentes con los RF/RN.
- Pruebas de autorización, publicación, sesión, participación y cálculo de nota.
- Manual breve de ejecución, caso de prueba de demostración y registro de resultados.
- Registro de funcionalidades pospuestas y decisiones de alcance aprobadas.

No se fijan fechas, infraestructura de despliegue ni objetivos cuantitativos de rendimiento: los requisitos consultados no los especifican. Deberán acordarse por separado, junto con los RNF.

## 9. Riesgos y decisiones abiertas

1. **Correo obligatorio:** confirmar si RF-21 y RF-27 deben demostrarse en la primera entrega. Se recomienda incluir el envío mínimo si hay un servicio disponible; de lo contrario, registrar la excepción.
2. **Recorte de RF:** aprobar expresamente la postergación de exportación, reintentos, comentarios generales, edición de convocatoria y archivado. Mientras no se apruebe, siguen siendo obligaciones del alcance general.
3. **RF-22 y RN-15:** el RF señala una base temporal ambigua; RN-15 establece que el límite se calcula desde el inicio de la sesión programada. Registrar esta interpretación con el responsable académico antes de implementar.
4. **DSL:** concretar qué reglas verificables debe aplicar la validación de casos conforme a RN-05, sin sustituirla por una comprobación solo de campos no vacíos.
5. **RNF:** seguridad, desempeño, confiabilidad y despliegue todavía requieren un catálogo de requisitos no funcionales acordado.

## 10. Relación con los documentos vigentes

- [Requisitos funcionales](requisitos/requisitos-funcionales.md)
- [Reglas de negocio](requisitos/reglas-negocio.md)
- [Casos de uso](requisitos/casos-de-uso.md)
- [Arquitectura inicial del backend](../backend/README.md)

**Criterio de gobierno:** este documento prioriza entregas, pero **no modifica** los RF/RN de origen. Ante contradicciones se deben corregir las decisiones de alcance mediante revisión, no cambiar silenciosamente los requisitos.
