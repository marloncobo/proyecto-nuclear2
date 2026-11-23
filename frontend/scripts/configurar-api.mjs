import { access, writeFile } from 'node:fs/promises';
import { resolve } from 'node:path';

const urlConfigurada = (process.env.PSICOAPP_API_URL ?? '').trim().replace(/\/$/, '');
if (!urlConfigurada) {
  console.error('Falta PSICOAPP_API_URL. Usa la URL publica HTTPS del backend en Railway.');
  process.exit(1);
}

let url;
try {
  url = new URL(urlConfigurada);
} catch {
  console.error('PSICOAPP_API_URL no es una URL valida.');
  process.exit(1);
}
if (url.protocol !== 'https:' || url.pathname !== '/' || url.search || url.hash) {
  console.error('PSICOAPP_API_URL debe ser un origen HTTPS sin ruta, consulta ni fragmento.');
  process.exit(1);
}

const destino = resolve('dist/psicoapp/browser/configuracion.js');
await access(destino).catch(() => {
  console.error('No se encontro el build de Angular. Ejecuta este script desde frontend/.');
  process.exit(1);
});
await writeFile(
  destino,
  `window.configuracionPsicoapp = ${JSON.stringify({ urlApi: url.origin })};\n`,
  'utf8',
);
console.log(`Frontend configurado para ${url.origin}`);
