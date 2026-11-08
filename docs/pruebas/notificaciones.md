# Pruebas del módulo de notificaciones

El módulo `notifications` cubre RF-20, RF-21 y RF-27 mediante un puerto de
canal (`CanalNotificaciones`) y un repositorio de notificaciones. Cada aviso
conserva destinatario, correo, tipo, asunto, mensaje, URL, código de acceso,
fecha y estado de entrega.

## Casos unitarios

| Caso | Regla | Resultado esperado |
| --- | --- | --- |
| `PruebaNotificacion.exigeLosDatosMinimos` | Datos obligatorios | Rechaza un correo vacío. |
| `PruebaNotificacion.conservaContenidoAlMarcarEntrega` | Estado de entrega | Cambia el estado sin perder el contenido. |
| `PruebaServicioNotificaciones.creaYMarcaComoEnviadaUnaConvocatoria` | RF-20/RF-21 | Persiste y marca enviada la convocatoria. |
| `PruebaServicioNotificaciones.impideConsultarNotificacionesDeOtroEstudiante` | Acceso | Solo el destinatario consulta sus avisos. |
| `PruebaServicioNotificaciones.conservaElFalloDelCanalYPropagaLaExcepcion` | Fallo de canal | Marca `FALLIDA` y no oculta el error. |

Ejecutar desde `backend/`:

```powershell
.\mvnw.cmd -Dtest="Prueba*Notificacion*" test
```

El adaptador actual registra la entrega mediante el logger y deja el puerto
listo para conectar SMTP u otro proveedor sin cambiar el dominio ni los casos
de uso.
