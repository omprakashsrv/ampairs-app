/// <reference types="cypress" />

// ***********************************************
// This example commands.ts shows you how to
// create various custom commands and overwrite
// existing commands.
//
// For more comprehensive examples of custom
// commands please read more here:
// https://on.cypress.io/custom-commands
// ***********************************************

declare global {
  namespace Cypress {
    interface Chainable {
      /**
       * Custom command to log in with mobile number and OTP
       * @example cy.login('9876543210', '123456')
       */
      login(mobileNumber: string, otp?: string): Chainable<void>

      /**
       * Custom command to intercept and mock API calls
       * @example cy.mockAuthApi()
       */
      mockAuthApi(): Chainable<void>

      /**
       * Custom command to clear authentication data
       * @example cy.clearAuth()
       */
      clearAuth(): Chainable<void>

      /**
       * Custom command to wait for Angular to be ready
       * @example cy.waitForAngular()
       */
      waitForAngular(): Chainable<void>

      /**
       * Custom command to fill mobile number input
       * @example cy.fillMobileNumber('9876543210')
       */
      fillMobileNumber(mobileNumber: string): Chainable<void>

      /**
       * Custom command to fill OTP input
       * @example cy.fillOtp('123456')
       */
      fillOtp(otp: string): Chainable<void>

      /**
       * Custom command to verify authentication state
       * @example cy.verifyAuthenticated()
       */
      verifyAuthenticated(): Chainable<void>

      /**
       * Custom command to verify not authenticated state
       * @example cy.verifyNotAuthenticated()
       */
      verifyNotAuthenticated(): Chainable<void>

      /**
       * Custom command to mock successful OTP generation for testing
       * @example cy.mockOtpGeneration('123456')
       */
      mockOtpGeneration(otp: string): Chainable<void>

      /**
       * Custom command to setup test database state
       * @example cy.setupTestData()
       */
      setupTestData(): Chainable<void>

      /**
       * Custom command to cleanup test data
       * @example cy.cleanupTestData()
       */
      cleanupTestData(): Chainable<void>
    }
  }
}

/**
 * Complete login flow with mobile number and OTP
 */
Cypress.Commands.add('login', (mobileNumber: string, otp: string = '123456') => {
  cy.log(`Logging in with mobile number: ${mobileNumber}`)

  // Visit login page
  cy.visit('/login')
  cy.waitForAngular()

  // Fill mobile number and submit
  cy.fillMobileNumber(mobileNumber)
  cy.get('[data-cy="login-submit-btn"]').click()

  // Wait for navigation to OTP page
  cy.url().should('include', '/verify-otp')
  cy.waitForAngular()

  // Fill OTP and submit
  cy.fillOtp(otp)

  // Wait for successful authentication
  cy.url().should('include', '/home')
  cy.verifyAuthenticated()
})

/**
 * Mock API endpoints for testing
 */
Cypress.Commands.add('mockAuthApi', () => {
  const apiUrl = Cypress.env('apiUrl')

  // Mock auth init endpoint
  cy.intercept('POST', `${apiUrl}/auth/v1/init`, {
    statusCode: 200,
    body: {
      success: true,
      sessionId: 'test-session-id-' + Date.now()
    }
  }).as('authInit')

  // Mock OTP verification endpoint
  cy.intercept('POST', `${apiUrl}/auth/v1/verify`, {
    statusCode: 200,
    body: {
      access_token: 'mock-access-token-' + Date.now(),
      refresh_token: 'mock-refresh-token-' + Date.now()
    }
  }).as('otpVerify')

  // Mock user profile endpoint
  cy.intercept('GET', `${apiUrl}/user/v1`, {
    statusCode: 200,
    body: {
      id: 'test-user-id',
      mobileNumber: Cypress.env('testMobileNumber'),
      name: 'Test User',
      email: 'test@example.com'
    }
  }).as('userProfile')

  cy.log('API endpoints mocked successfully')
})

/**
 * Clear all authentication data
 */
Cypress.Commands.add('clearAuth', () => {
  cy.clearCookies()
  cy.clearLocalStorage()
  cy.window().then((win) => {
    win.sessionStorage.clear()
  })
  cy.log('Authentication data cleared')
})

/**
 * Wait for Angular to be ready
 */
Cypress.Commands.add('waitForAngular', () => {
  cy.window().then((win: any) => {
    if (win.getAllAngularTestabilities) {
      return new Cypress.Promise((resolve) => {
        const testabilities = win.getAllAngularTestabilities()
        if (!testabilities || testabilities.length === 0) {
          resolve(undefined)
          return
        }

        let count = testabilities.length
        testabilities.forEach((testability: any) => {
          testability.whenStable(() => {
            count--
            if (count === 0) {
              resolve(undefined)
            }
          })
        })
      })
    }
  })
})

/**
 * Fill mobile number input with validation
 */
Cypress.Commands.add('fillMobileNumber', (mobileNumber: string) => {
  cy.get('[data-cy="mobile-number-input"]')
    .clear()
    .type(mobileNumber)
    .should('have.value', mobileNumber)

  // Verify form validation
  if (mobileNumber.length === 10 && /^[6-9]\d{9}$/.test(mobileNumber)) {
    cy.get('[data-cy="mobile-number-input"]').should('not.have.class', 'ng-invalid')
  }
})

/**
 * Fill OTP input with validation
 */
Cypress.Commands.add('fillOtp', (otp: string) => {
  cy.get('[data-cy="otp-input"]')
    .clear()
    .type(otp)
    .should('have.value', otp)

  // Verify form validation
  if (otp.length === 6 && /^\d{6}$/.test(otp)) {
    cy.get('[data-cy="otp-input"]').should('not.have.class', 'ng-invalid')
  }
})

/**
 * Verify user is authenticated
 */
Cypress.Commands.add('verifyAuthenticated', () => {
  // Check for access token in cookies
  cy.getCookie('access_token').should('exist')

  // Check current URL is not login page
  cy.url().should('not.include', '/login')
  cy.url().should('not.include', '/verify-otp')

  cy.log('User authentication verified')
})

/**
 * Verify user is not authenticated
 */
Cypress.Commands.add('verifyNotAuthenticated', () => {
  // Check for absence of access token
  cy.getCookie('access_token').should('not.exist')

  cy.log('User not authenticated - verified')
})

/**
 * Mock OTP generation in backend for testing
 */
Cypress.Commands.add('mockOtpGeneration', (otp: string) => {
  const apiUrl = Cypress.env('apiUrl')

  // Intercept notification queue endpoint to capture OTP
  cy.intercept('POST', `${apiUrl}/notification/v1/queue`, (req) => {
    // Extract OTP from message if needed for verification
    cy.log(`OTP generated: ${otp}`)
    req.reply({
      statusCode: 200,
      body: {
        success: true,
        notificationId: 'test-notification-id'
      }
    })
  }).as('otpGeneration')
})

/**
 * Setup test data in database
 */
Cypress.Commands.add('setupTestData', () => {
  cy.task('log', 'Setting up test data...')

  // Clean existing test data first
  cy.cleanupTestData()

  // Setup can be extended to create specific test users, etc.
  cy.task('log', 'Test data setup completed')
})

/**
 * Cleanup test data from database
 */
Cypress.Commands.add('cleanupTestData', () => {
  cy.task('log', 'Cleaning up test data...')

  // Clear cookies and storage
  cy.clearAuth()

  // Additional cleanup can be added here
  // For example, database cleanup via API calls or direct DB connection

  cy.task('log', 'Test data cleanup completed')
})

// Export to prevent TypeScript errors
export {}
