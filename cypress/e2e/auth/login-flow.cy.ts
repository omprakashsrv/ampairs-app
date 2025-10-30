describe('Login Flow End-to-End Tests', () => {
  const testMobileNumber = Cypress.env('testMobileNumber')
  const testOtp = Cypress.env('testOtp')

  beforeEach(() => {
    // Setup test environment
    cy.setupTestData()
    cy.mockAuthApi()
  })

  afterEach(() => {
    // Cleanup after each test
    cy.cleanupTestData()
  })

  describe('Successful Login Flow', () => {
    it('should complete full login flow with valid credentials', () => {
      cy.visit('/login')
      cy.waitForAngular()

      // Verify we're on the login page
      cy.url().should('include', '/login')
      cy.get('[data-cy="login-form"]').should('be.visible')

      // Fill mobile number
      cy.fillMobileNumber(testMobileNumber)
      cy.get('[data-cy="login-submit-btn"]').should('be.enabled')
      cy.get('[data-cy="login-submit-btn"]').click()

      // Wait for API call and navigation
      cy.wait('@authInit')
      cy.url().should('include', '/verify-otp')

      // Verify OTP page is displayed
      cy.get('[data-cy="otp-form"]').should('be.visible')
      cy.get('[data-cy="mobile-display"]').should('contain', testMobileNumber)

      // Fill OTP
      cy.fillOtp(testOtp)
      cy.get('[data-cy="otp-submit-btn"]').should('be.enabled')
      cy.get('[data-cy="otp-submit-btn"]').click()

      // Wait for OTP verification and navigation
      cy.wait('@otpVerify')
      cy.url().should('include', '/home')

      // Verify authentication status
      cy.verifyAuthenticated()
      cy.getCookie('access_token').should('exist')
      cy.getCookie('refresh_token').should('exist')
    })

    it('should use custom login command successfully', () => {
      cy.login(testMobileNumber, testOtp)
      cy.verifyAuthenticated()
    })

    it('should maintain authentication state after page refresh', () => {
      cy.login(testMobileNumber, testOtp)
      cy.reload()
      cy.waitForAngular()
      cy.verifyAuthenticated()
      cy.url().should('not.include', '/login')
    })
  })

  describe('Mobile Number Validation', () => {
    beforeEach(() => {
      cy.visit('/login')
      cy.waitForAngular()
    })

    it('should validate valid Indian mobile numbers', () => {
      const validNumbers = ['9876543210', '8765432109', '7654321098', '6543210987']

      validNumbers.forEach(number => {
        cy.fillMobileNumber(number)
        cy.get('[data-cy="mobile-number-input"]').should('not.have.class', 'ng-invalid')
        cy.get('[data-cy="login-submit-btn"]').should('be.enabled')
      })
    })

    it('should reject invalid mobile numbers', () => {
      const invalidNumbers = [
        '1234567890', // Doesn't start with 6-9
        '987654321',  // Too short
        '98765432101', // Too long
        '9876543a10', // Contains letters
        '5876543210'  // Starts with 5
      ]

      invalidNumbers.forEach(number => {
        cy.get('[data-cy="mobile-number-input"]').clear().type(number)
        cy.get('[data-cy="mobile-number-input"]').should('have.class', 'ng-invalid')
        cy.get('[data-cy="login-submit-btn"]').should('be.disabled')
      })
    })

    it('should show validation error messages', () => {
      cy.get('[data-cy="mobile-number-input"]').type('123')
      cy.get('[data-cy="mobile-number-input"]').blur()
      cy.get('[data-cy="mobile-error"]').should('be.visible')
      cy.get('[data-cy="mobile-error"]').should('contain', 'Enter a valid 10-digit mobile number')
    })
  })

  describe('OTP Validation', () => {
    beforeEach(() => {
      cy.visit('/login')
      cy.fillMobileNumber(testMobileNumber)
      cy.get('[data-cy="login-submit-btn"]').click()
      cy.wait('@authInit')
      cy.url().should('include', '/verify-otp')
    })

    it('should validate valid OTP format', () => {
      cy.fillOtp('123456')
      cy.get('[data-cy="otp-input"]').should('not.have.class', 'ng-invalid')
      cy.get('[data-cy="otp-submit-btn"]').should('be.enabled')
    })

    it('should reject invalid OTP format', () => {
      const invalidOtps = ['12345', '1234567', '12345a', 'abcdef']

      invalidOtps.forEach(otp => {
        cy.get('[data-cy="otp-input"]').clear().type(otp)
        cy.get('[data-cy="otp-input"]').should('have.class', 'ng-invalid')
        cy.get('[data-cy="otp-submit-btn"]').should('be.disabled')
      })
    })

    it('should show OTP validation error messages', () => {
      cy.get('[data-cy="otp-input"]').type('123')
      cy.get('[data-cy="otp-input"]').blur()
      cy.get('[data-cy="otp-error"]').should('be.visible')
      cy.get('[data-cy="otp-error"]').should('contain', 'Enter a valid 6-digit OTP')
    })
  })

  describe('Error Handling', () => {
    it('should handle mobile number submission errors', () => {
      // Mock API error response
      cy.intercept('POST', `${Cypress.env('apiUrl')}/auth/v1/init`, {
        statusCode: 400,
        body: {
          success: false,
          error: {
            code: 'INVALID_MOBILE',
            message: 'Invalid mobile number format'
          }
        }
      }).as('authInitError')

      cy.visit('/login')
      cy.fillMobileNumber(testMobileNumber)
      cy.get('[data-cy="login-submit-btn"]').click()

      cy.wait('@authInitError')
      cy.get('[data-cy="error-message"]').should('be.visible')
      cy.get('[data-cy="error-message"]').should('contain', 'Invalid mobile number format')
    })

    it('should handle OTP verification errors', () => {
      // Setup successful init, then error on verify
      cy.visit('/login')
      cy.fillMobileNumber(testMobileNumber)
      cy.get('[data-cy="login-submit-btn"]').click()
      cy.wait('@authInit')

      // Mock OTP verification error
      cy.intercept('POST', `${Cypress.env('apiUrl')}/auth/v1/verify`, {
        statusCode: 401,
        body: {
          success: false,
          error: {
            code: 'INVALID_OTP',
            message: 'Invalid or expired OTP'
          }
        }
      }).as('otpVerifyError')

      cy.fillOtp('123456')
      cy.get('[data-cy="otp-submit-btn"]').click()

      cy.wait('@otpVerifyError')
      cy.get('[data-cy="error-message"]').should('be.visible')
      cy.get('[data-cy="error-message"]').should('contain', 'Invalid or expired OTP')
      cy.url().should('include', '/verify-otp') // Should stay on OTP page
    })

    it('should handle network errors gracefully', () => {
      cy.intercept('POST', `${Cypress.env('apiUrl')}/auth/v1/init`, {
        forceNetworkError: true
      }).as('networkError')

      cy.visit('/login')
      cy.fillMobileNumber(testMobileNumber)
      cy.get('[data-cy="login-submit-btn"]').click()

      cy.wait('@networkError')
      cy.get('[data-cy="error-message"]').should('be.visible')
      cy.get('[data-cy="error-message"]').should('contain', 'Network error. Please try again.')
    })
  })

  describe('Session Management', () => {
    it('should redirect to login when not authenticated', () => {
      cy.clearAuth()
      cy.visit('/home')
      cy.url().should('include', '/login')
    })

    it('should redirect authenticated users away from auth pages', () => {
      cy.login(testMobileNumber, testOtp)

      // Try to visit login page when authenticated
      cy.visit('/login')
      cy.url().should('include', '/home')

      // Try to visit OTP page when authenticated
      cy.visit('/verify-otp')
      cy.url().should('include', '/home')
    })

    it('should handle logout correctly', () => {
      cy.login(testMobileNumber, testOtp)

      // Trigger logout (assuming there's a logout button)
      cy.get('[data-cy="logout-btn"]').click()

      cy.verifyNotAuthenticated()
      cy.url().should('include', '/login')
    })
  })

  describe('UI/UX Behavior', () => {
    it('should show loading states during authentication', () => {
      cy.visit('/login')
      cy.fillMobileNumber(testMobileNumber)

      // Mock slow API response
      cy.intercept('POST', `${Cypress.env('apiUrl')}/auth/v1/init`, (req) => {
        req.reply({
          delay: 2000,
          statusCode: 200,
          body: {
            success: true,
            sessionId: 'test-session-id'
          }
        })
      }).as('slowAuthInit')

      cy.get('[data-cy="login-submit-btn"]').click()

      // Should show loading state
      cy.get('[data-cy="login-submit-btn"]').should('be.disabled')
      cy.get('[data-cy="loading-spinner"]').should('be.visible')

      cy.wait('@slowAuthInit')
      cy.get('[data-cy="loading-spinner"]').should('not.exist')
    })

    it('should show countdown timer for OTP resend', () => {
      cy.visit('/login')
      cy.fillMobileNumber(testMobileNumber)
      cy.get('[data-cy="login-submit-btn"]').click()
      cy.wait('@authInit')

      // Check for resend timer
      cy.get('[data-cy="resend-timer"]').should('be.visible')
      cy.get('[data-cy="resend-otp-btn"]').should('be.disabled')

      // Wait for timer to complete (or mock it)
      cy.get('[data-cy="resend-otp-btn"]', {timeout: 60000}).should('be.enabled')
    })

    it('should handle browser back button correctly', () => {
      cy.visit('/login')
      cy.fillMobileNumber(testMobileNumber)
      cy.get('[data-cy="login-submit-btn"]').click()
      cy.wait('@authInit')
      cy.url().should('include', '/verify-otp')

      // Go back to login page
      cy.go('back')
      cy.url().should('include', '/login')

      // Mobile number should be preserved
      cy.get('[data-cy="mobile-number-input"]').should('have.value', testMobileNumber)
    })
  })

  describe('Accessibility', () => {
    it('should have proper ARIA labels and roles', () => {
      cy.visit('/login')

      cy.get('[data-cy="mobile-number-input"]').should('have.attr', 'aria-label')
      cy.get('[data-cy="login-submit-btn"]').should('have.attr', 'aria-label')
      cy.get('[data-cy="login-form"]').should('have.attr', 'role', 'form')
    })

    it('should support keyboard navigation', () => {
      cy.visit('/login')

      cy.get('[data-cy="mobile-number-input"]').focus()
      cy.get('[data-cy="mobile-number-input"]').type(testMobileNumber)
      cy.get('[data-cy="mobile-number-input"]').tab()
      cy.get('[data-cy="login-submit-btn"]').should('be.focused')
      cy.get('[data-cy="login-submit-btn"]').type('{enter}')

      cy.wait('@authInit')
      cy.url().should('include', '/verify-otp')
    })
  })

  describe('Cross-browser Compatibility', () => {
    it('should work in different viewport sizes', () => {
      const viewports = [
        {width: 320, height: 568},  // Mobile
        {width: 768, height: 1024}, // Tablet
        {width: 1920, height: 1080} // Desktop
      ]

      viewports.forEach(viewport => {
        cy.viewport(viewport.width, viewport.height)
        cy.visit('/login')
        cy.waitForAngular()

        cy.get('[data-cy="login-form"]').should('be.visible')
        cy.get('[data-cy="mobile-number-input"]').should('be.visible')
        cy.get('[data-cy="login-submit-btn"]').should('be.visible')
      })
    })
  })
})
