# Construccion de casos — primera entrega (Builder)

> Alcance historico: las [correcciones posteriores](correcciones-revision.md) actualizan las reglas y funciones descritas en esta entrega.

> Esta pagina registra el alcance de la primera entrega. La segunda agrega
> [validacion y publicacion con Template Method](casos-publicacion.md), sin
> cambiar que el Builder siempre crea borradores.

## Alcance

Esta entrega implementa la construccion y consulta de borradores de `cases`:
RF-05, RF-06, RF-07, RF-08 y RF-17; RN-02 y estructura de RN-11.
No declara completos los casos de uso de edicion (CU-04–CU-08): por ahora se
recibe el contenido en una sola creacion. No hay actualizacion de un caso
existente ni importacion/exportacion del DSL.

La segunda entrega implementara Template Method para validar y publicar
(RF-09/RN-03/RN-05). La tercera implementara State en `sessions`.
Un borrador de esta entrega no se puede publicar ni programar.

## Colaboracion y contratos

`ControladorCasos -> ServicioCreacionCaso -> ConstructorCaso -> Caso`.

- `ConstructorCasoAcademico` implementa los pasos `definirDatos`,
  `agregarEscenario`, `asociarRubrica` y `construir`. Cada solicitud usa su
  propia instancia; el resultado es inmutable.
- La aplicacion comprueba `DirectorioIdentidad.exigirCreacionCasos`, que
  reutiliza la comprobacion existente de usuario activo, rol docente y permiso
  `CREAR_CASOS` de `ServicioUsuarios`. El Builder no autoriza ni persiste.
- `RepositorioCasos` es el puerto propio; su adaptador JDBC inserta el
  agregado completo en una transaccion. No sobrescribe identificadores.
- La consulta exige docente activo y propiedad del borrador. Las opciones
  correctas y justificaciones solo se exponen al docente responsable.
- No se accede a repositorios internos de `identity` ni `academic`.
  El espacio academico no pertenece a la definicion reutilizable del caso;
  se asociara al programar la sesion, mediante el contrato publico de
  `academic`, segun RF-04/RF-18.

Se siguen las columnas y relaciones de `ModeloER.drawio`, incluida
`criterio_pregunta`. No se agregan puntajes por opcion ni versionado:
el DSL los menciona, pero su reconciliacion con el ER requiere una decision
de diseno antes de implementar publicacion/importacion y evaluacion.

## Invariantes del borrador

Se admiten textos vacios, escenarios sin preguntas, preguntas sin opciones y
ausencia de rubrica o de tiempo estimado. Si se especifica tiempo debe ser
positivo. Los textos se normalizan con `strip()`; los limites son 180
caracteres para titulos/nombre del caso, 120 para rubrica/criterios y 16000
para textos almacenados como TEXT, para caber en MySQL con utf8mb4.

Los UUID son obligatorios y unicos por tipo dentro del caso. Escenarios y
preguntas conservan su `orden` positivo y unico en su contenedor. Se permiten
huecos mientras se trabaja en borrador. Las colecciones sin columna de orden
en el ER (opciones, criterios y vinculos de preguntas) se normalizan por UUID;
su orden no expresa una regla de negocio.

Una rubrica presente exige escala maxima positiva; sus decimales y
ponderaciones no negativas deben caber en DECIMAL(5,2), sin redondeo silencioso.
No se impone aun suma de ponderaciones, minimo de opciones o contenido completo.
Las referencias de criterios deben apuntar a preguntas del mismo caso.
La validacion completa para publicar corresponde a la segunda entrega.

## API

Requiere el mismo HTTP Basic y token CSRF que identidad y espacios academicos.

- `POST /api/casos`: docente con permiso, responde 201 con el borrador creado.
- `GET /api/casos/{id}`: docente responsable, responde 200.
- 400: datos invalidos; 401: sin autenticacion; 403: rol, permiso, propiedad
  o CSRF; 404: caso inexistente; 409: conflicto de integridad.

El actor se toma del principal autenticado. No se aceptan del cliente el
identificador del caso, el propietario ni el estado del caso. Los UUID del
contenido anidado son aportados por el cliente para enlazar la rubrica.

Ejemplo de cuerpo de creacion:

```json
{
  "nombre": "Conflicto en un equipo",
  "desarrolloSituacional": "Un desacuerdo afecta la colaboracion.",
  "contexto": "Actividad educativa de psicologia social.",
  "tiempoEstimadoMin": 40,
  "escenarios": [{
    "id": "10000000-0000-0000-0000-000000000001",
    "titulo": "Reunion inicial",
    "contextoSituacional": "El equipo expone sus perspectivas.",
    "orden": 1,
    "preguntas": [{
      "id": "20000000-0000-0000-0000-000000000001",
      "enunciado": "Como abordar el desacuerdo?",
      "orden": 1,
      "opciones": [{
        "id": "30000000-0000-0000-0000-000000000001",
        "texto": "Escuchar las perspectivas",
        "esCorrecta": true,
        "justificacion": "Permite reconocer intereses."
      }]
    }]
  }],
  "rubrica": {
    "id": "40000000-0000-0000-0000-000000000001",
    "nombre": "Analisis del conflicto",
    "escalaMaxima": 5.00,
    "criterios": [{
      "id": "50000000-0000-0000-0000-000000000001",
      "nombre": "Reconocimiento de perspectivas",
      "ponderacion": 1.00,
      "preguntas": ["20000000-0000-0000-0000-000000000001"]
    }]
  }
}
```

El cuerpo anterior es un borrador, no una declaracion de validez para publicar.
Tambien se admite un borrador minimo: `{"escenarios":[]}`.

## Verificacion

Desde `backend/`: `.\mvnw.cmd verify`.

Resultado del 01-11-2026: 72 pruebas ejecutadas, cero fallos, cero errores y
cero omitidas (18 pruebas nuevas de `cases`); JAR generado. `git diff --check`
sin incidencias. Mensaje propuesto para el primer commit, pendiente de revision:
`feat(cases): construir borradores academicos con Builder`.

| Prueba | Requisitos / comportamiento |
| --- | --- |
| `PruebaConstructorCasoAcademico` | RF-05/06/07/08/17; RN-02/11: construccion, orden, aislamiento, duplicados, referencias y rangos |
| `PruebaServicioCreacionCaso` | RF-02/05: autorizacion antes de guardar, identidad del creador y consulta protegida |
| `PruebaControladorCasos` | RF-02/05/09: HTTP, rol, CSRF, propietario autenticado, errores y estado BORRADOR |
| `PruebaRepositorioCasosJdbc` | RF-05/06/07/08/17: agregado completo, tiempo omitido y rollback por fallo de un hijo |

Las pruebas JDBC ejecutan `schema.sql` sobre H2 en modo MySQL (dependencia
exclusiva de pruebas). No equivalen a una prueba de integracion con un servidor
MySQL real. Se mantiene MySQL como motor de produccion.

En la sesion de desarrollo, el entorno restringido impide la autoasociacion
del agente de Mockito. Para verificar se cargo el agente Byte Buddy ya
descargado mediante `-DargLine=-javaagent:<ruta-al-jar>`, sin cambiar la
configuracion habitual del proyecto. La cache Maven y los temporales se
ubicaron fuera del repositorio.
