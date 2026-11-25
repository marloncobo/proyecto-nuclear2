import type { PasoTutorial } from './tutorial-guiado';

function paso(objetivo: string, titulo: string, descripcion: string): PasoTutorial {
  return { objetivo, titulo, descripcion };
}

const menuDocente = paso('#navegacion-principal, #alternar-menu', 'Menú docente',
  'Desde aquí puedes abrir la biblioteca, las sesiones, los espacios académicos, los resultados y las notificaciones.');
const cuenta = paso('.acciones-cabecera .boton-texto', 'Mi cuenta',
  'Consulta tu cuenta y el rol con el que ingresaste.');
const menuEstudiante = paso('.navegacion nav', 'Menú estudiantil',
  'Abre tus actividades, tus resultados o tus notificaciones desde estas opciones.');
const cuentaEstudiante = paso('.mi-cuenta', 'Mi cuenta',
  'Consulta tu correo y rol, o cierra sesión cuando termines de usar Psicoapp.');

export const pasosIngreso = [
  paso('.formulario-ingreso form, .ingreso-estudiante form', 'Acceso a Psicoapp',
    'Ingresa con las credenciales de tu cuenta. Si es tu primer acceso con una clave temporal, el sistema te pedirá cambiarla.'),
  paso('.formulario-ingreso .boton-ingresar', 'Entrar a mi espacio',
    'Abre el espacio que corresponde a tu cuenta después de validar tus datos.'),
  paso('[data-tutorial="demostracion"]', 'Explorar la demostración',
    'Prueba el espacio docente con datos de ejemplo antes de iniciar sesión.'),
];

export const pasosAplicacion: Record<string, readonly PasoTutorial[]> = {
  inicio: [
    menuDocente,
    paso('[data-tutorial="crear-caso"]', 'Crear un caso', 'Abre el editor para redactar un caso, sus preguntas y su rúbrica.'),
    paso('.paneles-inicio', 'Resumen del espacio', 'Consulta el estado de tus casos y encuentros recientes.'),
    paso('[data-tutorial="registrar-estudiante"]', 'Registrar estudiante', 'Crea una cuenta que luego podrás seleccionar en grupos y sesiones.'),
    cuenta,
  ],
  casos: [
    paso('[data-tutorial="crear-caso"]', 'Crear un caso', 'Abre el editor para guardar un borrador nuevo.'),
    paso('.filtros', 'Filtrar casos', 'Limita la biblioteca al estado que quieres consultar.'),
    paso('.rejilla-casos .tarjeta-caso', 'Explorar caso', 'Haz clic en cualquier parte de la tarjeta para abrir los detalles y las acciones del caso.'),
  ],
  sesiones: [
    paso('.encabezado-pagina .primario', 'Programar sesión', 'Elige un caso publicado, un espacio académico, los horarios y a quiénes quieres convocar.'),
    paso('.filtros-listado', 'Buscar y filtrar', 'Encuentra una sesión por caso, lugar, estado o espacio.'),
    paso('.abrir-detalle-sesion', 'Detalle de sesión', 'Consulta apertura, cierre, participación, aciertos y notas.'),
    paso('.tarjeta-sesion > .boton', 'Gestionar la sesión', 'Según su estado, puedes editar los convocados, iniciar o finalizar el encuentro.'),
    paso('.paginacion-sesiones', 'Cambiar de página', 'Usa Anterior y Siguiente para recorrer la lista de cinco en cinco.'),
  ],
  espacios: [
    paso('.encabezado-espacios .primario', 'Nuevo espacio', 'Crea un grupo con nombre, período y estudiantes predeterminados.'),
    paso('.filtros-espacios', 'Buscar espacios', 'Filtra los grupos por nombre o período.'),
    paso('.acciones-espacio .primario', 'Programar un encuentro', 'Abre una sesión nueva con este espacio y sus estudiantes preseleccionados.'),
    paso('.acciones-espacio .secundario', 'Editar espacio', 'Cambia los datos permitidos y la lista predeterminada para futuros encuentros.'),
  ],
  resultados: [
    paso('.panel-reporte', 'Consultar resultados', 'Elige una sesión para consultar el progreso, las notas y el detalle de cada intento.'),
    paso('.filtros-resultados', 'Filtrar resultados', 'Busca un estudiante o limita el reporte por su estado.'),
    paso('.lista-reporte-sesion', 'Acciones del reporte', 'Abre intentos, añade valoraciones o autoriza reintentos cuando estén disponibles.'),
  ],
  notificaciones: [
    paso('.panel-notificaciones', 'Notificaciones del docente', 'Aquí llegan los avisos sobre tus sesiones y resultados.'),
    paso('.filtros-notificaciones', 'Filtrar avisos', 'Elige qué tipos de avisos o qué estados de lectura quieres ver.'),
    paso('.lista-notificaciones', 'Gestionar avisos', 'Abre los avisos disponibles o márcalos como leídos.'),
  ],
  administracion: [
    paso('[data-tutorial="crear-perfil"]', 'Crear perfil', 'Abre el formulario para registrar a un docente o estudiante.'),
    paso('#actualizar-usuarios', 'Actualizar lista', 'Vuelve a consultar las cuentas y su estado actual.'),
    paso('.filtros', 'Buscar y filtrar cuentas', 'Busca por nombre o correo y combina los filtros de rol y estado.'),
    paso('.cuenta, #titulo-administracion', 'Administrar cuentas', 'Abre una cuenta para ver sus datos, editar lo permitido o cambiar su rol o estado.'),
    paso('#titulo-permisos', 'Roles y permisos', 'Concede o retira al rol docente el permiso para crear casos.'),
    cuenta,
  ],
};

export const pasosModales: Record<string, readonly PasoTutorial[]> = {
  crear: [
    paso('.modal psico-editor-caso form > label', 'Datos del caso', 'Identifica el caso y describe la situación que analizarán los estudiantes.'),
    paso('.modal .escenario', 'Escenarios y preguntas', 'Abre un escenario para editar su contexto, preguntas, opciones y justificaciones.'),
    paso('.modal .suma-ponderaciones', 'Rúbrica', 'Asigna preguntas a cada criterio y comprueba que las ponderaciones sumen 1.'),
    paso('[data-tutorial="reiniciar-borrador"]', 'Empezar de cero', 'Borra el borrador local y reinicia el formulario después de que confirmes.'),
    paso('.modal psico-editor-caso button.primario', 'Guardar borrador', 'Guarda el caso en la biblioteca para seguir editándolo o publicarlo después.'),
  ],
  programar: [
    paso('.modal select[name="casoSesion"]', 'Caso publicado', 'Selecciona la actividad que resolverán los estudiantes.'),
    paso('.modal select[name="espacioSesion"]', 'Espacio académico', 'Elige el grupo para precargar sus estudiantes predeterminados.'),
    paso('.modal psico-selector-estudiantes', 'Convocatoria', 'Marca o desmarca estudiantes para esta sesión.'),
    paso('.modal .dos-columnas', 'Horarios y navegación', 'Define la ventana de ingreso, el tiempo por intento y el modo de navegación.'),
    paso('.modal button.primario', 'Programar sesión', 'Guarda el encuentro y confirma la lista de convocados.'),
  ],
  espacio: [
    paso('.modal form > label', 'Datos del espacio', 'Define el nombre del grupo y el período.'),
    paso('.modal psico-selector-estudiantes', 'Estudiantes predeterminados', 'Marca qué estudiantes deben aparecer preseleccionados al programar un encuentro con este grupo.'),
    paso('.modal form .primario', 'Guardar espacio', 'Guarda la configuración para los próximos encuentros.'),
  ],
  'alta-estudiante': [
    paso('.modal form', 'Registrar estudiante', 'Escribe el nombre, correo institucional y contraseña inicial de la cuenta.'),
    paso('.modal form .primario', 'Crear estudiante', 'Guarda la cuenta para que aparezca al elegir estudiantes en los grupos y sesiones.'),
  ],
  'detalle-sesion': [
    paso('.horarios-sesion', 'Horarios de la sesión', 'Compara los horarios programados con la apertura y el cierre reales cuando estén registrados.'),
    paso('.modal-detalle-sesion input[type="search"]', 'Buscar estudiante', 'Filtra los intentos por el nombre o el correo del estudiante.'),
    paso('.tabla-detalle-sesion', 'Participación', 'Consulta la hora de inicio y fin de cada intento, sus aciertos y la nota.'),
  ],
  convocados: [
    paso('.modal psico-selector-estudiantes', 'Editar convocados', 'Marca estudiantes para agregar a la sesión y desmarca a quienes quieras retirar.'),
    paso('.modal form .primario', 'Guardar convocatoria', 'Confirma los cambios para este encuentro.'),
  ],
  detalle: [
    paso('.cabecera-caso-detalle > #titulo-modal', 'Detalle del caso', 'Consulta el desarrollo, los escenarios, las preguntas y la rúbrica del caso.'),
    paso('.modal-detalle-caso .boton.primario', 'Acciones del caso', 'Según su estado, puedes editarlo, publicarlo o programar una sesión.'),
  ],
  archivar: [paso('.modal .primario', 'Confirmar archivado', 'Archiva el caso para impedir que se use en nuevas sesiones. Las sesiones existentes se conservan.')],
  'iniciar-sesion': [paso('.modal .primario', 'Confirmar inicio', 'Abre el encuentro para que los convocados puedan iniciar su intento.')],
  'finalizar-sesion': [paso('.modal .primario', 'Confirmar finalización', 'Cierra el encuentro e impide que se inicien nuevos intentos.')],
  ingreso: [paso('.modal .secundario', 'Cerrar sesión', 'Sal de tu cuenta para volver a la pantalla de ingreso.')],
};

export const pasosModalesAdministracion: Record<string, readonly PasoTutorial[]> = {
  detalle: [paso('#detalle-usuario', 'Detalle del usuario', 'Revisa los datos de la cuenta. Desde aquí puedes editar los campos permitidos y gestionar su rol o estado.')],
  registro: [paso('select[name="rolPerfil"]', 'Elegir el rol', 'Elige Docente o Estudiante y completa el nombre, correo institucional y contraseña inicial.')],
  estado: [paso('#detalle-usuario', 'Cambiar estado', 'Confirma si quieres habilitar o deshabilitar el acceso a la cuenta.')],
  rol: [paso('#detalle-usuario', 'Cambiar rol', 'Confirma qué permisos tendrá esta cuenta al ingresar.')],
};

export const pasosEstudiante: Record<string, readonly PasoTutorial[]> = {
  actividades: [
    menuEstudiante,
    paso('.resumen', 'Pendientes y finalizadas', 'Consulta cuántas actividades tienes pendientes y cuántos intentos has terminado.'),
    paso('#titulo-actividades, .tarjeta-actividad button', 'Mis actividades', 'Inicia o continúa tus simulaciones disponibles y abre los resultados de las que terminaste.'),
    cuentaEstudiante,
  ],
  simulacion: [
    paso('.cabecera-simulacion .enlace', 'Volver a mis actividades', 'Regresa a tu lista de actividades. Las respuestas enviadas quedan guardadas.'),
    paso('[role="timer"]', 'Tiempo restante', 'Consulta el tiempo que queda para entregar tu intento.'),
    paso('.simulador > aside', 'Progreso', 'Consulta cuántas preguntas has respondido y en cuál estás.'),
    paso('.simulador fieldset', 'Responder', 'Marca la opción que elijas. La respuesta se guarda al seleccionarla.'),
    paso('.simulador footer', 'Avanzar o entregar', 'Usa los botones de navegación disponibles y pulsa Entregar para finalizar el intento.'),
  ],
  resultados: [
    menuEstudiante,
    paso('.tarjeta-resultado, .encabezado-pagina', 'Mis resultados', 'Consulta un intento y abre su retroalimentación para revisar tus respuestas y la valoración docente.'),
    paso('.detalle-resultado', 'Retroalimentación', 'Revisa tus aciertos, las explicaciones y la nota de este intento.'),
  ],
  notificaciones: [
    menuEstudiante,
    paso('.panel-notificaciones', 'Avisos', 'Consulta convocatorias, resultados y comentarios. Puedes marcar como leído cada aviso pendiente.'),
  ],
};
