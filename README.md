# Psicoapp

La guía para el despliegue provisional del backend y MySQL en Railway y del frontend
en Firebase Hosting está en [docs/despliegue.md](docs/despliegue.md).

Consulta las [correcciones vigentes y su validacion](docs/pruebas/correcciones-revision.md):
edicion y archivado de casos, convocatorias iniciales y reglas de inicio de sesiones.

Proyecto dividido en `backend/` y `frontend/`.

La configuración local vive en `.env`. Copia `.env.example` antes de arrancar el
backend y completa las credenciales y claves necesarias; `.env` está excluido de Git.

El backend usa Java 21, Spring Boot 4.1.2 y Maven. Su estructura es un
monolito modular con paquete base `com.psicoapp`.
El frontend para administradores, docentes y estudiantes usa Angular 21. Incluye un modo de demostración y recorridos conectados a la API. Consulta sus [comandos, funciones y límites](frontend/README.md).

El backend tambien incluye construccion/publicacion de casos y programacion
sesiones con State, participacion, evaluacion y notificaciones con Observer.
Las convocatorias se entregan por SMTP y los resultados/valoraciones mediante
avisos internos persistentes. Consulta la [configuracion de notificaciones](docs/pruebas/notificaciones.md)
antes de programar sesiones y los limites en el [README del backend](backend/README.md).

## Ejecutar el backend

Requisitos: JDK 21 o superior compatible con Spring Boot y `JAVA_HOME`
apuntando al JDK, y MySQL 8.0 o superior. Configura la base de datos y el
administrador inicial según la [guía del backend](backend/README.md#configuración-local).
El Maven Wrapper descarga Maven al usarlo por primera vez;
se necesita acceso a internet para descargar las herramientas y dependencias.

En PowerShell, desde la raíz del repositorio:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

En Linux o macOS:

```sh
cd backend
sh mvnw spring-boot:run
```

El servidor usa el puerto `8080`. Consulta la [referencia completa de endpoints](docs/endpoints.md)
para conocer rutas, roles, entradas y respuestas. Los módulos `identity` y
`academic` también se resumen en la [API mínima protegida](backend/README.md#api-mínima).

### Ejecutar con Docker

Docker Compose construye el backend con Java 21 y levanta una instancia MySQL 8.4
con volumen persistente. No requiere tener Java, Maven ni MySQL instalados en el host.

```powershell
Copy-Item .env.example .env
# Completa en .env el administrador y las claves que necesites.
docker compose up --build -d
docker compose logs -f backend
```

La API queda disponible en `http://localhost:8080`. Para detenerla sin borrar los
datos use `docker compose down`. Para eliminar también la base local use
`docker compose down --volumes`; esta última operación es destructiva.

Las credenciales `PSICOAPP_DOCKER_*` de la plantilla son exclusivamente locales.
En un despliegue real deben sustituirse y las claves JWT y de cifrado deben
inyectarse desde un gestor de secretos. Consulte la
[guía Docker del backend](backend/README.md#ejecución-con-docker).

Para compilar, ejecutar las pruebas JUnit y de arranque y generar el JAR:

```powershell
cd backend
.\mvnw.cmd verify
java -jar target/psicoapp-0.0.1-SNAPSHOT.jar
```

Consulta la [arquitectura del backend](backend/README.md) antes de añadir funcionalidades.
La [actualización de bases existentes](backend/README.md#actualizar-una-base-existente)
describe los cambios de esquema para el historial y los reintentos.

El [registro de aceptación técnica](docs/pruebas/aceptacion-mvp.md) incluye el caso
de demostración, el resultado observado y las decisiones pendientes.
