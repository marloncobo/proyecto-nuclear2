import { ApplicationConfig, inject, provideAppInitializer, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';

import { rutas } from './app.routes';
import { ServicioApi } from './servicio-api';

export const configuracionAplicacion: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(rutas),
    provideAppInitializer(() => inject(ServicioApi).restaurarSesion()),
  ],
};
