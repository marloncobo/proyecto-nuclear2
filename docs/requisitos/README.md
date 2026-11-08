# Requisitos de Psicoapp

Documentación técnica derivada del Google Sheets `Requisitos_RF_RN_con_patrones`, organizada para versionarse en Git. **Sincronización de patrones: 29-10-2026.**

## Índice

- [Requisitos funcionales](requisitos-funcionales.md) — 27 RF, hoja `16 oct`.
- [Reglas de negocio](reglas-negocio.md) — 19 RN, hoja `RN depuradas`.
- [Casos de uso](casos-de-uso.md) — 36 CU, hoja `Casos de uso`.

## Alcance y origen

Estos archivos contienen el texto de las hojas indicadas, con ajustes de formato (espacios y estructura Markdown), sin cambiar su significado. Las recomendaciones y justificaciones de patrones reflejan las celdas actualizadas de las hojas `16 oct` y `RN depuradas` del Google Sheets. **Son propuestas de diseño**, no patrones ya implementados. Los textos de requisitos y reglas de negocio no se modificaron.

La hoja `RN académicas PN3` no se ha incorporado al catálogo vigente: contiene reglas de una iteración anterior que contradicen algunas decisiones actuales (p. ej., ramificación de escenas, retroalimentación y calificación), además de una fila marcada «NO USAR ESTOS». Se conserva únicamente en el libro de origen para trazabilidad histórica.

La columna «Módulo / épica» de los RF y la columna «Módulo» de los CU describen agrupaciones del documento original y **no equivalen automáticamente** a los paquetes actuales del backend (`identity`, `academic`, `cases`, `sessions`, `participation`, `evaluation`, `notifications`, `reports`). Su correspondencia debe validarse en una etapa de diseño posterior.

## Requisitos no funcionales

El libro proporcionado no contiene una hoja consolidada de requisitos no funcionales (RNF). No se crea un catálogo RNF ficticio; este deberá elaborarse y aprobarse por separado.

## Mantenimiento

- Mantener estables los identificadores `RF-XX`, `RN-XX` y `CU-XX`.
- Modificar requisitos mediante revisión del equipo y Pull Request.
- Actualizar las referencias cruzadas cuando cambie un requisito.
- Evitar mezclar versiones anteriores con las reglas vigentes.

## Patrones seleccionados en la tabla académica corregida

| Patrón | Requisitos relacionados | Clase o interfaz propuesta |
| --- | --- | --- |
| Builder | RF-05, RF-07, RF-17 | `CasoBuilder` |
| Facade | RF-03, RF-04, RF-15 | `AdministracionFacade` |
| Proxy | RF-02, RF-20 | `ParticipacionProxy` |
| State | RF-19, RF-25 | `SesionState` |
| Strategy | RF-13, RF-14, RF-24 | `CalculoNotaStrategy` |
| Observer | RF-21, RF-27 | `NotificacionObserver` |
| Template Method | RF-09 | `ValidacionPublicacionTemplate` |

Las recomendaciones de los catálogos RF/RN pueden incluir asociaciones adicionales a las siete seleccionadas en esa tabla. No se debe interpretar la relación con un patrón como una obligación de implementarlo; especialmente, las variantes de cálculo de notas son ejemplos técnicos sujetos a validación.

La hoja `RN académicas PN3` y el catálogo de casos de uso no se modificaron durante la actualización de patrones.
