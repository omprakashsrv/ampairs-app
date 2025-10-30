import {defineConfig} from 'cypress'

export default defineConfig({
  e2e: {
    baseUrl: 'http://localhost:4200',
    specPattern: 'cypress/e2e/**/*.cy.{js,jsx,ts,tsx}',
    supportFile: 'cypress/support/e2e.ts',
    viewportWidth: 1280,
    viewportHeight: 720,
    video: true,
    screenshotOnRunFailure: true,
    defaultCommandTimeout: 10000,
    requestTimeout: 15000,
    responseTimeout: 15000,
    pageLoadTimeout: 30000,
    setupNodeEvents(on, config) {
      // implement node event listeners here
      on('task', {
        log(message) {
          console.log(message)
          return null
        },
        table(message) {
          console.table(message)
          return null
        }
      })
    },
  },
  env: {
    // Backend API URL for testing
    apiUrl: 'http://localhost:8080',
    // Test credentials and configuration
    testMobileNumber: '9876543210',
    testOtp: '123456',
    recaptchaEnabled: false,
    // Database connection for test data cleanup
    dbConfig: {
      host: 'localhost',
      port: 3306,
      user: 'root',
      password: 'pass',
      database: 'munsi_app'
    }
  },
  retries: {
    runMode: 2,
    openMode: 0
  }
})
