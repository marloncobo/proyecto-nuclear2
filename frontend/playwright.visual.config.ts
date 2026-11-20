import { defineConfig } from '@playwright/test';
import configuracionFuncional from './playwright.config';

export default defineConfig({
  ...configuracionFuncional,
  testDir: './pruebas-visuales',
  outputDir: './test-results/visual',
  workers: 1,
  retries: 0,
  reporter: [['list'], ['html', { outputFolder: 'playwright-report/visual', open: 'never' }]],
  snapshotPathTemplate: '{testDir}/referencias/{projectName}/{platform}/{arg}{ext}',
  expect: {
    toHaveScreenshot: { animations: 'disabled', caret: 'hide', maxDiffPixelRatio: 0.005 },
  },
  use: {
    ...configuracionFuncional.use,
    locale: 'es-CO',
    timezoneId: 'America/Bogota',
    colorScheme: 'light',
    reducedMotion: 'reduce',
    deviceScaleFactor: 1,
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
    video: 'off',
  },
  projects: [
    { name: 'escritorio', use: { viewport: { width: 1440, height: 1000 } } },
    { name: 'tableta', use: { viewport: { width: 768, height: 1024 }, hasTouch: true } },
    {
      name: 'movil',
      use: { viewport: { width: 390, height: 844 }, isMobile: true, hasTouch: true },
    },
  ],
});
