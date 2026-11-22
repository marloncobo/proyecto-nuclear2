import fs from "node:fs";
import path from "node:path";

const outputDirectory = path.resolve("docs/pruebas/postman");
fs.mkdirSync(outputDirectory, { recursive: true });

const jsonHeader = { key: "Content-Type", value: "application/json", type: "text" };

function request({ name, method = "GET", route, actor, body, expected = [200], tests = [], description = "" }) {
  const auth = actor === "public"
    ? { type: "noauth" }
    : { type: "bearer", bearer: [{ key: "token", value: `{{${actor}Token}}`, type: "string" }] };
  const accepted = JSON.stringify(expected);
  const testLines = [
    `const estadosEsperados = ${accepted};`,
    "pm.test(`Estado HTTP esperado (${estadosEsperados.join(', ')})`, () => {",
    "  pm.expect(estadosEsperados).to.include(pm.response.code);",
    "});",
    ...tests,
  ];
  return {
    name,
    request: {
      method,
      header: body === undefined ? [] : [jsonHeader],
      body: body === undefined ? undefined : { mode: "raw", raw: JSON.stringify(body, null, 2), options: { raw: { language: "json" } } },
      auth,
      url: `{{baseUrl}}${route}`,
      description,
    },
    event: [{ listen: "test", script: { type: "text/javascript", exec: testLines } }],
  };
}

const saveJson = (variable, expression = "respuesta.id") => [
  "const respuesta = pm.response.json();",
  `pm.collectionVariables.set('${variable}', ${expression});`,
];

const loginTests = (variable) => [
  "const respuesta = pm.response.json();",
  "pm.test('Entrega un JWT Bearer', () => {",
  "  pm.expect(respuesta.token).to.be.a('string').and.not.empty;",
  "  pm.expect(respuesta.tipo).to.eql('Bearer');",
  "  pm.expect(respuesta.venceEn).to.be.a('string');",
  "});",
  `pm.collectionVariables.set('${variable}', respuesta.token);`,
];

const identity = [
  request({
    name: "Ingresar como administrador",
    method: "POST",
    route: "/api/identidad/ingreso",
    actor: "public",
    body: { correo: "{{adminEmail}}", clave: "{{adminPassword}}" },
    tests: loginTests("adminToken"),
    description: "Inicia el recorrido automatizado. Configure adminEmail y adminPassword en el entorno.",
  }),
  request({ name: "Consultar mi identidad (administrador)", route: "/api/identidad/yo", actor: "admin", tests: ["pm.test('El actor es administrador', () => pm.expect(pm.response.json().rol).to.eql('ADMINISTRADOR'));"] }),
  request({ name: "Listar roles", route: "/api/identidad/roles", actor: "admin" }),
  request({ name: "Consultar permiso de creación de casos", route: "/api/identidad/roles/DOCENTE/creacion-casos", actor: "admin" }),
  request({ name: "Habilitar creación de casos para docentes", method: "PUT", route: "/api/identidad/roles/DOCENTE/creacion-casos", actor: "admin", body: { habilitado: true }, expected: [204] }),
  request({
    name: "Crear docente",
    method: "POST",
    route: "/api/identidad/docentes",
    actor: "admin",
    body: { correo: "{{teacherEmail}}", nombre: "Docente Postman", clave: "{{teacherPassword}}" },
    expected: [201],
    tests: saveJson("teacherId"),
  }),
  request({ name: "Listar usuarios con filtros", route: "/api/identidad/usuarios?rol=DOCENTE&activo=true&busqueda=Postman", actor: "admin" }),
  request({ name: "Consultar docente", route: "/api/identidad/usuarios/{{teacherId}}", actor: "admin" }),
  request({ name: "Editar docente", method: "PUT", route: "/api/identidad/docentes/{{teacherId}}", actor: "admin", body: { correo: "{{teacherEmail}}", nombre: "Docente Postman Actualizado" } }),
  request({ name: "Confirmar rol docente", method: "PUT", route: "/api/identidad/usuarios/{{teacherId}}/rol", actor: "admin", body: { rol: "DOCENTE" } }),
  request({ name: "Confirmar cuenta activa", method: "PUT", route: "/api/identidad/usuarios/{{teacherId}}/estado", actor: "admin", body: { activo: true } }),
  request({
    name: "Ingresar como docente",
    method: "POST",
    route: "/api/identidad/ingreso",
    actor: "public",
    body: { correo: "{{teacherEmail}}", clave: "{{teacherPassword}}" },
    tests: loginTests("teacherToken"),
  }),
  request({ name: "Consultar mi identidad (docente)", route: "/api/identidad/yo", actor: "teacher", tests: ["pm.test('El actor es docente', () => pm.expect(pm.response.json().rol).to.eql('DOCENTE'));"] }),
  request({
    name: "Registrar estudiante",
    method: "POST",
    route: "/api/identidad/estudiantes",
    actor: "teacher",
    body: { correo: "{{studentEmail}}", nombre: "Estudiante Postman" },
    expected: [201],
    tests: saveJson("studentId"),
  }),
  request({ name: "Buscar estudiante por correo", route: "/api/identidad/estudiantes?correo={{studentEmail}}", actor: "teacher" }),
  request({ name: "Listar estudiantes", route: "/api/identidad/estudiantes/listado", actor: "teacher" }),
];

const academic = [
  request({
    name: "Crear espacio académico",
    method: "POST",
    route: "/api/academico/espacios",
    actor: "teacher",
    body: { nombre: "Consultorio Postman", periodo: "2026-2" },
    expected: [201],
    tests: saveJson("spaceId"),
  }),
  request({ name: "Editar espacio académico", method: "PUT", route: "/api/academico/espacios/{{spaceId}}", actor: "teacher", body: { nombre: "Consultorio Postman Actualizado", periodo: "2026-2" } }),
  request({ name: "Listar espacios académicos", route: "/api/academico/espacios", actor: "teacher" }),
  request({ name: "Consultar espacio académico", route: "/api/academico/espacios/{{spaceId}}", actor: "teacher" }),
];

const caseBody = {
  nombre: "Caso clínico Postman",
  desarrolloSituacional: "Caso completo creado automáticamente desde Postman.",
  contexto: "Contexto académico controlado para validar el flujo integral.",
  tiempoEstimadoMin: 30,
  escenarios: [{
    id: "{{scenarioId}}",
    titulo: "Entrevista inicial",
    contextoSituacional: "La persona consulta por dificultades de adaptación.",
    orden: 1,
    preguntas: [
      {
        id: "{{questionOneId}}",
        enunciado: "¿Cuál es la primera acción profesional adecuada?",
        orden: 1,
        opciones: [
          { id: "{{correctOptionOneId}}", texto: "Explorar el motivo de consulta", esCorrecta: true, justificacion: "Permite delimitar la necesidad." },
          { id: "{{wrongOptionOneId}}", texto: "Emitir un diagnóstico inmediato", esCorrecta: false, justificacion: "No existe información suficiente." },
        ],
      },
      {
        id: "{{questionTwoId}}",
        enunciado: "¿Qué elemento debe registrarse después?",
        orden: 2,
        opciones: [
          { id: "{{correctOptionTwoId}}", texto: "Antecedentes relevantes", esCorrecta: true, justificacion: "Aporta contexto clínico." },
          { id: "{{wrongOptionTwoId}}", texto: "Suposiciones no verificadas", esCorrecta: false, justificacion: "No son evidencia clínica." },
        ],
      },
    ],
  }],
  rubrica: {
    id: "{{rubricId}}",
    nombre: "Rúbrica Postman",
    escalaMaxima: 5,
    criterios: [{ id: "{{criterionId}}", nombre: "Razonamiento profesional", ponderacion: 1, preguntas: ["{{questionOneId}}", "{{questionTwoId}}"] }],
  },
};

const cases = [
  request({ name: "Crear caso", method: "POST", route: "/api/casos", actor: "teacher", body: caseBody, expected: [201], tests: saveJson("caseId") }),
  request({ name: "Actualizar borrador", method: "PUT", route: "/api/casos/{{caseId}}", actor: "teacher", body: { ...caseBody, nombre: "Caso clínico Postman actualizado" } }),
  request({ name: "Listar casos", route: "/api/casos", actor: "teacher" }),
  request({ name: "Consultar caso", route: "/api/casos/{{caseId}}", actor: "teacher" }),
  request({ name: "Publicar caso", method: "POST", route: "/api/casos/{{caseId}}/publicar", actor: "teacher", tests: ["pm.test('El caso queda publicado', () => pm.expect(pm.response.json().estado).to.eql('PUBLICADO'));"] }),
];

const sessions = [
  request({
    name: "Programar sesión",
    method: "POST",
    route: "/api/sesiones",
    actor: "teacher",
    body: {
      idCaso: "{{caseId}}", idEspacio: "{{spaceId}}", inicioPrevisto: "{{sessionStart}}", finPrevisto: "{{sessionEnd}}",
      duracionMaximaMin: 30, modoNavegacion: "FLEXIBLE", lugar: "Consultorio 1", mensaje: "Sesión generada por Postman",
      estudiantes: ["{{studentId}}"],
    },
    expected: [201],
    tests: [
      "const respuesta = pm.response.json();",
      "pm.collectionVariables.set('sessionId', respuesta.id);",
      "pm.collectionVariables.set('studentPassword', respuesta.convocatorias[0].codigoAcceso);",
      "pm.test('Entrega el código de convocatoria una sola vez', () => pm.expect(respuesta.convocatorias[0].codigoAcceso).to.be.a('string').and.not.empty);",
    ],
  }),
  request({ name: "Listar sesiones", route: "/api/sesiones", actor: "teacher" }),
  request({ name: "Consultar sesión", route: "/api/sesiones/{{sessionId}}", actor: "teacher" }),
  request({ name: "Iniciar sesión", method: "POST", route: "/api/sesiones/{{sessionId}}/iniciar", actor: "teacher", tests: ["pm.test('La sesión queda en curso', () => pm.expect(pm.response.json().estado).to.eql('EN_CURSO'));"] }),
];

const participation = [
  request({
    name: "Ingresar como estudiante",
    method: "POST",
    route: "/api/identidad/ingreso",
    actor: "public",
    body: { correo: "{{studentEmail}}", clave: "{{studentPassword}}" },
    tests: loginTests("studentToken"),
  }),
  request({ name: "Consultar mi identidad (estudiante)", route: "/api/identidad/yo", actor: "student", tests: ["pm.test('El actor es estudiante', () => pm.expect(pm.response.json().rol).to.eql('ESTUDIANTE'));"] }),
  request({
    name: "Iniciar participación",
    method: "POST",
    route: "/api/participaciones",
    actor: "student",
    body: { idSesion: "{{sessionId}}", codigoAcceso: "{{studentPassword}}", preguntas: ["{{questionOneId}}", "{{questionTwoId}}"] },
    expected: [201],
    tests: saveJson("attemptId"),
  }),
  request({ name: "Ingresar a la simulación", method: "POST", route: "/api/participaciones/ingreso", actor: "student", body: { idSesion: "{{sessionId}}", codigoAcceso: "{{studentPassword}}" }, tests: ["pm.test('No expone claves de corrección', () => pm.expect(pm.response.text()).not.to.include('esCorrecta'));"] }),
  request({ name: "Consultar intento", route: "/api/participaciones/{{attemptId}}", actor: "student" }),
  request({ name: "Responder primera pregunta", method: "PUT", route: "/api/participaciones/{{attemptId}}/respuestas", actor: "student", body: { idPregunta: "{{questionOneId}}", idOpcion: "{{correctOptionOneId}}" } }),
  request({ name: "Avanzar a segunda pregunta", method: "POST", route: "/api/participaciones/{{attemptId}}/avanzar", actor: "student" }),
  request({ name: "Responder segunda pregunta", method: "PUT", route: "/api/participaciones/{{attemptId}}/respuestas", actor: "student", body: { idPregunta: "{{questionTwoId}}", idOpcion: "{{correctOptionTwoId}}" } }),
  request({ name: "Retroceder a primera pregunta", method: "POST", route: "/api/participaciones/{{attemptId}}/retroceder", actor: "student" }),
  request({ name: "Avanzar nuevamente", method: "POST", route: "/api/participaciones/{{attemptId}}/avanzar", actor: "student" }),
  request({ name: "Finalizar participación", method: "POST", route: "/api/participaciones/{{attemptId}}/finalizar", actor: "student", tests: ["pm.test('El intento queda finalizado', () => pm.expect(pm.response.json().estado).to.eql('FINALIZADO'));"] }),
];

const evaluation = [
  request({ name: "Solicitar evaluación idempotente", method: "POST", route: "/api/evaluaciones/{{attemptId}}", actor: "student", expected: [201], tests: ["pm.test('La evaluación contiene resultado', () => pm.expect(pm.response.json().resultado).to.be.an('object'));"] }),
  request({ name: "Consultar evaluación", route: "/api/evaluaciones/{{attemptId}}", actor: "student" }),
  request({ name: "Registrar valoración docente", method: "POST", route: "/api/evaluaciones/{{attemptId}}/valoraciones", actor: "teacher", body: { mensaje: "Buen razonamiento y adecuada priorización." }, expected: [201], tests: saveJson("valuationId") }),
  request({ name: "Consultar valoraciones como estudiante", route: "/api/evaluaciones/{{attemptId}}/valoraciones", actor: "student" }),
];

const reports = [
  request({ name: "Consultar reporte por caso y estudiante", route: "/api/reportes/casos/{{caseId}}/estudiantes/{{studentId}}", actor: "teacher" }),
  request({ name: "Consultar reporte de sesión", route: "/api/reportes/sesiones/{{sessionId}}", actor: "teacher" }),
  request({ name: "Registrar valoración desde reportes", method: "POST", route: "/api/reportes/participaciones/{{attemptId}}/valoraciones", actor: "teacher", body: { mensaje: "Valoración registrada desde el módulo de reportes." }, expected: [201] }),
];

const notificationsAndClosure = [
  request({
    name: "Listar notificaciones",
    route: "/api/notificaciones?limite=20&desplazamiento=0",
    actor: "student",
    tests: [
      "const avisos = pm.response.json();",
      "pm.test('La bandeja es un arreglo', () => pm.expect(avisos).to.be.an('array'));",
      "if (avisos.length > 0) pm.collectionVariables.set('notificationId', avisos[0].id);",
    ],
  }),
  request({
    name: "Marcar notificación como leída",
    method: "PUT",
    route: "/api/notificaciones/{{notificationId}}/leida",
    actor: "student",
    expected: [204, 404],
    description: "Devuelve 204 si el despachador interno ya creó un aviso; 404 si las notificaciones están deshabilitadas o aún no se han despachado.",
  }),
  request({ name: "Finalizar sesión", method: "POST", route: "/api/sesiones/{{sessionId}}/finalizar", actor: "teacher", tests: ["pm.test('La sesión queda finalizada', () => pm.expect(pm.response.json().estado).to.eql('FINALIZADO'));"] }),
  request({ name: "Archivar caso", method: "POST", route: "/api/casos/{{caseId}}/archivar", actor: "teacher", tests: ["pm.test('El caso queda archivado', () => pm.expect(pm.response.json().estado).to.eql('ARCHIVADO'));"] }),
];

const manualOperations = [
  request({
    name: "Modificar convocados",
    method: "PUT",
    route: "/api/sesiones/{{sessionId}}/convocados",
    actor: "teacher",
    body: { agregar: ["{{additionalStudentId}}"], retirar: [] },
    expected: [200, 400, 404, 409],
    description: "Operación manual: asigne additionalStudentId a otro estudiante activo y ejecútela antes de finalizar la sesión.",
  }),
  request({
    name: "Reanudar intento",
    method: "POST",
    route: "/api/participaciones/{{attemptId}}/reanudar",
    actor: "student",
    expected: [200, 409],
    description: "Operación manual para un intento suspendido. En el recorrido feliz el intento termina FINALIZADO y la API responde 409.",
  }),
];

const collectionVariables = [
  "adminToken", "teacherToken", "studentToken", "teacherId", "studentId", "additionalStudentId", "spaceId", "caseId",
  "sessionId", "attemptId", "valuationId", "notificationId", "studentPassword", "scenarioId", "questionOneId", "questionTwoId",
  "correctOptionOneId", "wrongOptionOneId", "correctOptionTwoId", "wrongOptionTwoId", "rubricId", "criterionId", "sessionStart", "sessionEnd",
].map((key) => ({ key, value: "", type: "string" }));

const initializationScript = [
  "const uuid = () => pm.variables.replaceIn('{{$guid}}');",
  "const initialize = pm.info.requestName === 'Ingresar como administrador' || !pm.collectionVariables.get('scenarioId');",
  "if (initialize) {",
  "  const suffix = `${Date.now()}-${Math.floor(Math.random() * 10000)}`;",
  "  const domain = pm.environment.get('institutionalDomain') || 'universidad.edu.co';",
  "  pm.collectionVariables.set('teacherEmail', `docente.postman.${suffix}@${domain}`);",
  "  pm.collectionVariables.set('studentEmail', `estudiante.postman.${suffix}@${domain}`);",
  "  ['scenarioId','questionOneId','questionTwoId','correctOptionOneId','wrongOptionOneId','correctOptionTwoId','wrongOptionTwoId','rubricId','criterionId','notificationId'].forEach(k => pm.collectionVariables.set(k, uuid()));",
  "  const now = new Date();",
  "  pm.collectionVariables.set('sessionStart', new Date(Math.floor((now.getTime() - 60000) / 1000) * 1000).toISOString());",
  "  pm.collectionVariables.set('sessionEnd', new Date(Math.floor((now.getTime() + 3600000) / 1000) * 1000).toISOString());",
  "}",
];

const collection = {
  info: {
    _postman_id: "c80ca1c6-4ba3-4b75-9e0c-a5c0a9922026",
    name: "Psicoapp API - develop",
    description: "Colección completa de la API disponible en la rama develop. El Runner ejecuta un recorrido integral con JWT y conserva los identificadores en variables de colección.",
    schema: "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
  },
  variable: collectionVariables,
  event: [{ listen: "prerequest", script: { type: "text/javascript", exec: initializationScript } }],
  item: [
    { name: "01 - Identidad y autenticación", item: identity },
    { name: "02 - Espacios académicos", item: academic },
    { name: "03 - Casos", item: cases },
    { name: "04 - Sesiones", item: sessions },
    { name: "05 - Participación", item: participation },
    { name: "06 - Evaluación y valoraciones", item: evaluation },
    { name: "07 - Reportes", item: reports },
    { name: "08 - Notificaciones y cierre", item: notificationsAndClosure },
    { name: "09 - Operaciones manuales", description: "Estas rutas requieren estados o datos adicionales y no forman parte del recorrido feliz.", item: manualOperations },
  ],
};

const environment = {
  id: "98695d89-69cb-4e54-aaf6-a5c0a9922026",
  name: "Psicoapp - local (develop)",
  values: [
    { key: "baseUrl", value: "http://localhost:8080", type: "default", enabled: true },
    { key: "adminEmail", value: "", type: "default", enabled: true },
    { key: "adminPassword", value: "", type: "secret", enabled: true },
    { key: "teacherPassword", value: "Postman-Teacher-2026!", type: "secret", enabled: true },
    { key: "institutionalDomain", value: "universidad.edu.co", type: "default", enabled: true },
  ],
  _postman_variable_scope: "environment",
  _postman_exported_at: new Date().toISOString(),
  _postman_exported_using: "Codex",
};

fs.writeFileSync(path.join(outputDirectory, "Psicoapp-develop.postman_collection.json"), `${JSON.stringify(collection, null, 2)}\n`);
fs.writeFileSync(path.join(outputDirectory, "Psicoapp-local.postman_environment.json"), `${JSON.stringify(environment, null, 2)}\n`);
console.log(`Colección generada con ${[identity, academic, cases, sessions, participation, evaluation, reports, notificationsAndClosure, manualOperations].flat().length} peticiones.`);
