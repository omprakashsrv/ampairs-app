/**
 * Utility functions for Cypress tests
 */
import testData from '../fixtures/test-data.json'

/**
 * Generate a random mobile number for testing
 */
export function generateRandomMobileNumber(): string {
  const prefixes = ['6', '7', '8', '9']
  const prefix = prefixes[Math.floor(Math.random() * prefixes.length)]
  const remaining = Math.floor(Math.random() * 900000000) + 100000000
  return prefix + remaining.toString()
}

/**
 * Generate a random OTP for testing
 */
export function generateRandomOtp(): string {
  return Math.floor(Math.random() * 900000 + 100000).toString()
}

/**
 * Get current timestamp
 */
export function getCurrentTimestamp(): string {
  return new Date().toISOString()
}

/**
 * Replace template variables in mock responses
 */
export function replaceMockTemplateVariables(template: any): any {
  const templateStr = JSON.stringify(template)
  const timestamp = Date.now().toString()
  const replacedStr = templateStr.replace(/\{\{timestamp\}\}/g, timestamp)
  return JSON.parse(replacedStr)
}

/**
 * Wait for a specific amount of time
 */
export function waitFor(milliseconds: number): Cypress.Chainable<void> {
  return cy.wait(milliseconds)
}

/**
 * Generate test session ID
 */
export function generateTestSessionId(): string {
  return `test-session-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
}

/**
 * Generate mock JWT token for testing
 */
export function generateMockJwtToken(payload: any = {}): string {
  const header = {alg: 'HS256', typ: 'JWT'}
  const defaultPayload = {
    sub: 'test-user-id',
    mobile: testData.users.validUser.mobileNumber,
    exp: Math.floor(Date.now() / 1000) + 3600, // 1 hour
    iat: Math.floor(Date.now() / 1000),
    tenant: 'test-tenant'
  }

  const finalPayload = {...defaultPayload, ...payload}

  const headerEncoded = btoa(JSON.stringify(header))
  const payloadEncoded = btoa(JSON.stringify(finalPayload))
  const signature = 'mock-signature'

  return `${headerEncoded}.${payloadEncoded}.${signature}`
}

/**
 * Generate expired JWT token for testing
 */
export function generateExpiredJwtToken(): string {
  return generateMockJwtToken({
    exp: Math.floor(Date.now() / 1000) - 3600 // Expired 1 hour ago
  })
}

/**
 * Validate mobile number format
 */
export function isValidMobileNumber(mobile: string): boolean {
  return /^[6-9]\d{9}$/.test(mobile)
}

/**
 * Validate OTP format
 */
export function isValidOtp(otp: string): boolean {
  return /^\d{6}$/.test(otp)
}

/**
 * Get random element from array
 */
export function getRandomElement<T>(array: T[]): T {
  return array[Math.floor(Math.random() * array.length)]
}

/**
 * Create test user data
 */
export function createTestUserData(overrides: any = {}): any {
  return {
    ...testData.users.validUser,
    mobileNumber: generateRandomMobileNumber(),
    id: `test-user-${Date.now()}`,
    ...overrides
  }
}

/**
 * Setup interceptors for common API calls
 */
export function setupCommonInterceptors(): void {
  const apiUrl = Cypress.env('apiUrl')

  // Health check
  cy.intercept('GET', `${apiUrl}/actuator/health`, {
    statusCode: 200,
    body: {status: 'UP'}
  }).as('healthCheck')

  // CORS preflight
  cy.intercept('OPTIONS', `${apiUrl}/**`, {
    statusCode: 200,
    headers: {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, POST, PUT, DELETE, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type, Authorization'
    }
  }).as('corsCheck')
}

/**
 * Mock network error for API calls
 */
export function mockNetworkError(url: string, alias: string): void {
  cy.intercept('POST', url, {
    forceNetworkError: true
  }).as(alias)
}

/**
 * Mock slow API response
 */
export function mockSlowResponse(url: string, response: any, delay: number, alias: string): void {
  cy.intercept('POST', url, (req) => {
    req.reply({
      delay: delay,
      statusCode: 200,
      body: response
    })
  }).as(alias)
}

/**
 * Mock rate limited response
 */
export function mockRateLimitedResponse(url: string, alias: string): void {
  cy.intercept('POST', url, {
    statusCode: 429,
    body: {
      success: false,
      error: {
        code: 'RATE_LIMITED',
        message: 'Too many requests. Please try again later.',
        timestamp: getCurrentTimestamp()
      }
    }
  }).as(alias)
}

/**
 * Verify response structure
 */
export function verifyResponseStructure(response: any, expectedStructure: string[]): void {
  expectedStructure.forEach(property => {
    expect(response.body).to.have.property(property)
  })
}

/**
 * Verify error response structure
 */
export function verifyErrorResponseStructure(response: any): void {
  expect(response.body).to.have.property('success', false)
  expect(response.body).to.have.property('error')
  expect(response.body.error).to.have.property('code')
  expect(response.body.error).to.have.property('message')
  expect(response.body.error).to.have.property('timestamp')
}

/**
 * Log test information
 */
export function logTestInfo(message: string, data?: any): void {
  cy.task('log', `[TEST INFO] ${message}`)
  if (data) {
    cy.task('table', data)
  }
}

/**
 * Measure response time
 */
export function measureResponseTime(requestPromise: any): Cypress.Chainable<number> {
  const startTime = Date.now()
  return cy.wrap(requestPromise).then(() => {
    const endTime = Date.now()
    const responseTime = endTime - startTime
    logTestInfo(`Response time: ${responseTime}ms`)
    return responseTime
  })
}

/**
 * Retry with exponential backoff
 */
export function retryWithBackoff(
  operation: () => Cypress.Chainable<any>,
  maxRetries: number = 3,
  baseDelay: number = 1000
): Cypress.Chainable<any> {
  let retryCount = 0

  function attemptOperation(): Cypress.Chainable<any> {
    return operation().catch((error) => {
      if (retryCount < maxRetries) {
        retryCount++
        const delay = baseDelay * Math.pow(2, retryCount - 1)
        logTestInfo(`Retry attempt ${retryCount} after ${delay}ms delay`)
        return cy.wait(delay).then(() => attemptOperation())
      } else {
        throw error
      }
    })
  }

  return attemptOperation()
}

/**
 * Generate test report data
 */
export function generateTestReportData(testName: string, result: 'pass' | 'fail', duration: number, error?: string): any {
  return {
    testName,
    result,
    duration,
    timestamp: getCurrentTimestamp(),
    error: error || null,
    environment: Cypress.env('environment') || 'development'
  }
}

/**
 * Clean up test data from local storage and cookies
 */
export function cleanupTestData(): void {
  cy.clearLocalStorage()
  cy.clearCookies()
  cy.window().then((win) => {
    win.sessionStorage.clear()
  })
}

/**
 * Setup test environment
 */
export function setupTestEnvironment(): void {
  // Set viewport to default size
  cy.viewport(1280, 720)

  // Setup common interceptors
  setupCommonInterceptors()

  // Clean up any existing data
  cleanupTestData()

  logTestInfo('Test environment setup completed')
}

/**
 * Verify JWT token structure
 */
export function verifyJwtTokenStructure(token: string): void {
  const parts = token.split('.')
  expect(parts).to.have.length(3)

  try {
    const header = JSON.parse(atob(parts[0]))
    const payload = JSON.parse(atob(parts[1]))

    expect(header).to.have.property('alg')
    expect(header).to.have.property('typ', 'JWT')
    expect(payload).to.have.property('exp')
    expect(payload).to.have.property('iat')
  } catch (error) {
    throw new Error(`Invalid JWT token structure: ${error}`)
  }
}

/**
 * Check if JWT token is expired
 */
export function isJwtTokenExpired(token: string): boolean {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    const currentTime = Math.floor(Date.now() / 1000)
    return payload.exp < currentTime
  } catch (error) {
    return true
  }
}

/**
 * Format phone number for display
 */
export function formatPhoneNumber(phone: string): string {
  if (phone.length === 10) {
    return `${phone.slice(0, 5)} ${phone.slice(5)}`
  }
  return phone
}

/**
 * Generate performance report
 */
export function generatePerformanceReport(metrics: any[]): any {
  const totalTests = metrics.length
  const passedTests = metrics.filter(m => m.result === 'pass').length
  const failedTests = totalTests - passedTests
  const averageDuration = metrics.reduce((sum, m) => sum + m.duration, 0) / totalTests

  return {
    summary: {
      totalTests,
      passedTests,
      failedTests,
      successRate: (passedTests / totalTests) * 100,
      averageDuration
    },
    details: metrics,
    generatedAt: getCurrentTimestamp()
  }
}

// Export all utility functions
export default {
  generateRandomMobileNumber,
  generateRandomOtp,
  getCurrentTimestamp,
  replaceMockTemplateVariables,
  waitFor,
  generateTestSessionId,
  generateMockJwtToken,
  generateExpiredJwtToken,
  isValidMobileNumber,
  isValidOtp,
  getRandomElement,
  createTestUserData,
  setupCommonInterceptors,
  mockNetworkError,
  mockSlowResponse,
  mockRateLimitedResponse,
  verifyResponseStructure,
  verifyErrorResponseStructure,
  logTestInfo,
  measureResponseTime,
  retryWithBackoff,
  generateTestReportData,
  cleanupTestData,
  setupTestEnvironment,
  verifyJwtTokenStructure,
  isJwtTokenExpired,
  formatPhoneNumber,
  generatePerformanceReport
}
