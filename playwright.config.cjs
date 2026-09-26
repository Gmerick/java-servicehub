const { defineConfig } = require("@playwright/test");
module.exports = defineConfig({
  testDir: "./e2e",
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [["list"], ["html", { open: "never" }]],
  use: {
    launchOptions: process.env.CHROME_PATH
      ? { executablePath: process.env.CHROME_PATH }
      : {},
    baseURL: "http://127.0.0.1:18083",
    viewport: { width: 1440, height: 1000 },
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  webServer: {
    command:
      'java -jar target/app.jar --server.port=18083 "--spring.datasource.url=jdbc:h2:mem:browser;DB_CLOSE_DELAY=-1"',
    url: "http://127.0.0.1:18083/api/health",
    reuseExistingServer: false,
    timeout: 90000,
  },
});
