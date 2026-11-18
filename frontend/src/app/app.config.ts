import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';

import { rutas } from './app.routes';

export const configuracionAplicacion: ApplicationConfig = {
  providers: [provideBrowserGlobalErrorListeners(), provideRouter(rutas)],
};
