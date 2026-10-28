# Arquitectura del backend

Un único proyecto Maven y una aplicación Spring Boot, organizados por módulos
funcionales. `PsicoappApplication` está en el paquete raíz para que Spring
descubra los componentes de todos los módulos.

```text
src/main/java/com/psicoapp/
├── PsicoappApplication.java
├── identity/
├── academic/
├── cases/
├── sessions/
├── participation/
├── evaluation/
├── notifications/
├── reports/
└── shared/
    ├── configuration/
    ├── security/
    ├── exceptions/
    └── events/
```

Cada uno de los ocho módulos funcionales contiene estas cuatro capas:

```text
<modulo>/
├── api/
├── application/
├── domain/
└── infrastructure/
```

Los archivos `package-info.java` documentan las responsabilidades y permiten
versionar los paquetes incluso antes de añadir sus primeras clases.

## Responsabilidades

| Módulo | Responsabilidad prevista |
| --- | --- |
| `identity` | Usuarios, roles e identidad |
| `academic` | Organización académica |
| `cases` | Gestión de casos |
| `sessions` | Sesiones de atención |
| `participation` | Participación y asistencia |
| `evaluation` | Evaluaciones e instrumentos |
| `notifications` | Envío de notificaciones |
| `reports` | Generación y consulta de reportes |

Estos límites son una base de organización; los casos de uso se concretarán
con los requisitos del proyecto.

## Capas y dependencias

- `api`: controladores HTTP, DTO y validación de solicitudes. Delega en
  `application`; no accede directamente a persistencia.
- `application`: casos de uso y coordinación de operaciones. Depende de
  `domain` y define los puertos de salida necesarios para sus casos de uso.
- `domain`: entidades, objetos de valor y reglas de negocio. No depende de
  Spring, HTTP, persistencia ni de las otras capas del módulo.
- `infrastructure`: adaptadores de persistencia e integraciones. Implementa
  los puertos y depende de `application` y `domain`.

El flujo habitual es `api -> application -> domain`. Los adaptadores de
`infrastructure` se conectan a través de interfaces, mediante inyección de
dependencias. Las interfaces que expresan conceptos propios del dominio pueden
pertenecer a `domain`.

Entre módulos, consumir contratos públicos de `application` o eventos
compartidos; evitar acceder a entidades internas y adaptadores de otro módulo.
Mantener las dependencias entre módulos sin ciclos.
Estas son convenciones de desarrollo; todavía no hay validación automática de
las dependencias entre capas.

## Código transversal

- `shared.configuration`: configuración común y ensamblado de componentes.
- `shared.security`: mecanismos de seguridad comunes. Los usuarios y roles
  pertenecen a `identity`.
- `shared.exceptions`: contratos de error y traducción común a respuestas HTTP.
- `shared.events`: contratos de eventos usados por varios módulos.

`shared` no debe depender de los módulos funcionales. El código específico de
un único módulo debe permanecer en ese módulo.

## Estado inicial

La base incluye Spring MVC, validación, Maven Wrapper y una prueba de carga del
contexto de Spring. La configuración está en `src/main/resources/application.properties`.
No se han definido base de datos, autenticación, entidades, endpoints ni reglas
de negocio. El paquete `shared.security` reserva su ubicación; aún no configura
protección de acceso.

Las pruebas se ubican en `src/test/java/com/psicoapp`, siguiendo los mismos
módulos y capas cuando se incorporen funcionalidades. Las instrucciones de
arranque y compilación están en el [README principal](../README.md).
