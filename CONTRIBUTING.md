# Guía de contribución

Gracias por contribuir a Psicoapp. Esta guía describe cómo preparar el entorno,
organizar los cambios y enviarlos para revisión.

## Antes de comenzar

- Lee el [README principal](README.md) y la
  [arquitectura del backend](backend/README.md).
- Revisa los issues y pull requests existentes para evitar trabajo duplicado.
- Para cambios grandes, propone primero el problema, el alcance y la solución
  en un issue. Para corregir un error pequeño o mejorar documentación, puedes
  preparar directamente un pull request.

El proyecto está en una fase inicial: el backend incluye una base de usuarios,
roles, permisos y espacios académicos con MySQL, autenticación y pruebas JUnit.
El frontend está pendiente de implementación. Consulta el README del backend
para conocer el alcance implementado y las integraciones pendientes.

## Flujo de trabajo con Gitflow

El proyecto Psicoapp utiliza una adaptación simplificada de **Gitflow** para organizar el desarrollo colaborativo, revisar los cambios y mantener una versión estable del sistema.

### Ramas principales

- `main`: contiene las versiones estables del proyecto. Se actualiza mediante Pull Requests desde `develop`.
- `develop`: integra las funcionalidades, correcciones y documentación que han sido revisadas y aprobadas.

### Ramas de trabajo

Cada cambio debe desarrollarse en una rama creada a partir de `develop`.

Convenciones de nombres:

- `feat/*`: nuevas funcionalidades.
- `fix/*`: correcciones de errores.
- `docs/*`: actualizaciones de documentación.
- `refactor/*`: mejoras de código sin alterar el comportamiento.
- `test/*`: incorporación o modificación de pruebas.
- `chore/*`: tareas de configuración o mantenimiento.

Ejemplos:

`feat/autenticacion`

`feat/gestion-casos`

`fix/validacion-sesiones`

`docs/actualizar-requisitos`

### Crear una rama de trabajo

Antes de comenzar una tarea, actualiza la rama `develop`:

```bash
git switch develop
git pull origin develop
git switch -c feat/nombre-funcionalidad
```

Realiza los cambios, ejecuta las verificaciones correspondientes y registra los commits.

Para publicar la rama:

```bash
git push -u origin feat/nombre-funcionalidad
```

### Pull Requests hacia develop

Antes de crear un Pull Request, verifica que la rama de origen sea la rama de trabajo (feat/*, fix/*, etc.) y que la rama de destino sea develop. Los cambios de las ramas de trabajo no deben integrarse directamente en main.

Cuando una funcionalidad esté terminada:

1. Crea un Pull Request desde la rama de trabajo hacia `develop`.
2. Describe los cambios y relaciona el Issue correspondiente, si existe.
3. Verifica que las pruebas se ejecuten correctamente.
4. Solicita la revisión de otro integrante.
5. Corrige las observaciones de la revisión.
6. Integra el Pull Request cuando esté aprobado.

**No deben realizarse commits directamente en `develop` ni en `main`.**

Toda funcionalidad nueva o corrección de una regla de negocio deberá incluir pruebas unitarias con JUnit cuando exista lógica comprobable. Cada prueba deberá estar relacionada con el requisito o caso de prueba correspondiente al plan aprobado. Los Pull Requests hacia develop deberán indicar las pruebas ejecutadas y sus resultados.

### Pull Requests hacia main

Cuando el equipo considere que `develop` contiene una versión estable y lista para una entrega académica:

1. Verifica que las funcionalidades previstas para la entrega estén completas.
2. Ejecuta las pruebas correspondientes.
3. Crea un Pull Request desde `develop` hacia `main`.
4. Revisa los cambios incluidos en la versión.
5. Integra el Pull Request después de su aprobación.
6. Registra una etiqueta de versión cuando corresponda.

Las etiquetas de versión deben crearse después de integrar el Pull Request de develop hacia main, comprobando que la rama local main esté actualizada y apunte al commit correspondiente a la entrega.

Ejemplo:

```bash
git switch main
git pull origin main
git tag -a v0.1.0 -m "Primera entrega estable de Psicoapp"
git push origin v0.1.0
```

La etiqueta debe crearse sobre el commit de `main` que representa la versión entregada.

### Reglas de integración

- Ninguna rama de trabajo debe fusionarse directamente con `main`.
- Los cambios deben revisarse mediante Pull Requests.
- Los conflictos deben resolverse antes de integrar los cambios.
- Las ramas de trabajo deben mantenerse actualizadas con `develop` cuando sea necesario.
- Una vez integrado un Pull Request, la rama de trabajo puede eliminarse.
- Los cambios de arquitectura o requisitos deben reflejarse en la documentación correspondiente.

## Preparar el entorno

Necesitas Git, MySQL 8.0 o superior y JDK 21, con `JAVA_HOME` apuntando al JDK.
Configura la base de datos y las variables de entorno según el
[README del backend](backend/README.md#configuración-local). Usa el Maven Wrapper
incluido en `backend/`; no necesitas instalar Maven por separado. La primera
ejecución requiere acceso a internet para descargar Maven y las dependencias.

Si no tienes acceso de escritura al repositorio,
trabaja desde un fork y abre el pull request hacia el repositorio original.

Para arrancar el backend desde la raíz del repositorio:

**Windows (PowerShell):**

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

**Linux o macOS:**

```sh
cd backend
sh mvnw spring-boot:run
```

El servidor usa el puerto `8080`. La API protegida de identidad se publica bajo
`/api/identidad` y la de espacios académicos bajo `/api/academico/espacios`.

## Organización y convenciones

- Coloca el código del backend en `backend/src/main/java/com/psicoapp`, dentro
  del módulo funcional correspondiente: `identity`, `academic`, `cases`,
  `sessions`, `participation`, `evaluation`, `notifications` o `reports`.
- Respeta las capas: `api` recibe solicitudes y delega en `application`;
  `application` coordina casos de uso y depende de `domain`;
  `infrastructure` implementa los puertos e integraciones. Mantén `domain`
  independiente de Spring, HTTP y persistencia.
- Entre módulos, utiliza contratos públicos de `application` o eventos
  compartidos. Evita dependencias circulares y el acceso a detalles internos
  de otros módulos.
- Reserva `shared` para código transversal. No debe depender de módulos
  funcionales; el código exclusivo de un módulo permanece en ese módulo.
- Sigue el formato del código existente: paquetes en minúsculas, clases en
  `PascalCase`, métodos y variables en `camelCase`, y constantes en
  `UPPER_SNAKE_CASE`.
- Mantén cada cambio enfocado. Explica las dependencias nuevas y actualiza la
  documentación cuando cambien el arranque, la configuración o la arquitectura.
- Cuando implementes el frontend, documenta sus herramientas y comandos en
  `frontend/` y actualiza esta guía.

### Convención obligatoria de nombres

Los identificadores propios del proyecto deben estar en español: clases,
interfaces, enumeraciones, DTO, métodos, atributos, variables, pruebas, campos
JSON, rutas HTTP y claves de configuración propias. Usar `PascalCase` para
tipos y `camelCase` para métodos y variables, sin tildes ni letra ñ en los
identificadores; las constantes usan `UPPER_SNAKE_CASE` con palabras españolas.
Las clases de prueba nuevas se nombran `Prueba<Clase>` y Maven las descubre
mediante su configuración de Surefire.

Se conservan las firmas y símbolos impuestos por Java, Spring, JUnit, SQL y
otras dependencias, por ejemplo `authenticate`, `supports`, `toString` y
`spring.datasource.url`. También se conservan los nombres de paquetes y la
clase de arranque preexistentes de la arquitectura (`identity`, `academic`,
`application`, `domain`, `infrastructure`, `shared`, `PsicoappApplication`).
Estas excepciones de integración no autorizan nombres nuevos en inglés.
Los nombres técnicos de patrones como Facade y Builder pueden citarse en la
explicación, pero los identificadores se escriben en español: por ejemplo,
`FachadaAdministracion`, `ConstructorCaso` y `EstadoSesion`.

## Verificar los cambios

Coloca las pruebas en `backend/src/test/java/com/psicoapp`, reflejando los
módulos y capas del código que verifican. Añade pruebas de comportamiento para
funcionalidades nuevas y pruebas de regresión para errores corregidos.

Antes de enviar cambios de código del backend, ejecuta desde `backend/`:

**Windows (PowerShell):**

```powershell
.\mvnw.cmd verify
```

**Linux o macOS:**

```sh
sh mvnw verify
```

Este comando compila, ejecuta las pruebas y genera el JAR. Las pruebas actuales
comprueban reglas de usuarios y espacios académicos, autorización, autenticación,
contratos HTTP y carga del contexto de Spring. Las pruebas unitarias usan dobles
de los puertos y no requieren una instancia de MySQL.

Para cambios exclusivos de documentación, revisa los enlaces, los comandos y
la coherencia con el estado del proyecto. Antes de crear el commit, revisa
`git diff --check` y `git diff` para detectar espacios incorrectos y cambios
accidentales.

## Commits y pull requests

Escribe commits breves y descriptivos. Sigue el formato del historial reciente:

```text
tipo(alcance): descripción breve
```

Tipos sugeridos: `feat`, `fix`, `docs`, `test`, `refactor` y `chore`.
Ejemplo: `docs(backend): explicar las dependencias entre capas`.

En el pull request, incluye:

- El problema o necesidad y el resultado del cambio.
- El issue relacionado, si existe.
- Las verificaciones realizadas y sus resultados; indica si alguna no pudo
  ejecutarse y por qué.
- Los cambios de configuración, dependencias o pasos adicionales necesarios.
- Capturas de pantalla cuando haya cambios visuales.

Responde a los comentarios de revisión y mantén la conversación respetuosa
y centrada en el cambio.

## Reportar errores y proteger los datos

Para reportar un error, incluye los pasos para reproducirlo, el resultado
esperado, el resultado observado y el entorno utilizado. Adjunta registros
relevantes después de eliminar información sensible.

No incluyas contraseñas, tokens, credenciales ni datos personales o clínicos
reales en código, pruebas, capturas, logs o issues. Usa datos ficticios y
plantillas sin secretos para documentar la configuración local. No agregues
archivos generados, como `target/`, ni configuraciones personales del editor.

Si detectas una vulnerabilidad, comunícala en privado a los mantenedores por
un canal disponible del repositorio; evita publicar credenciales o detalles
explotables en un issue público.
