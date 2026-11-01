# Psicoapp

Proyecto dividido en `backend/` y `frontend/`.

El backend usa Java 21, Spring Boot 4.1.2 y Maven. Su estructura es un
monolito modular con paquete base `com.psicoapp`.
El frontend está pendiente de implementación.

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

El servidor usa el puerto `8080`. Los módulos `identity` y `academic` exponen
la [API mínima protegida](backend/README.md#api-mínima) de usuarios y espacios académicos.

Para compilar, ejecutar las pruebas JUnit y de arranque y generar el JAR:

```powershell
cd backend
.\mvnw.cmd verify
java -jar target/psicoapp-0.0.1-SNAPSHOT.jar
```

Consulta la [arquitectura del backend](backend/README.md) antes de añadir funcionalidades.
