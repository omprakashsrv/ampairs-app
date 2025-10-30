describe('OTP Verification End-to-End Tests', () => {
  const testMobileNumber = Cypress.env('testMobileNumber')
  const testOtp = Cypress.env('testOtp')

  beforeEach(() => {
    cy.setupTestData()
    cy.mockAuthApi()

    // Navigate to OTP page via login flow
    cy.visit('/login')
    cy.waitForAngular()
    cy.fillMobileNumber(testMobileNumber)
    cy.get('[data-cy="login-submit-btn"]').click()
    cy.wait('@authInit')
    cy.url().should('include', '/verify-otp')
  })

  afterEach(() => {
    cy.cleanupTestData()
  })

  describe('OTP Verification Page', () => {
    it('should display OTP verification form correctly', () => {
      // Verify page elements
      cy.get('[data-cy="otp-form"]').should('be.visible')
      cy.get('[data-cy="otp-input"]').should('be.visible')
      cy.get('[data-cy="otp-submit-btn"]').should('be.visible')
      cy.get('[data-cy="resend-otp-btn"]').should('be.visible')

      // Verify mobile number is displayed
      cy.get('[data-cy="mobile-display"]').should('contain', testMobileNumber)

      // Verify page title and instructions
      cy.get('[data-cy="otp-title"]').should('contain', 'Verify OTP')
      cy.get('[data-cy="otp-instructions"]').should('contain', 'Enter the 6-digit code sent to')
    })

    it('should have correct initial form state', () => {
      cy.get('[data-cy="otp-input"]').should('have.value', '')
      cy.get('[data-cy="otp-submit-btn"]').should('be.disabled')
      cy.get('[data-cy="resend-otp-btn"]').should('be.disabled') // Initially disabled due to timer
    })
  })

  describe('OTP Input Behavior', () => {
    it('should handle OTP input correctly', () => {
      const otp = '123456'

      cy.get('[data-cy="otp-input"]').type(otp)
      cy.get('[data-cy="otp-input"]').should('have.value', otp)
      cy.get('[data-cy="otp-submit-btn"]').should('be.enabled')
    })

    it('should format OTP input (if applicable)', () => {
      // Test if OTP input has any formatting (spaces, dashes, etc.)
      cy.get('[data-cy="otp-input"]').type('123456')

      // Check if the input formats the OTP (this depends on implementation)
      cy.get('[data-cy="otp-input"]').should('have.value', '123456')
    })

    it('should limit OTP input to 6 digits', () => {
      cy.get('[data-cy="otp-input"]').type('1234567890')
      cy.get('[data-cy="otp-input"]').should('have.value', '123456')
    })

    it('should only accept numeric input', () => {
      cy.get('[data-cy="otp-input"]').type('abc123def456')
      cy.get('[data-cy="otp-input"]').should('have.value', '123456')
    })

    it('should clear input when cleared', () => {
      cy.get('[data-cy="otp-input"]').type('123456')
      cy.get('[data-cy="otp-input"]').clear()
      cy.get('[data-cy="otp-input"]').should('have.value', '')
      cy.get('[data-cy="otp-submit-btn"]').should('be.disabled')
    })
  })

  describe('OTP Verification Success', () => {
    it('should verify OTP and redirect to home page', () => {
      cy.fillOtp(testOtp)
      cy.get('[data-cy="otp-submit-btn"]').click()

      cy.wait('@otpVerify')
      cy.url().should('include', '/home')
      cy.verifyAuthenticated()
    })

    it('should store authentication tokens after successful verification', () => {
      cy.fillOtp(testOtp)
      cy.get('[data-cy="otp-submit-btn"]').click()

      cy.wait('@otpVerify')
      cy.getCookie('access_token').should('exist')
      cy.getCookie('refresh_token').should('exist')
    })

    it('should fetch user profile after successful authentication', () => {
      cy.fillOtp(testOtp)
      cy.get('[data-cy="otp-submit-btn"]').click()

      cy.wait('@otpVerify')
      cy.wait('@userProfile')

      // Verify user profile is loaded (this depends on UI implementation)
      cy.get('[data-cy="user-profile"]').should('be.visible')
    })
  })

  describe('OTP Verification Failures', () => {
    it('should handle invalid OTP error', () => {
      // Mock invalid OTP response
      cy.intercept('POST', `${Cypress.env('apiUrl')}/auth/v1/verify`, {
        statusCode: 401,
        body: {
          success: false,
          error: {
            code: 'INVALID_OTP',
            message: 'Invalid OTP entered'
          }
        }
      }).as('invalidOtp')

      cy.fillOtp('999999')
      cy.get('[data-cy="otp-submit-btn"]').click()

      cy.wait('@invalidOtp')
      cy.get('[data-cy="error-message"]').should('be.visible')
      cy.get('[data-cy="error-message"]').should('contain', 'Invalid OTP')
      cy.url().should('include', '/verify-otp') // Should stay on OTP page
    })

    it('should handle expired OTP error', () => {
      cy.intercept('POST', `${Cypress.env('apiUrl')}/auth/v1/verify`, {
        statusCode: 410,
        body: {
          success: false,
          error: {
            code: 'OTP_EXPIRED',
            message: 'OTP has expired. Please request a new one.'
          }
        }
      }).as('expiredOtp')

      cy.fillOtp(testOtp)
      cy.get('[data-cy="otp-submit-btn"]').click()

      cy.wait('@expiredOtp')
      cy.get('[data-cy="error-message"]').should('contain', 'OTP has expired')
      cy.get('[data-cy="resend-otp-btn"]').should('be.enabled')
    })

    it('should handle too many attempts error', () => {
      cy.intercept('POST', `${Cypress.env('apiUrl')}/auth/v1/verify`, {
        statusCode: 429,
        body: {
          success: false,
          error: {
            code: 'TOO_MANY_ATTEMPTS',
            message: 'Too many failed attempts. Please try again later.'
          }
        }
      }).as('tooManyAttempts')

      cy.fillOtp('123456')
      cy.get('[data-cy="otp-submit-btn"]').click()

      cy.wait('@tooManyAttempts')
      cy.get('[data-cy="error-message"]').should('contain', 'Too many failed attempts')
      cy.get('[data-cy="otp-submit-btn"]').should('be.disabled')
    })

    it('should handle session expired error', () => {
      cy.intercept('POST', `${Cypress.env('apiUrl')}/auth/v1/verify`, {
        statusCode: 401,
        body: {
          success: false,
          error: {
            code: 'SESSION_EXPIRED',
            message: 'Session has expired. Please start over.'
          }
        }
      }).as('sessionExpired')

      cy.fillOtp(testOtp)
      cy.get('[data-cy="otp-submit-btn"]').click()

      cy.wait('@sessionExpired')
      cy.get('[data-cy="error-message"]').should('contain', 'Session has expired')

      // Should redirect back to login page
      cy.url().should('include', '/login')
    })
  })

  describe('OTP Resend Functionality', () => {
    it('should show countdown timer for resend button', () => {
      cy.get('[data-cy="resend-timer"]').should('be.visible')
      cy.get('[data-cy="resend-timer"]').should('contain', '1:') // Should show remaining time
      cy.get('[data-cy="resend-otp-btn"]').should('be.disabled')
    })

    it('should enable resend button after timer expires', () => {
      // Mock short timer for testing
      cy.window().then((win) => {
        // Manipulate timer or wait for it to complete
        // This depends on implementation details
      })

      cy.get('[data-cy="resend-otp-btn"]', {timeout: 65000}).should('be.enabled')
      cy.get('[data-cy="resend-timer"]').should('not.exist')
    })

    it('should resend OTP successfully', () => {
      // Mock resend API
      cy.intercept('POST', `${Cypress.env('apiUrl')}/auth/v1/resend`, {
        statusCode: 200,
        body: {
          success: true,
          sessionId: 'new-session-id'
        }
      }).as('resendOtp')

      // Enable resend button (simulate timer completion)
      cy.get('[data-cy="resend-otp-btn"]').invoke('prop', 'disabled', false).click()

      cy.wait('@resendOtp')
      cy.get('[data-cy="success-message"]').should('contain', 'OTP sent successfully')

      // Timer should restart
      cy.get('[data-cy="resend-timer"]').should('be.visible')
      cy.get('[data-cy="resend-otp-btn"]').should('be.disabled')
    })

    it('should handle resend OTP failure', () => {
      cy.intercept('POST', `${Cypress.env('apiUrl')}/auth/v1/resend`, {
        statusCode: 500,
        body: {
          success: false,
          error: {
            code: 'RESEND_FAILED',
            message: 'Failed to resend OTP. Please try again.'
          }
        }
      }).as('resendFailed')

      cy.get('[data-cy="resend-otp-btn"]').invoke('prop', 'disabled', false).click()

      cy.wait('@resendFailed')
      cy.get('[data-cy="error-message"]').should('contain', 'Failed to resend OTP')
    })

    it('should limit number of resend attempts', () => {
      // Test maximum resend attempts (if implemented)
      cy.intercept('POST', `${Cypress.env('apiUrl')}/auth/v1/resend`, {
        statusCode: 429,
        body: {
          success: false,
          error: {
            code: 'MAX_RESENDS_EXCEEDED',
            message: 'Maximum resend attempts exceeded'
          }
        }
      }).as('maxResendsExceeded')

      cy.get('[data-cy="resend-otp-btn"]').invoke('prop', 'disabled', false).click()

      cy.wait('@maxResendsExceeded')
      cy.get('[data-cy="error-message"]').should('contain', 'Maximum resend attempts')
      cy.get('[data-cy="resend-otp-btn"]').should('be.disabled')
    })
  })

  describe('Navigation and Back Button', () => {
    it('should handle back button navigation', () => {
      cy.go('back')
      cy.url().should('include', '/login')

      // Mobile number should be preserved
      cy.get('[data-cy="mobile-number-input"]').should('have.value', testMobileNumber)
    })

    it('should provide edit mobile number option', () => {
      cy.get('[data-cy="edit-mobile-btn"]').click()
      cy.url().should('include', '/login')
      cy.get('[data-cy="mobile-number-input"]').should('have.value', testMobileNumber)
    })

    it('should prevent direct navigation to OTP page without session', () => {
      cy.clearAuth()
      cy.visit('/verify-otp')
      cy.url().should('include', '/login')
    })
  })

  describe('Loading States and UI Feedback', () => {
    it('should show loading state during OTP verification', () => {
      // Mock slow verification
      cy.intercept('POST', `${Cypress.env('apiUrl')}/auth/v1/verify`, (req) => {
        req.reply({
          delay: 2000,
          statusCode: 200,
          body: {
            access_token: 'mock-token',
            refresh_token: 'mock-refresh-token'
          }
        })
      }).as('slowVerify')

      cy.fillOtp(testOtp)
      cy.get('[data-cy="otp-submit-btn"]').click()

      // Should show loading state
      cy.get('[data-cy="otp-submit-btn"]').should('be.disabled')
      cy.get('[data-cy="loading-spinner"]').should('be.visible')

      cy.wait('@slowVerify')
      cy.get('[data-cy="loading-spinner"]').should('not.exist')
    })

    it('should show loading state during OTP resend', () => {
      cy.intercept('POST', `${Cypress.env('apiUrl')}/auth/v1/resend`, (req) => {
        req.reply({
          delay: 1500,
          statusCode: 200,
          body: {success: true, sessionId: 'new-session'}
        })
      }).as('slowResend')

      cy.get('[data-cy="resend-otp-btn"]').invoke('prop', 'disabled', false).click()

      cy.get('[data-cy="resend-loading"]').should('be.visible')
      cy.get('[data-cy="resend-otp-btn"]').should('be.disabled')

      cy.wait('@slowResend')
      cy.get('[data-cy="resend-loading"]').should('not.exist')
    })
  })

  describe('Form Validation and Accessibility', () => {
    it('should have proper ARIA labels and roles', () => {
      cy.get('[data-cy="otp-input"]').should('have.attr', 'aria-label')
      cy.get('[data-cy="otp-form"]').should('have.attr', 'role', 'form')
      cy.get('[data-cy="otp-submit-btn"]').should('have.attr', 'aria-label')
    })

    it('should support keyboard navigation', () => {
      cy.get('[data-cy="otp-input"]').focus()
      cy.get('[data-cy="otp-input"]').type(testOtp)
      cy.get('[data-cy="otp-input"]').tab()
      cy.get('[data-cy="otp-submit-btn"]').should('be.focused')
      cy.get('[data-cy="otp-submit-btn"]').type('{enter}')

      cy.wait('@otpVerify')
      cy.url().should('include', '/home')
    })

    it('should announce errors to screen readers', () => {
      cy.intercept('POST', `${Cypress.env('apiUrl')}/auth/v1/verify`, {
        statusCode: 401,
        body: {
          success: false,
          error: {code: 'INVALID_OTP', message: 'Invalid OTP'}
        }
      }).as('invalidOtp')

      cy.fillOtp('999999')
      cy.get('[data-cy="otp-submit-btn"]').click()

      cy.wait('@invalidOtp')
      cy.get('[data-cy="error-message"]').should('have.attr', 'role', 'alert')
    })
  })

  describe('Security Considerations', () => {
    it('should not expose sensitive data in DOM', () => {
      // Ensure OTP is not exposed in any hidden fields or data attributes
      cy.fillOtp(testOtp)
      cy.get('body').should('not.contain', testOtp)
      cy.get('[data-otp]').should('not.exist')
    })

    it('should clear sensitive data on navigation', () => {
      cy.fillOtp(testOtp)
      cy.go('back')
      cy.go('forward')

      // OTP input should be cleared
      cy.get('[data-cy="otp-input"]').should('have.value', '')
    })

    it('should handle session timeout gracefully', () => {
      // Simulate session timeout after long delay
      cy.wait(5000) // Wait 5 seconds

      cy.intercept('POST', `${Cypress.env('apiUrl')}/auth/v1/verify`, {
        statusCode: 401,
        body: {
          success: false,
          error: {code: 'SESSION_EXPIRED', message: 'Session expired'}
        }
      }).as('sessionTimeout')

      cy.fillOtp(testOtp)
      cy.get('[data-cy="otp-submit-btn"]').click()

      cy.wait('@sessionTimeout')
      cy.url().should('include', '/login')
    })
  })

  describe('Mobile and Responsive Design', () => {
    it('should work correctly on mobile devices', () => {
      cy.viewport('iphone-6')

      cy.get('[data-cy="otp-form"]').should('be.visible')
      cy.get('[data-cy="otp-input"]').should('be.visible')
      cy.fillOtp(testOtp)
      cy.get('[data-cy="otp-submit-btn"]').click()

      cy.wait('@otpVerify')
      cy.url().should('include', '/home')
    })

    it('should handle virtual keyboard properly', () => {
      cy.viewport('iphone-6')

      cy.get('[data-cy="otp-input"]').focus()
      cy.get('[data-cy="otp-input"]').should('have.attr', 'inputmode', 'numeric')
      cy.get('[data-cy="otp-input"]').should('have.attr', 'pattern', '[0-9]*')
    })
  })
})
