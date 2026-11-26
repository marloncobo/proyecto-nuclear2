import { Workbook } from 'exceljs';
import type { FilaReporteSesion } from './servicio-api';

const columnas = [
  { titulo: 'Estudiante', width: 32 },
  { titulo: 'Correo institucional', width: 39 },
  { titulo: 'Estado', width: 19 },
  { titulo: 'Calificación', width: 17 },
  { titulo: 'Preguntas respondidas', width: 19 },
  { titulo: 'Preguntas totales', width: 18 },
  { titulo: 'Preguntas ausentes', width: 18 },
  { titulo: 'ID del estudiante', width: 40 },
  { titulo: 'ID del intento', width: 40 },
];

const estados: Record<string, { texto: string; fondo: string; color: string }> = {
  FINALIZADO: { texto: 'Finalizado', fondo: 'FFE4F0E8', color: 'FF24523C' },
  EN_PROGRESO: { texto: 'En progreso', fondo: 'FFE2EFF7', color: 'FF254F70' },
  VENCIDO: { texto: 'Vencido', fondo: 'FFF7ECDD', color: 'FF764919' },
  AUSENTE: { texto: 'Ausente', fondo: 'FFF7E6E4', color: 'FF873F38' },
  SIN_INTENTO: { texto: 'Sin intento', fondo: 'FFEDF0F1', color: 'FF53636D' },
};

const fechaColombia = new Intl.DateTimeFormat('es-CO', {
  dateStyle: 'medium',
  timeStyle: 'short',
  timeZone: 'America/Bogota',
});
const azul = 'FF143C59';
const texto = 'FF203748';
const filaEncabezados = 7;

export async function generarReporteSesionExcel(
  sesion: { id: string; caso: string; espacio: string; inicio: string; fin: string },
  filas: FilaReporteSesion[],
) {
  const libro = new Workbook();
  libro.creator = 'Psicoapp';
  const hoja = libro.addWorksheet('Participación', {
    properties: { tabColor: { argb: azul } },
    views: [{ state: 'frozen', xSplit: 1, ySplit: filaEncabezados, showGridLines: false }],
  });
  hoja.columns = columnas.map(({ width }) => ({ width }));
  hoja.getColumn(4).numFmt = '0.00';

  hoja.mergeCells('A1:I1');
  const titulo = hoja.getCell('A1');
  titulo.value = 'Reporte de participación';
  titulo.font = { name: 'Calibri', size: 16, bold: true, color: { argb: azul } };
  titulo.alignment = { vertical: 'middle', indent: 1 };
  hoja.getRow(1).height = 32;

  const datosSesion = [
    ['Caso', sesion.caso],
    ['Espacio académico', sesion.espacio],
    ['Horario previsto (Colombia)', `${fechaColombia.format(new Date(sesion.inicio))} – ${fechaColombia.format(new Date(sesion.fin))}`],
  ];
  datosSesion.forEach(([etiqueta, valor], indice) => {
    const numeroFila = indice + 2;
    hoja.mergeCells(numeroFila, 2, numeroFila, columnas.length);
    hoja.getCell(numeroFila, 1).value = etiqueta;
    hoja.getCell(numeroFila, 2).value = valor;
    const fila = hoja.getRow(numeroFila);
    fila.height = 22;
    fila.font = { name: 'Calibri', size: 11, color: { argb: texto } };
    fila.alignment = { vertical: 'middle', indent: 1 };
    hoja.getCell(numeroFila, 1).font = { ...fila.font, bold: true };
  });

  hoja.mergeCells('A5:I5');
  const nota = hoja.getCell('A5');
  nota.value = 'Una fila por intento, ausencia o estudiante sin intento. La nota queda en blanco cuando aún no está disponible.';
  nota.font = { name: 'Calibri', size: 11, italic: true, color: { argb: 'FF4B6269' } };
  nota.alignment = { vertical: 'middle', indent: 1 };
  hoja.getRow(5).height = 23;
  hoja.getRow(6).height = 8;

  const encabezados = hoja.getRow(filaEncabezados);
  encabezados.values = columnas.map(({ titulo }) => titulo);
  encabezados.height = 32;
  encabezados.font = { name: 'Calibri', size: 11, bold: true, color: { argb: 'FFFFFFFF' } };
  encabezados.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: azul } };
  encabezados.alignment = { vertical: 'middle', horizontal: 'center', wrapText: true };
  encabezados.eachCell((celda) => {
    celda.border = { right: { style: 'thin', color: { argb: 'FFFFFFFF' } } };
  });

  filas.forEach((registro, indice) => {
    const estado = Object.hasOwn(estados, registro.estado) ? estados[registro.estado] : undefined;
    const fila = hoja.addRow([
      registro.nombreEstudiante,
      registro.correoEstudiante,
      estado?.texto ?? registro.estado,
      registro.calificacion ?? null,
      registro.preguntasRespondidas ?? null,
      registro.preguntasTotales ?? null,
      registro.ausencia?.detalles.length ?? 0,
      registro.idEstudiante,
      registro.idIntento ?? null,
    ]);
    const lineas = Math.max(
      Math.ceil(registro.nombreEstudiante.length / 30),
      Math.ceil(registro.correoEstudiante.length / 37),
      1,
    );
    fila.height = Math.max(23, lineas * 14 + 4);
    for (let columna = 1; columna <= columnas.length; columna++) {
      const celda = fila.getCell(columna);
      celda.fill = {
        type: 'pattern',
        pattern: 'solid',
        fgColor: { argb: indice % 2 === 0 ? 'FFFFFFFF' : 'FFF1F6F2' },
      };
      celda.font = { name: 'Calibri', size: 11, color: { argb: texto } };
      celda.alignment = {
        vertical: 'middle',
        horizontal: columna >= 4 && columna <= 7 ? 'right' : 'left',
        wrapText: columna <= 2,
        indent: 1,
      };
    }
    fila.getCell(8).border = { left: { style: 'thin', color: { argb: 'FFD5E1DA' } } };
    if (estado) {
      const etiqueta = fila.getCell(3);
      etiqueta.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: estado.fondo } };
      etiqueta.font = { name: 'Calibri', size: 11, bold: true, color: { argb: estado.color } };
    }
  });

  if (filas.length) {
    hoja.autoFilter = {
      from: { row: filaEncabezados, column: 1 },
      to: { row: filaEncabezados + filas.length, column: columnas.length },
    };
  } else {
    hoja.mergeCells('A8:I8');
    hoja.getCell('A8').value = 'La sesión todavía no tiene participaciones para mostrar.';
    hoja.getRow(8).height = 23;
  }

  const bytes = await libro.xlsx.writeBuffer();
  const archivo = new Blob([new Uint8Array(bytes)], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  });
  const caso = sesion.caso.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase()
    .replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '').slice(0, 48);
  return { archivo, nombre: `participacion-${caso ? caso + '-' : ''}${sesion.id.slice(0, 8)}.xlsx` };
}
