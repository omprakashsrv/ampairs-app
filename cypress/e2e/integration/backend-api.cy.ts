describe('Backend API Integration Tests', () => {
  const testMobileNumber = Cypress.env('testMobileNumber')
  const testOtp = Cypress.env('testOtp')
  const apiUrl = Cypress.env('apiUrl')

  beforeEach(() => {
    cy.setupTestData()
  })

  afterEach(() => {
    cy.cleanupTestData()
  })

  describe('Authentication API Endpoints', () => {
    describe('POST /auth/v1/init', () => {
      it('should initialize authentication with valid mobile number', () => {
        const requestBody = {
          phone: testMobileNumber,
          countryCode: 91,
          tokenId: '',
          recaptchaToken: undefined
        }

        cy.request({
          method: 'POST',
          url: `${apiUrl}/auth/v1/init`,
          body: requestBody,
          headers: {
            'Content-Type': 'application/json'
          }
        }).then((response) => {
          expect(response.status).to.eq(200)
          expect(response.body).to.have.property('success', true)
          expect(response.body).to.have.property('sessionId')
          expect(response.body.sessionId).to.be.a('string')
          expect(response.body.sessionId).to.not.be.empty
        })
      })

      it('should reject invalid mobile number format', () => {
        const requestBody = {
          phone: '1234567890', // Invalid format
          countryCode: 91,
          tokenId: ''
        }

        cy.request({
          method: 'POST',
          url: `${apiUrl}/auth/v1/init`,
          body: requestBody,
          failOnStatusCode: false
        }).then((response) => {
          expect(response.status).to.eq(400)
          expect(response.body).to.have.property('success', false)
          expect(response.body).to.have.property('error')
          expect(response.body.error).to.have.property('code')
          expect(response.body.error).to.have.property('message')
        })
      })

      it('should handle missing required fields', () => {
        const requestBody = {
          countryCode: 91
          // Missing phone field
        }

        cy.request({
          method: 'POST',
          url: `${apiUrl}/auth/v1/init`,
          body: requestBody,
          failOnStatusCode: false
        }).then((response) => {
          expect(response.status).to.eq(400)
          expect(response.body).to.have.property('success', false)
          expect(response.body.error.message).to.include('phone')
        })
      })

      it('should handle rate limiting', () => {
        const requestBody = {
          phone: testMobileNumber,
          countryCode: 91,
          tokenId: ''
        }

        // Make multiple rapid requests to trigger rate limiting
        const requests = Array.from({length: 5}, () =>
          cy.request({
            method: 'POST',
            url: `${apiUrl}/auth/v1/init`,
            body: requestBody,
            failOnStatusCode: false
          })
        )

        cy.wrap(Promise.all(requests)).then((responses: any[]) => {
          // At least one request should be rate limited
          const rateLimitedResponses = responses.filter(r => r.status === 429)
          expect(rateLimitedResponses.length).to.be.greaterThan(0)
        })
      })
    })

    describe('POST /auth/v1/verify', () => {
      let sessionId: string

      beforeEach(() => {
        // Get a valid session ID first
        cy.request({
          method: 'POST',
          url: `${apiUrl}/auth/v1/init`,
          body: {
            phone: testMobileNumber,
            countryCode: 91,
            tokenId: ''
          }
        }).then((response) => {
          sessionId = response.body.sessionId
        })
      })

      it('should verify OTP successfully with valid credentials', () => {
        const requestBody = {
          sessionId: sessionId,
          otp: testOtp,
          authMode: 'OTP'
        }

        cy.request({
          method: 'POST',
          url: `${apiUrl}/auth/v1/verify`,
          body: requestBody
        }).then((response) => {
          expect(response.status).to.eq(200)
          expect(response.body).to.have.property('access_token')
          expect(response.body).to.have.property('refresh_token')
          expect(response.body.access_token).to.be.a('string')
          expect(response.body.refresh_token).to.be.a('string')

          // Validate JWT token structure
          const accessToken = response.body.access_token
          const tokenParts = accessToken.split('.')
          expect(tokenParts).to.have.length(3)
        })
      })

      it('should reject invalid OTP', () => {
        const requestBody = {
          sessionId: sessionId,
          otp: '999999', // Invalid OTP
          authMode: 'OTP'
        }

        cy.request({
          method: 'POST',
          url: `${apiUrl}/auth/v1/verify`,
          body: requestBody,
          failOnStatusCode: false
        }).then((response) => {
          expect(response.status).to.eq(401)
          expect(response.body).to.have.property('success', false)
          expect(response.body.error.code).to.include('INVALID')
        })
      })

      it('should reject invalid session ID', () => {
        const requestBody = {
          sessionId: 'invalid-session-id',
          otp: testOtp,
          authMode: 'OTP'
        }

        cy.request({
          method: 'POST',
          url: `${apiUrl}/auth/v1/verify`,
          body: requestBody,
          failOnStatusCode: false
        }).then((response) => {
          expect(response.status).to.eq(401)
          expect(response.body).to.have.property('success', false)
          expect(response.body.error.code).to.include('SESSION')
        })
      })

      it('should handle expired session', () => {
        // Wait for session to expire (if timeout is short) or mock expired session
        cy.wait(5000) // Wait 5 seconds

        const requestBody = {
          sessionId: sessionId,
          otp: testOtp,
          authMode: 'OTP'
        }

        cy.request({
          method: 'POST',
          url: `${apiUrl}/auth/v1/verify`,
          body: requestBody,
          failOnStatusCode: false
        }).then((response) => {
          // Response could be 401 or 410 depending on implementation
          expect([401, 410]).to.include(response.status)
          expect(response.body).to.have.property('success', false)
        })
      })
    })

    describe('POST /auth/v1/refresh_token', () => {
      let refreshToken: string

      beforeEach(() => {
        // Get valid tokens first
        cy.request({
          method: 'POST',
          url: `${apiUrl}/auth/v1/init`,
          body: {
            phone: testMobileNumber,
            countryCode: 91,
            tokenId: ''
          }
        }).then((initResponse) => {
          return cy.request({
            method: 'POST',
            url: `${apiUrl}/auth/v1/verify`,
            body: {
              sessionId: initResponse.body.sessionId,
              otp: testOtp,
              authMode: 'OTP'
            }
          })
        }).then((verifyResponse) => {
          refreshToken = verifyResponse.body.refresh_token
        })
      })

      it('should refresh token successfully with valid refresh token', () => {
        const requestBody = {
          refreshToken: refreshToken
        }

        cy.request({
          method: 'POST',
          url: `${apiUrl}/auth/v1/refresh_token`,
          body: requestBody
        }).then((response) => {
          expect(response.status).to.eq(200)
          expect(response.body).to.have.property('access_token')
          expect(response.body).to.have.property('refresh_token')

          // New tokens should be different from original
          expect(response.body.refresh_token).to.not.eq(refreshToken)
        })
      })

      it('should reject invalid refresh token', () => {
        const requestBody = {
          refreshToken: 'invalid-refresh-token'
        }

        cy.request({
          method: 'POST',
          url: `${apiUrl}/auth/v1/refresh_token`,
          body: requestBody,
          failOnStatusCode: false
        }).then((response) => {
          expect(response.status).to.eq(401)
          expect(response.body).to.have.property('success', false)
        })
      })
    })

    describe('POST /auth/v1/logout', () => {
      let accessToken: string

      beforeEach(() => {
        // Get valid access token
        cy.request({
          method: 'POST',
          url: `${apiUrl}/auth/v1/init`,
          body: {
            phone: testMobileNumber,
            countryCode: 91,
            tokenId: ''
          }
        }).then((initResponse) => {
          return cy.request({
            method: 'POST',
            url: `${apiUrl}/auth/v1/verify`,
            body: {
              sessionId: initResponse.body.sessionId,
              otp: testOtp,
              authMode: 'OTP'
            }
          })
        }).then((verifyResponse) => {
          accessToken = verifyResponse.body.access_token
        })
      })

      it('should logout successfully with valid token', () => {
        cy.request({
          method: 'POST',
          url: `${apiUrl}/auth/v1/logout`,
          headers: {
            'Authorization': `Bearer ${accessToken}`
          }
        }).then((response) => {
          expect(response.status).to.eq(200)
          expect(response.body).to.have.property('success', true)
        })
      })

      it('should handle logout without authentication', () => {
        cy.request({
          method: 'POST',
          url: `${apiUrl}/auth/v1/logout`,
          failOnStatusCode: false
        }).then((response) => {
          expect(response.status).to.eq(401)
        })
      })
    })
  })

  describe('User API Endpoints', () => {
    let accessToken: string

    beforeEach(() => {
      // Authenticate and get access token
      cy.request({
        method: 'POST',
        url: `${apiUrl}/auth/v1/init`,
        body: {
          phone: testMobileNumber,
          countryCode: 91,
          tokenId: ''
        }
      }).then((initResponse) => {
        return cy.request({
          method: 'POST',
          url: `${apiUrl}/auth/v1/verify`,
          body: {
            sessionId: initResponse.body.sessionId,
            otp: testOtp,
            authMode: 'OTP'
          }
        })
      }).then((verifyResponse) => {
        accessToken = verifyResponse.body.access_token
      })
    })

    describe('GET /user/v1', () => {
      it('should get user profile with valid token', () => {
        cy.request({
          method: 'GET',
          url: `${apiUrl}/user/v1`,
          headers: {
            'Authorization': `Bearer ${accessToken}`
          }
        }).then((response) => {
          expect(response.status).to.eq(200)
          expect(response.body).to.have.property('id')
          expect(response.body).to.have.property('mobileNumber', testMobileNumber)
          expect(response.body).to.have.property('name')
          expect(response.body).to.have.property('email')
        })
      })

      it('should reject request without authentication', () => {
        cy.request({
          method: 'GET',
          url: `${apiUrl}/user/v1`,
          failOnStatusCode: false
        }).then((response) => {
          expect(response.status).to.eq(401)
        })
      })

      it('should reject request with invalid token', () => {
        cy.request({
          method: 'GET',
          url: `${apiUrl}/user/v1`,
          headers: {
            'Authorization': 'Bearer invalid-token'
          },
          failOnStatusCode: false
        }).then((response) => {
          expect(response.status).to.eq(401)
        })
      })
    })
  })

  describe('API Security and Headers', () => {
    it('should include proper CORS headers', () => {
      cy.request({
        method: 'OPTIONS',
        url: `${apiUrl}/auth/v1/init`,
        headers: {
          'Origin': 'http://localhost:4200',
          'Access-Control-Request-Method': 'POST'
        }
      }).then((response) => {
        expect(response.status).to.eq(200)
        expect(response.headers).to.have.property('access-control-allow-origin')
        expect(response.headers).to.have.property('access-control-allow-methods')
        expect(response.headers).to.have.property('access-control-allow-headers')
      })
    })

    it('should include security headers', () => {
      cy.request({
        method: 'POST',
        url: `${apiUrl}/auth/v1/init`,
        body: {
          phone: testMobileNumber,
          countryCode: 91,
          tokenId: ''
        }
      }).then((response) => {
        expect(response.headers).to.have.property('x-content-type-options', 'nosniff')
        expect(response.headers).to.have.property('x-frame-options')
        expect(response.headers).to.have.property('x-xss-protection')
      })
    })

    it('should validate Content-Type header', () => {
      cy.request({
        method: 'POST',
        url: `${apiUrl}/auth/v1/init`,
        headers: {
          'Content-Type': 'text/plain'
        },
        body: 'invalid body',
        failOnStatusCode: false
      }).then((response) => {
        expect(response.status).to.eq(415) // Unsupported Media Type
      })
    })

    it('should validate request size limits', () => {
      const largeBody = {
        phone: testMobileNumber,
        countryCode: 91,
        tokenId: 'x'.repeat(10000), // Very large token
        extraData: 'x'.repeat(100000) // Large extra data
      }

      cy.request({
        method: 'POST',
        url: `${apiUrl}/auth/v1/init`,
        body: largeBody,
        failOnStatusCode: false
      }).then((response) => {
        expect([413, 400]).to.include(response.status) // Payload Too Large or Bad Request
      })
    })
  })

  describe('API Performance and Reliability', () => {
    it('should respond within acceptable time limits', () => {
      const startTime = Date.now()

      cy.request({
        method: 'POST',
        url: `${apiUrl}/auth/v1/init`,
        body: {
          phone: testMobileNumber,
          countryCode: 91,
          tokenId: ''
        }
      }).then((response) => {
        const responseTime = Date.now() - startTime
        expect(responseTime).to.be.lessThan(5000) // Less than 5 seconds
        expect(response.status).to.eq(200)
      })
    })

    it('should handle concurrent requests properly', () => {
      const concurrentRequests = Array.from({length: 10}, (_, i) =>
        cy.request({
          method: 'POST',
          url: `${apiUrl}/auth/v1/init`,
          body: {
            phone: `${testMobileNumber.slice(0, -1)}${i}`, // Slightly different numbers
            countryCode: 91,
            tokenId: ''
          },
          failOnStatusCode: false
        })
      )

      cy.wrap(Promise.all(concurrentRequests)).then((responses: any[]) => {
        // Most requests should succeed
        const successfulResponses = responses.filter(r => r.status === 200)
        expect(successfulResponses.length).to.be.greaterThan(5)

        // Some might be rate limited
        const rateLimited = responses.filter(r => r.status === 429)
        expect(rateLimited.length).to.be.lessThan(responses.length / 2)
      })
    })

    it('should maintain API availability', () => {
      // Health check endpoint (if available)
      cy.request({
        method: 'GET',
        url: `${apiUrl}/actuator/health`,
        failOnStatusCode: false
      }).then((response) => {
        if (response.status === 200) {
          expect(response.body).to.have.property('status', 'UP')
        }
      })
    })
  })

  describe('Error Response Format Consistency', () => {
    it('should return consistent error format for validation errors', () => {
      cy.request({
        method: 'POST',
        url: `${apiUrl}/auth/v1/init`,
        body: {
          phone: 'invalid',
          countryCode: 91
        },
        failOnStatusCode: false
      }).then((response) => {
        expect(response.status).to.eq(400)
        expect(response.body).to.have.property('success', false)
        expect(response.body).to.have.property('error')
        expect(response.body.error).to.have.property('code')
        expect(response.body.error).to.have.property('message')
        expect(response.body.error).to.have.property('timestamp')
      })
    })

    it('should return consistent error format for authentication errors', () => {
      cy.request({
        method: 'GET',
        url: `${apiUrl}/user/v1`,
        headers: {
          'Authorization': 'Bearer invalid-token'
        },
        failOnStatusCode: false
      }).then((response) => {
        expect(response.status).to.eq(401)
        expect(response.body).to.have.property('success', false)
        expect(response.body).to.have.property('error')
        expect(response.body.error).to.have.property('code')
        expect(response.body.error).to.have.property('message')
      })
    })

    it('should return consistent error format for server errors', () => {
      // Test with an endpoint that might cause server error
      cy.request({
        method: 'POST',
        url: `${apiUrl}/auth/v1/init`,
        body: null, // Null body might cause server error
        failOnStatusCode: false
      }).then((response) => {
        if (response.status >= 500) {
          expect(response.body).to.have.property('success', false)
          expect(response.body).to.have.property('error')
          expect(response.body.error).to.have.property('code')
          expect(response.body.error).to.have.property('message')
        }
      })
    })
  })
})
