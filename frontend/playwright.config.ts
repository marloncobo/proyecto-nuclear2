import { defineConfig, devices } from '@playwright/test';
export default defineConfig({
  testDir: './pruebas',
  fullyParallel: false,
  use: { baseURL: 'http://localhost:3000', ...devices['Desktop Chrome'], channel: 'msedge' },
  webServer: {
    command: 'node node_modules/@angular/cli/bin/ng.js serve --host 127.0.0.1',
    url: 'http://localhost:3000',
    reuseExistingServer: !process.env['CI'],
    timeout: 120000,
  },
  reporter: 'list',
});
