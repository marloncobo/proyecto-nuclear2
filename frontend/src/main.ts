import { bootstrapApplication } from '@angular/platform-browser';
import { configuracionAplicacion } from './app/app.config';
import { Aplicacion } from './app/app';
bootstrapApplication(Aplicacion, configuracionAplicacion).catch((error) => console.error(error));
