import { test as base } from '@playwright/test';

export { expect } from '@playwright/test';

export const test = base.extend({
  page: async ({ page }, use) => {
    await page.addInitScript(() => {
      const leer = Storage.prototype.getItem;
      Storage.prototype.getItem = function (clave: string) {
        const valor = leer.call(this, clave);
        return this === localStorage && clave === 'psicoapp.tutorial.oculto' ? '1' : valor;
      };
    });
    await use(page);
  },
});
