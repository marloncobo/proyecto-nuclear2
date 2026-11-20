# Frontend de Psicoapp

## Tecnología y alcance

- Mantener Angular 21, TypeScript, componentes standalone, signals, formularios Angular y CSS. Las preferencias por React/Vite/Next/shadcn de una skill no autorizan cambiar el framework de este repositorio.
- Respetar `CONTRIBUTING.md`: identificadores propios y textos en español. Usar npm y el lockfile existente.
- Trabajar en `frontend/`. No modificar el backend ni su configuración local para resolver diseño o pruebas.
- Antes de crear componentes, revisar `src/app/` y reutilizar componentes, formularios, patrones de diálogo y tokens existentes. Extraer piezas compartidas cuando un cambio real lo justifique; no duplicar controles ni refactorizar toda la aplicación como parte de un ajuste pequeño.

## Diseño UI/UX y adaptación

- Conservar la marca Psicoapp y la dirección visual aprobada: azul `--azul`, salvia `--salvia`, tipografía y escala existentes. Centralizar nuevos tokens compartidos en `src/styles.css`.
- Priorizar jerarquía clara, poco ruido visual, espacios consistentes y estados de carga, vacío, éxito y error. No inventar métricas ni promesas del producto.
- Para nuevos diseños o rediseños completos, usar `frontend-app-builder` y acordar una referencia completa antes de implementar. Para ajustes pequeños, respetar el diseño existente y el alcance del usuario.
- Comprobar escritorio (1440×1000), tableta (768×1024) y móvil (390×844). Evitar desbordamiento horizontal, solapamientos y controles inaccesibles. Respetar movimiento reducido.
- Los modales deben conservar etiquetas accesibles, foco contenido, Escape, bloqueo de fondo y retorno al control de origen. Mantener datos de formulario ante errores y evitar envíos repetidos.

## Figma y componentes

- Las skills locales de Figma no implican que la cuenta esté conectada. Antes de usar datos o escribir en Figma, verificar la conexión del plugin/MCP y el acceso al archivo.
- Pedir o utilizar un enlace real con `node-id`; no inventar archivos, nodos o bibliotecas. Solicitar contexto y captura del nodo antes de implementarlo.
- Indicar Angular y TypeScript al solicitar contexto cuando la herramienta admita esos parámetros. Si devuelve ejemplos React, tratarlos como referencia y traducir a componentes Angular existentes, sin incorporar React.
- Reutilizar componentes publicados, variantes, variables y estilos del sistema de diseño cuando existan. Code Connect es opcional: comprobar plan/asiento, publicación y compatibilidad con la documentación vigente de Figma antes de crear asociaciones. No instalar paquetes ni publicar mappings sin un archivo y componente concretos.
- Estas son reglas locales del proyecto; no afirmar que fueron generadas por Figma mientras no se haya ejecutado su herramienta autenticada.

## Validación

- Revisar los scripts de `package.json`. Ejecutar `npm run build` y las pruebas funcionales apropiadas al cambio. `npm test` conserva la suite funcional existente.
- `npm run test:visual` compara capturas de ingreso, biblioteca y modal de creación en tres tamaños. Se usa Edge/Chromium del proyecto y referencias separadas por plataforma.
- Para un cambio visual deliberado: ejecutar `npm run test:visual:update`, inspeccionar las imágenes y sus diferencias con `view_image` y luego `npm run test:visual`. No actualizar referencias para ocultar una regresión.
- Usar datos ficticios y la demostración en QA visual. No guardar credenciales, capturas de información real ni estados de autenticación en Git.
- Revisar visualmente las capturas; una comparación automática no certifica UX, accesibilidad completa ni integración con Spring/MySQL real.
- En Windows, la skill CLI de Playwright puede usarse con `npx.cmd --yes --package @playwright/cli@0.1.25 playwright-cli`; utilizar `--browser msedge` al abrir una sesión. Esto evita depender de rutas Bash/WSL para herramientas npm de Windows.
- Preferir herramientas Browser nativas cuando estén disponibles para exploración manual. La suite de Playwright sigue siendo la comprobación reproducible del proyecto; no afirmar que se usó un navegador o plugin que no esté disponible.

## Evidencia

- Mantener las referencias visuales en `pruebas-visuales/referencias/`; capturas de fallo, trazas e informes son artefactos excluidos de Git en `test-results/` y `playwright-report/`.
- Documentar los tamaños revisados, los recorridos comprobados y cualquier dependencia pendiente. Ver `HERRAMIENTAS-FRONTEND.md` para instalación, conexión y fuentes.
