# Herramientas de frontend

Revisión realizada el 19 de noviembre de 2026. El proyecto mantiene Angular 21. Esta configuración prepara diseño y QA; no rediseña la aplicación ni modifica el backend.

## Skills instaladas en Codex

Ubicación de usuario: `C:/Users/Usuario/.codex/skills/`. Están instaladas en este equipo, no empaquetadas como dependencias npm ni incluidas en Git. Se cargarán en el siguiente turno de Codex; si no aparecen, reiniciar Codex.

| Skill                              | Fuente oficial                   | Uso en Angular                                                                                                                                                              |
| ---------------------------------- | -------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `frontend-app-builder`             | `openai/plugins`, Build Web Apps | Dirección visual, referencias, tokens, componentes y fidelidad. Su preferencia por React no se aplica a este proyecto: tanto el usuario como el repositorio exigen Angular. |
| `frontend-testing-debugging`       | `openai/plugins`, Build Web Apps | Revisión de interfaz, flujos, errores, adaptación y evidencia visual.                                                                                                       |
| `playwright`                       | `openai/skills`, catálogo curado | Exploración del navegador mediante CLI; independiente del framework.                                                                                                        |
| `figma-use`                        | `openai/skills`, catálogo curado | Uso del contexto y la API de Figma cuando el conector esté disponible.                                                                                                      |
| `figma-implement-design`           | `openai/skills`, catálogo curado | Traducir diseños a los componentes Angular del repositorio.                                                                                                                 |
| `figma-create-design-system-rules` | `openai/skills`, catálogo curado | Generar reglas desde un sistema de diseño conectado. Las reglas locales de `AGENTS.md` se prepararon desde el repositorio, no desde Figma.                                  |
| `figma-code-connect-components`    | `openai/skills`, catálogo curado | Vincular componentes publicados a componentes de código; activación pendiente de archivo, cuenta y plan compatibles.                                                        |
| `figma-generate-design`            | `openai/skills`, catálogo curado | Creación de pantallas reutilizando componentes y tokens de Figma, una vez autorizado el acceso.                                                                             |

Fuentes comprobadas: [Build Web Apps](https://github.com/openai/plugins/tree/main/plugins/build-web-apps), [skills curadas de OpenAI](https://github.com/openai/skills/tree/main/skills/.curated). Revisiones consultadas: `openai/plugins@981b02c0c44058b595f3b4180002c3ae3c806d40` y `openai/skills@49f948faa9258a0c61caceaf225e179651397431`.

El listado por la API de GitHub respondió 403; se verificó el catálogo mediante clones públicos de los repositorios oficiales y se instalaron las skills con el instalador oficial en modo Git. `frontend-app-builder` no apareció como plugin instalable en la búsqueda de esta cuenta; sí se pudo instalar su skill desde la fuente oficial. No se instalaron plugins de terceros ni el paquete completo con guías React, shadcn, Stripe o Supabase.

## Figma: disponible, conexión pendiente

El catálogo confirmó que el plugin Figma está disponible para esta cuenta y no estaba instalado al realizar la revisión. Se inició la opción de instalación/conexión, pero esto no autoriza la cuenta automáticamente. No se ha accedido a archivos de Figma ni se han publicado componentes.

Para completar la integración:

1. Instalar Figma desde los plugins de Codex y completar su autorización de cuenta.
2. Confirmar que aparecen las herramientas del plugin en la conversación.
3. Facilitar un enlace al archivo y al nodo de diseño (`node-id`) con permisos suficientes. No hace falta exponer tokens en código o en el chat.
4. Solicitar contexto para Angular/TypeScript y reutilizar componentes existentes. Los ejemplos React que pueda devolver una herramienta son referencias, no una orden de cambiar el framework.

Se prefiere el plugin nativo; no se registró un segundo servidor Figma MCP ni se modificaron credenciales globales. El servidor remoto declarado por el plugin es `https://mcp.figma.com/mcp`. Figma Desktop solo es necesario si se desea trabajar con una selección local en lugar de enlaces remotos.

### Code Connect, opcional

Hace falta una biblioteca con componentes publicados, una URL de nodo y un plan/asiento que habilite Code Connect. No se instaló `@figma/code-connect` en la aplicación ni se generaron mappings ficticios: todavía no existe un archivo de diseño conectado.

La [documentación vigente de Figma](https://help.figma.com/hc/en-us/articles/35498295267991-Figma-MCP-collection-Improve-code-generation-with-Figma-s-Code-Connect-UI) incluye asientos Dev o Full en planes Professional, Organization y Enterprise. La skill curada todavía menciona Organization/Enterprise; comprobar la disponibilidad real de la cuenta antes de asumir una limitación. Figma admite documentar componentes Angular mediante HTML; para nuevas asociaciones seguir las plantillas actualmente mantenidas, no introducir parsers React heredados. Véanse [las guías de Code Connect](https://developers.figma.com/docs/code-connect/html/).

## Playwright: funcional y visual

Ya estaban instalados `@playwright/test` y `playwright` 1.65.1. Node 22.14 y npm 10.9 están presentes. Se conserva Microsoft Edge (`msedge`), disponible en el equipo. La CLI `@playwright/cli` 0.1.25 se verificó mediante `npx`; no se añadió una dependencia global ni se cambió el framework.

Desde `frontend/`:

```powershell
npm test
npm run test:visual
npm run test:visual:report
```

La configuración visual comprueba ingreso, biblioteca de demostración y modal de creación en escritorio (1440×1000), tableta (768×1024) y móvil (390×844). Captura nueve referencias en seis pruebas. Usa datos ficticios, sin cuentas reales ni escrituras al backend. El navegador emplea español de Colombia, zona horaria de Bogotá, escala 1, tema claro y movimiento reducido; espera a las fuentes antes de comparar.

La biblioteca se captura al inicio de la página y sin un control enfocado, para evitar que el desplazamiento heredado del login móvil altere la referencia. Las comprobaciones funcionales de teclado se mantienen en la suite existente; no se ocultan elementos con CSS para hacer pasar la comparación.

Para crear o actualizar deliberadamente las referencias:

```powershell
npm run test:visual:update
```

Inspeccionar las imágenes de `pruebas-visuales/referencias/` y volver a ejecutar `npm run test:visual`. Estas referencias registran el estado actual; no sustituyen una revisión de UX ni convierten un diseño en aprobado por el usuario. La tolerancia es del 0,5 % de píxeles, con animaciones desactivadas. Las referencias se separan por tamaño y sistema operativo porque fuentes y renderizado pueden variar entre entornos, como explica [Playwright](https://playwright.dev/docs/test-snapshots).

Se conservan capturas de fallo, trazas e informe visual en `test-results/visual/` y `playwright-report/visual/`, ya excluidos de Git. Las referencias aprobadas sí se versionan. Revisar diferencias y no regenerar referencias únicamente para hacer pasar una prueba.

### Exploración con la skill CLI en Windows

```powershell
npx.cmd --yes --package @playwright/cli@0.1.25 playwright-cli -s=psicoapp open http://localhost:3000/ingreso --browser msedge
npx.cmd --yes --package @playwright/cli@0.1.25 playwright-cli -s=psicoapp snapshot
npx.cmd --yes --package @playwright/cli@0.1.25 playwright-cli -s=psicoapp close
```

Los comandos `npx.cmd` evitan problemas entre las rutas de Windows y el wrapper Bash de la skill. Bash también está presente, pero no se necesita WSL para la suite del proyecto. Las capturas de exploración deben guardarse en los directorios de artefactos excluidos o fuera del repositorio.

## Dependencias que faltan o dependen de otra cuenta

- **Figma:** instalación y autorización del plugin, acceso a un archivo/nodo real y permisos para la operación solicitada. Las skills están instaladas, pero no sustituyen esa conexión.
- **Code Connect:** biblioteca publicada y plan/asiento compatible; no comprobados. Su CLI es opcional y se añadiría solo al realizar mappings concretos.
- **Otro equipo o CI:** ejecutar `npm ci` e instalar Edge si falta (`npx playwright install msedge`), o acordar un navegador fijo y generar referencias separadas. No se instalaron Firefox/WebKit porque no son necesarios para esta configuración.
- **Fuentes:** el diseño actual carga Google Fonts y necesita acceso a ese servicio para reproducir exactamente la tipografía. Para una CI aislada, conviene alojar las fuentes localmente; no se cambió su distribución como parte de este ajuste.
- **Playwright Interactive:** no se instaló esta variante porque requiere el runtime específico `js_repl`, que no se expone en esta sesión. La skill CLI y las pruebas del proyecto funcionan sin él.
- **ImageGen:** la capacidad de generación de imágenes y su skill del sistema ya están disponibles en esta sesión. No hace falta añadir una API key a Psicoapp para usarlas durante el trabajo de diseño.

No son dependencias de este proyecto React, Next.js, Vite como framework de aplicación, shadcn/ui, Stripe o Supabase. Se mantiene Angular y la API Spring existente.

## Verificación local de la configuración

- Las ocho carpetas de skills contienen su `SKILL.md` en la ubicación de usuario.
- La CLI 0.1.25 abrió `/ingreso` en Edge, obtuvo la estructura de la página y cerró únicamente su sesión de comprobación; los artefactos temporales se guardaron fuera del repositorio.
- `npm run build`: correcto. `npm test`: 28 pruebas funcionales aprobadas.
- `npm run test:visual:update` creó nueve referencias. Se inspeccionaron ingreso, biblioteca y modal en los tres tamaños; `npm run test:visual` comprobó las seis pruebas contra ellas, sin actualizar las imágenes.
- La conexión y los archivos de Figma no se han verificado: su instalación/autorización siguen pendientes. No se modificaron pantallas del producto ni archivos del backend como parte de esta configuración.
