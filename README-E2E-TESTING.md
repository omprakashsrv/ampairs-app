# End-to-End Testing Guide for Ampairs Web Application

## 🎯 Overview

This document provides comprehensive guidance for running E2E tests against the Ampairs web application, including handling rate limiting and backend configuration.

## 🚀 Quick Start

### 1. Backend Setup

The backend has **rate limiting enabled by default** which will cause 429 errors during testing. To run E2E tests successfully:

#### Option A: Start Backend in Test Mode (Recommended)

```bash
# From project root
./start-test.sh
```

#### Option B: Start Backend in Development Mode

```bash
# From project root
./start-dev.sh
```

#### Option C: Manual Backend Start

```bash
cd ampairs_service
export SPRING_PROFILES_ACTIVE=test
export BUCKET4J_ENABLED=false
./gradlew bootRun
```

### 2. Frontend & E2E Tests

```bash
cd ampairs-web

# Install dependencies (if not already done)
npm install

# Option 1: Run E2E tests with backend running
npm run test:e2e:headless

# Option 2: Start frontend and open Cypress UI
npm run test:e2e

# Option 3: Start both frontend and Cypress together
npm run test:e2e:dev
```

## 🛡️ Rate Limiting Configuration

### Current Rate Limits (Production)

- **Auth Init (`/auth/v1/init`)**: 1 request per 20 seconds per IP
- **General API (`/api/**`)**: 20 requests per minute per IP
- **Default endpoints**: 60 requests per minute per IP

### Development/Test Profiles

- **Development Profile (`dev`)**: Rate limiting **DISABLED**
- **Test Profile (`test`)**: Rate limiting **DISABLED**
- **Production Profile (`prod`)**: Rate limiting **ENABLED**

### Configuration Files

- `application.yml`: Main configuration (rate limiting disabled by default)
- `application-dev.yml`: Development settings (no rate limiting)
- `application-test.yml`: Test settings (no rate limiting)
- `application-prod.yml`: Production settings (rate limiting enabled)

## 📁 Test Structure

```
cypress/
├── e2e/
│   ├── auth/
│   │   ├── login-flow.cy.ts           # Complete login flow tests
│   │   └── otp-verification.cy.ts     # OTP verification tests
│   ├── integration/
│   │   └── backend-api.cy.ts          # Direct API integration tests
│   ├── rate-limiting/
│   │   └── rate-limit-tests.cy.ts     # Rate limiting behavior tests
│   └── smoke/
│       └── smoke-tests.cy.ts          # Critical functionality smoke tests
├── fixtures/
│   └── test-data.json                 # Test data and configuration
├── support/
│   ├── commands.ts                    # Custom Cypress commands
│   ├── e2e.ts                         # Global test setup
│   └── utils.ts                       # Utility functions
└── cypress.config.ts                  # Cypress configuration
```

## 🧪 Test Categories

### 1. Authentication Flow Tests (`auth/`)

- Complete login flow with mobile number and OTP
- Form validation (mobile number, OTP format)
- Error handling (invalid credentials, network errors)
- Session management and redirects
- UI/UX behavior and loading states

### 2. API Integration Tests (`integration/`)

- Direct backend API calls without UI
- Authentication endpoints testing
- Error response validation
- Performance and reliability checks
- CORS and security headers validation

### 3. Rate Limiting Tests (`rate-limiting/`)

- Rate limiting behavior verification
- Error handling for 429 responses
- Recovery after rate limit cooldown
- Different rate limits for different endpoints

### 4. Smoke Tests (`smoke/`)

- Critical path verification
- Application availability checks
- Performance benchmarks
- Cross-browser compatibility basics

## 🔧 Custom Cypress Commands

### Authentication Commands

```typescript
cy.login(mobileNumber, otp)           // Complete login flow
cy.mockAuthApi()                      // Mock API responses
cy.clearAuth()                        // Clear authentication data
cy.verifyAuthenticated()              // Verify user is logged in
cy.verifyNotAuthenticated()           // Verify user is logged out
```

### Form Interaction Commands

```typescript
cy.fillMobileNumber(number)           // Fill and validate mobile number
cy.fillOtp(otp)                      // Fill and validate OTP
cy.waitForAngular()                  // Wait for Angular to stabilize
```

### Test Management Commands

```typescript
cy.setupTestData()                   // Setup test environment
cy.cleanupTestData()                 // Cleanup after tests
cy.mockOtpGeneration(otp)           // Mock OTP generation
```

## 🐛 Troubleshooting

### 429 Too Many Requests Error

**Problem**: Getting "429 Too Many Requests" errors during testing.

**Solutions**:

1. **Use Test Profile**: Start backend with `./start-test.sh`
2. **Check Profile**: Ensure `SPRING_PROFILES_ACTIVE=test` or `dev`
3. **Verify Configuration**: Check `bucket4j.enabled: false` in test profile
4. **Wait**: If using production profile, wait 20 seconds between requests

### Tests Failing Due to Rate Limiting

**Problem**: Tests intermittently failing with rate limit errors.

**Solutions**:

1. **Disable Rate Limiting**: Use test or dev profile
2. **Mock Responses**: Use `cy.mockAuthApi()` in tests
3. **Add Delays**: Add `cy.wait(20000)` between requests if testing rate limits
4. **Use Test-Specific Config**: Create test-specific interceptors

### Authentication State Issues

**Problem**: Tests failing due to authentication state problems.

**Solutions**:

1. **Clear State**: Use `cy.clearAuth()` in `beforeEach`
2. **Verify Tokens**: Check cookies are properly set/cleared
3. **Mock Responses**: Use consistent mock responses
4. **Check Redirects**: Verify redirect logic in auth guard

### API Endpoint Issues

**Problem**: API calls failing or returning unexpected responses.

**Solutions**:

1. **Check Backend**: Ensure backend is running on port 8080
2. **Verify CORS**: Check CORS configuration for localhost:4200
3. **Check Environment**: Verify Cypress environment variables
4. **Use Network Tab**: Check actual API calls in browser dev tools

## 📊 Test Data Management

### Environment Variables (cypress.config.ts)

```typescript
env: {
  apiUrl: 'http://localhost:8080',
  testMobileNumber: '9876543210',
  testOtp: '123456',
  recaptchaEnabled: false
}
```

### Test Data (fixtures/test-data.json)

- User credentials and validation rules
- API endpoints and error codes
- UI selectors and timeouts
- Mock responses and test scenarios

## 🔍 Running Specific Test Suites

```bash
# Run only authentication tests
npx cypress run --spec "cypress/e2e/auth/**/*"

# Run only smoke tests
npx cypress run --spec "cypress/e2e/smoke/**/*"

# Run only API integration tests
npx cypress run --spec "cypress/e2e/integration/**/*"

# Run rate limiting tests (requires backend with rate limiting enabled)
npx cypress run --spec "cypress/e2e/rate-limiting/**/*"
```

## 📈 Performance Testing

### Response Time Thresholds

- **Page Load**: < 5 seconds
- **API Response**: < 3 seconds
- **User Interaction**: < 1 second

### Load Testing

The E2E tests include basic load testing scenarios:

- Concurrent authentication requests
- Multiple viewport testing
- Performance measurement utilities

## 🔒 Security Testing

The test suite includes security-focused tests:

- JWT token validation
- Sensitive data exposure checks
- HTTPS enforcement
- Session timeout handling
- CSRF protection verification

## 📱 Cross-Platform Testing

### Supported Viewports

- **Mobile**: 320x568 (iPhone SE)
- **Tablet**: 768x1024 (iPad)
- **Desktop**: 1920x1080 (Full HD)

### Accessibility Testing

- ARIA labels and roles
- Keyboard navigation
- Screen reader support
- Focus management

## 🚀 CI/CD Integration

### GitHub Actions (Example)

```yaml
- name: Start Backend (Test Mode)
  run: |
    export SPRING_PROFILES_ACTIVE=test
    export BUCKET4J_ENABLED=false
    ./start-test.sh &
    
- name: Wait for Backend
  run: npx wait-on http://localhost:8080/actuator/health
  
- name: Run E2E Tests
  run: npm run test:e2e:headless
```

## 📝 Best Practices

### 1. Test Environment Management

- Always use test or dev profile for E2E testing
- Clear test data before and after test runs
- Use consistent test credentials

### 2. Test Structure

- Keep tests focused and independent
- Use descriptive test names
- Group related tests in describe blocks

### 3. Error Handling

- Test both success and failure scenarios
- Mock API responses for consistent testing
- Handle network errors gracefully

### 4. Performance

- Set reasonable timeouts
- Use efficient selectors (data-cy attributes)
- Minimize unnecessary waits

### 5. Maintainability

- Use custom commands for common actions
- Keep test data in fixtures
- Document complex test scenarios

## 🎯 Next Steps

1. **Add More Test Coverage**: Extend tests to cover more user workflows
2. **Visual Testing**: Add visual regression testing with Percy or similar
3. **API Contract Testing**: Add contract testing with Pact
4. **Performance Monitoring**: Integrate with performance monitoring tools
5. **Test Reporting**: Add comprehensive test reporting and metrics
