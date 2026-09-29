const { defineConfig } = require("@playwright/test");
const auth = require('./target/test-auth.json');
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
    baseURL: "http://127.0.0.1:18085",
    viewport: { width: 1440, height: 1000 },
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  webServer: {
    command:
      'java -jar target/app.jar --server.port=18085 "--spring.datasource.url=jdbc:h2:mem:browser;DB_CLOSE_DELAY=-1" --app.demo=true --app.public-demo=true --app.login.max-attempts=200',
    url: "http://127.0.0.1:18085/api/health",
    env: { ADMIN_USERNAME: auth.username, ADMIN_PASSWORD_HASH: auth.hash },
    reuseExistingServer: false,
    timeout: 90000,
  },
});
