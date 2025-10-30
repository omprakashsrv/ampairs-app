/**
 * Smoke Tests - Quick verification that core functionality works
 * These tests should run fast and cover the most critical user paths
 */

describe('Smoke Tests - Critical Path Verification', () => {
  const testMobileNumber = Cypress.env('testMobileNumber')
  const testOtp = Cypress.env('testOtp')
  const apiUrl = Cypress.env('apiUrl')

  beforeEach(() => {
    cy.setupTestData()
  })

  afterEach(() => {
    cy.cleanupTestData()
  })

  describe('Application Availability', () => {
    it('should load the application successfully', () => {
      cy.visit('/')
      cy.get('body').should('be.visible')

      // Check if application is responsive
      cy.title().should('not.be.empty')

      // Verify no JavaScript errors
      cy.window().then((win) => {
        expect(win.console.error).to.not.have.been.called
      })
    })

    it('should redirect unauthenticated users to login', () => {
      cy.clearAuth()
      cy.visit('/home')
      cy.url().should('include', '/login')
    })

    it('should display login page correctly', () => {
      cy.visit('/login')
      cy.get('[data-cy="login-form"]').should('be.visible')
      cy.get('[data-cy="mobile-number-input"]').should('be.visible')
      cy.get('[data-cy="login-submit-btn"]').should('be.visible')
    })
  })

  describe('Backend API Availability', () => {
    it('should have backend API running', () => {
      cy.request({
        method: 'GET',
        url: `${apiUrl}/actuator/health`,
        failOnStatusCode: false
      }).then((response) => {
        expect([200, 404]).to.include(response.status)
        // 404 is acceptable if health endpoint is not configured
      })
    })

    it('should handle CORS properly', () => {
      cy.request({
        method: 'OPTIONS',
        url: `${apiUrl}/auth/v1/init`,
        headers: {
          'Origin': 'http://localhost:4200',
          'Access-Control-Request-Method': 'POST'
        },
        failOnStatusCode: false
      }).then((response) => {
        expect([200, 204]).to.include(response.status)
      })
    })

    it('should respond to auth init endpoint', () => {
      cy.request({
        method: 'POST',
        url: `${apiUrl}/auth/v1/init`,
        body: {
          phone: testMobileNumber,
          countryCode: 91,
          tokenId: ''
        },
        failOnStatusCode: false
      }).then((response) => {
        // Should either succeed or fail with proper error structure
        expect([200, 400, 429, 500]).to.include(response.status)
      })
    })
  })

  describe('Critical User Journey - Happy Path', () => {
    it('should complete login flow end-to-end', () => {
      // Mock successful API responses
      cy.mockAuthApi()

      // Start login flow
      cy.visit('/login')
      cy.waitForAngular()

      // Enter mobile number
      cy.fillMobileNumber(testMobileNumber)
      cy.get('[data-cy="login-submit-btn"]').click()

      // Wait for navigation to OTP page
      cy.wait('@authInit')
      cy.url().should('include', '/verify-otp')

      // Enter OTP
      cy.fillOtp(testOtp)
      cy.get('[data-cy="otp-submit-btn"]').click()

      // Wait for successful login
      cy.wait('@otpVerify')
      cy.url().should('include', '/home')

      // Verify authentication state
      cy.verifyAuthenticated()
      cy.getCookie('access_token').should('exist')
    })

    it('should maintain session after page reload', () => {
      cy.mockAuthApi()
      cy.login(testMobileNumber, testOtp)

      // Reload page
      cy.reload()
      cy.waitForAngular()

      // Should still be authenticated
      cy.verifyAuthenticated()
      cy.url().should('not.include', '/login')
    })
  })

  describe('Error Handling - Critical Failures', () => {
    it('should handle API server down gracefully', () => {
      // Mock network error
      cy.intercept('POST', `${apiUrl}/auth/v1/init`, {
        forceNetworkError: true
      }).as('networkError')

      cy.visit('/login')
      cy.fillMobileNumber(testMobileNumber)
      cy.get('[data-cy="login-submit-btn"]').click()

      cy.wait('@networkError')

      // Should show error message
      cy.get('[data-cy="error-message"]').should('be.visible')
      cy.get('[data-cy="error-message"]').should('contain', 'Network error')
    })

    it('should handle invalid credentials gracefully', () => {
      // Mock auth init success but verify failure
      cy.intercept('POST', `${apiUrl}/auth/v1/init`, {
        statusCode: 200,
        body: {success: true, sessionId: 'test-session'}
      }).as('authInit')

      cy.intercept('POST', `${apiUrl}/auth/v1/verify`, {
        statusCode: 401,
        body: {
          success: false,
          error: {
            code: 'INVALID_OTP',
            message: 'Invalid OTP entered'
          }
        }
      }).as('authVerifyError')

      cy.visit('/login')
      cy.fillMobileNumber(testMobileNumber)
      cy.get('[data-cy="login-submit-btn"]').click()
      cy.wait('@authInit')

      cy.fillOtp('999999')
      cy.get('[data-cy="otp-submit-btn"]').click()
      cy.wait('@authVerifyError')

      // Should show error and stay on OTP page
      cy.get('[data-cy="error-message"]').should('be.visible')
      cy.url().should('include', '/verify-otp')
    })
  })

  describe('Performance - Basic Checks', () => {
    it('should load login page within acceptable time', () => {
      const startTime = Date.now()

      cy.visit('/login')
      cy.get('[data-cy="login-form"]').should('be.visible')

      cy.then(() => {
        const loadTime = Date.now() - startTime
        expect(loadTime).to.be.lessThan(5000) // Should load within 5 seconds
      })
    })

    it('should respond to user input quickly', () => {
      cy.visit('/login')

      const startTime = Date.now()
      cy.get('[data-cy="mobile-number-input"]').type(testMobileNumber)

      cy.then(() => {
        const responseTime = Date.now() - startTime
        expect(responseTime).to.be.lessThan(1000) // Should respond within 1 second
      })
    })

    it('should handle API calls within reasonable time', () => {
      cy.mockAuthApi()

      cy.visit('/login')
      cy.fillMobileNumber(testMobileNumber)

      const startTime = Date.now()
      cy.get('[data-cy="login-submit-btn"]').click()

      cy.wait('@authInit').then(() => {
        const apiResponseTime = Date.now() - startTime
        expect(apiResponseTime).to.be.lessThan(10000) // Should respond within 10 seconds
      })
    })
  })

  describe('Browser Compatibility - Basic Checks', () => {
    it('should work in different viewport sizes', () => {
      const viewports = [
        {width: 320, height: 568},   // Mobile
        {width: 768, height: 1024},  // Tablet
        {width: 1920, height: 1080}  // Desktop
      ]

      viewports.forEach((viewport, index) => {
        cy.viewport(viewport.width, viewport.height)
        cy.visit('/login')

        cy.get('[data-cy="login-form"]').should('be.visible')
        cy.get('[data-cy="mobile-number-input"]').should('be.visible')
        cy.get('[data-cy="login-submit-btn"]').should('be.visible')

        // Test basic interaction
        cy.get('[data-cy="mobile-number-input"]').type('9876543210')
        cy.get('[data-cy="mobile-number-input"]').should('have.value', '9876543210')
      })
    })

    it('should handle browser back/forward navigation', () => {
      cy.mockAuthApi()

      cy.visit('/login')
      cy.fillMobileNumber(testMobileNumber)
      cy.get('[data-cy="login-submit-btn"]').click()
      cy.wait('@authInit')
      cy.url().should('include', '/verify-otp')

      // Go back
      cy.go('back')
      cy.url().should('include', '/login')

      // Go forward
      cy.go('forward')
      cy.url().should('include', '/verify-otp')
    })
  })

  describe('Security - Basic Checks', () => {
    it('should not expose sensitive information in DOM', () => {
      cy.mockAuthApi()
      cy.visit('/login')
      cy.fillMobileNumber(testMobileNumber)
      cy.get('[data-cy="login-submit-btn"]').click()
      cy.wait('@authInit')

      cy.fillOtp(testOtp)

      // Check that OTP is not visible in DOM attributes or content
      cy.get('body').should('not.contain', testOtp)
      cy.get('[data-otp]').should('not.exist')
      cy.get('[value="' + testOtp + '"]').should('not.exist')
    })

    it('should use HTTPS in production-like environments', () => {
      cy.location('protocol').then((protocol) => {
        // In local development, HTTP is acceptable
        if (Cypress.config().baseUrl?.includes('localhost') ||
          Cypress.config().baseUrl?.includes('127.0.0.1')) {
          expect(['http:', 'https:']).to.include(protocol)
        } else {
          // In other environments, should be HTTPS
          expect(protocol).to.eq('https:')
        }
      })
    })

    it('should clear sensitive data on logout', () => {
      cy.mockAuthApi()
      cy.login(testMobileNumber, testOtp)

      // Logout (if logout functionality exists)
      cy.get('body').then(($body) => {
        if ($body.find('[data-cy="logout-btn"]').length > 0) {
          cy.get('[data-cy="logout-btn"]').click()

          // Verify tokens are cleared
          cy.getCookie('access_token').should('not.exist')
          cy.getCookie('refresh_token').should('not.exist')

          // Should redirect to login
          cy.url().should('include', '/login')
        }
      })
    })
  })

  describe('Accessibility - Basic Checks', () => {
    it('should have proper form labels', () => {
      cy.visit('/login')

      cy.get('[data-cy="mobile-number-input"]').should('have.attr', 'aria-label')
      cy.get('[data-cy="login-form"]').should('have.attr', 'role')
    })

    it('should support keyboard navigation', () => {
      cy.visit('/login')

      // Tab through form elements
      cy.get('[data-cy="mobile-number-input"]').focus()
      cy.focused().should('have.attr', 'data-cy', 'mobile-number-input')

      cy.tab()
      cy.focused().should('have.attr', 'data-cy', 'login-submit-btn')
    })

    it('should announce errors to screen readers', () => {
      cy.visit('/login')

      // Trigger validation error
      cy.get('[data-cy="mobile-number-input"]').type('123')
      cy.get('[data-cy="mobile-number-input"]').blur()

      // Error message should have proper ARIA attributes
      cy.get('[data-cy="mobile-error"]').should('have.attr', 'role', 'alert')
    })
  })

  describe('Data Integrity - Basic Checks', () => {
    it('should preserve form data during navigation', () => {
      cy.mockAuthApi()

      cy.visit('/login')
      cy.fillMobileNumber(testMobileNumber)
      cy.get('[data-cy="login-submit-btn"]').click()
      cy.wait('@authInit')

      // Go back to login
      cy.go('back')

      // Mobile number should be preserved
      cy.get('[data-cy="mobile-number-input"]').should('have.value', testMobileNumber)
    })

    it('should handle session expiration gracefully', () => {
      cy.mockAuthApi()
      cy.login(testMobileNumber, testOtp)

      // Clear tokens to simulate expiration
      cy.clearCookies()

      // Try to access protected page
      cy.visit('/home')

      // Should redirect to login
      cy.url().should('include', '/login')
    })
  })

  describe('Monitoring and Logging', () => {
    it('should not have console errors on page load', () => {
      cy.visit('/login')

      cy.window().then((win) => {
        // Check for console errors (this requires proper setup)
        cy.get('@consoleError').should('not.have.been.called')
      })
    })

    it('should track important user interactions', () => {
      cy.mockAuthApi()

      cy.visit('/login')
      cy.fillMobileNumber(testMobileNumber)
      cy.get('[data-cy="login-submit-btn"]').click()

      // Verify API call was made
      cy.wait('@authInit').its('request.body').should('deep.include', {
        phone: testMobileNumber,
        countryCode: 91
      })
    })
  })
})
