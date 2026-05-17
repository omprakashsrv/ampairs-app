# Firebase Performance Monitoring Integration Guide

This guide explains how Firebase Performance is automatically integrated throughout the Ampairs KMP mobile application.

## 🎯 Overview

Firebase Performance monitoring tracks:
- ✅ **Automatic Screen Load Times** - Every navigation tracked via Compose Navigation
- ✅ **API Request Performance** - Helper functions for tracking network calls
- ✅ **Database Operations** - Track Room database performance
- ✅ **Custom Operations** - Track any performance-critical code

## 📊 Automatic Performance Tracking

### Screen Load Performance

**Screen load times are automatically tracked** via Compose Navigation integration in `AppNavigation.kt`:

```kotlin
// Automatic tracking - no manual code required!
// When navigating: navController.navigate(Route.Customer)
// Performance trace started: "screen_load"
// Attributes: { screen_name: "Customer", screen_class: "Route.Customer" }
// Trace auto-stops after 3 seconds or on next navigation
```

**Implementation Details:**
- **Trace Name**: `screen_load` (from `PerformanceTraces.SCREEN_LOAD`)
- **Attributes Tracked**:
  - `screen_name`: Extracted screen name (e.g., "Customer", "Workspace_Members")
  - `screen_class`: Full route class (e.g., "Route.Customer")
  - `success`: "true" (auto-set after timeout)
- **Auto-Stop**: Traces stop after 3 seconds or when navigating away

**Console Output:**
```
Performance: Screen load trace started - Customer
Analytics: Screen view tracked - Customer
Performance: Screen load trace auto-stopped - Customer
```

## 🚀 Manual Performance Tracking

### 1. Track API Requests

Use `trackApiPerformance()` helper to automatically track API performance:

**File**: `CustomerRepository.kt`

```kotlin
import com.ampairs.common.firebase.performance.FirebasePerformance
import com.ampairs.common.firebase.performance.trackApiPerformance

class CustomerRepository(
    private val customerApi: CustomerApi,
    private val performance: FirebasePerformance, // Inject via Koin
    // ... other dependencies
) {

    suspend fun getCustomers(): Response<List<Customer>> {
        return trackApiPerformance(
            performance = performance,
            endpoint = "/api/v1/customers",
            method = "GET"
        ) {
            customerApi.getCustomers()
        }
    }

    suspend fun createCustomer(customer: Customer): Response<Customer> {
        return trackApiPerformance(
            performance = performance,
            endpoint = "/api/v1/customers",
            method = "POST"
        ) {
            customerApi.createCustomer(customer)
        }
    }
}
```

**Tracked Attributes:**
- `api_endpoint`: "/api/v1/customers"
- `http_method`: "GET", "POST", "PUT", "DELETE"
- `success`: "true" or "false"
- `status_code`: "200", "error", "exception"
- `error_message`: Error details if request fails
- `exception_type`: Exception class name if thrown

**Firebase Console Output:**
```
Trace: api_request
Duration: 245ms
Attributes:
  api_endpoint: /api/v1/customers
  http_method: GET
  success: true
  status_code: 200
```

### 2. Track Database Operations

Use `trackPerformance()` for custom operations like database queries:

```kotlin
import com.ampairs.common.firebase.performance.FirebasePerformance
import com.ampairs.common.firebase.performance.PerformanceTraces
import com.ampairs.common.firebase.performance.PerformanceAttributes
import com.ampairs.common.firebase.performance.trackPerformance

class CustomerRepository(
    private val customerDao: CustomerDao,
    private val performance: FirebasePerformance,
    // ...
) {

    suspend fun saveCustomersToDatabase(customers: List<Customer>) {
        trackPerformance(
            performance = performance,
            traceName = PerformanceTraces.DATABASE_WRITE,
            attributes = mapOf(
                PerformanceAttributes.OPERATION_TYPE to "batch_insert",
                PerformanceAttributes.ENTITY_NAME to "Customer",
                "item_count" to customers.size.toString()
            )
        ) {
            customerDao.insertAll(customers.map { it.toEntity() })
        }
    }

    suspend fun searchCustomers(query: String): List<Customer> {
        return trackPerformance(
            performance = performance,
            traceName = PerformanceTraces.DATABASE_READ,
            attributes = mapOf(
                PerformanceAttributes.OPERATION_TYPE to "search",
                PerformanceAttributes.ENTITY_NAME to "Customer",
                "search_query_length" to query.length.toString()
            )
        ) {
            customerDao.searchByName(query).map { it.toDomain() }
        }
    }
}
```

### 3. Track Custom Metrics

Use `measureAndRecord()` to add custom metrics to existing traces:

```kotlin
suspend fun syncCustomersInBatches() {
    val trace = performance.newTrace("customer_sync")
    trace.putAttribute("sync_type", "full")
    trace.start()

    try {
        // Measure batch download time
        val customers = trace.measureAndRecord("download_time_ms") {
            customerApi.getAllCustomers()
        }

        // Measure database write time
        trace.measureAndRecord("database_write_time_ms") {
            customerDao.insertAll(customers.map { it.toEntity() })
        }

        trace.putMetric("items_synced", customers.size.toLong())
        trace.putAttribute("success", "true")
    } catch (e: Exception) {
        trace.putAttribute("success", "false")
        trace.putAttribute("error", e.message ?: "Unknown error")
    } finally {
        trace.stop()
    }
}
```

**Firebase Console Output:**
```
Trace: customer_sync
Duration: 1,234ms
Attributes:
  sync_type: full
  success: true
Metrics:
  download_time_ms: 980
  database_write_time_ms: 254
  items_synced: 150
```

### 4. Track User Flows

Track complex user flows with multiple steps:

```kotlin
class CustomerFormViewModel(
    private val performance: FirebasePerformance,
    // ...
) : ViewModel() {

    fun saveCustomer(customer: Customer) {
        viewModelScope.launch {
            val trace = performance.newTrace(PerformanceTraces.USER_FLOW)
            trace.putAttribute("flow_name", "create_customer")
            trace.start()

            try {
                // Step 1: Validate
                trace.measureAndRecord("validation_time_ms") {
                    validateCustomer(customer)
                }

                // Step 2: Generate UID
                trace.measureAndRecord("uid_generation_time_ms") {
                    customer.uid = UidGenerator.generateUid(Constants.UID_PREFIX)
                }

                // Step 3: Save to database
                trace.measureAndRecord("local_save_time_ms") {
                    repository.createCustomer(customer)
                }

                // Step 4: Sync to server
                trace.measureAndRecord("server_sync_time_ms") {
                    repository.syncToServer(customer)
                }

                trace.putAttribute("success", "true")
            } catch (e: Exception) {
                trace.putAttribute("success", "false")
                trace.putAttribute("error_type", e::class.simpleName ?: "Unknown")
            } finally {
                trace.stop()
            }
        }
    }
}
```

## 📋 Pre-defined Trace Names

**File**: `PerformanceTraces.kt`

```kotlin
object PerformanceTraces {
    const val SCREEN_LOAD = "screen_load"
    const val API_REQUEST = "api_request"
    const val DATABASE_READ = "database_read"
    const val DATABASE_WRITE = "database_write"
    const val USER_FLOW = "user_flow"
    const val IMAGE_UPLOAD = "image_upload"
    const val SYNC_OPERATION = "sync_operation"
}
```

## 📋 Pre-defined Attributes

**File**: `PerformanceAttributes.kt`

```kotlin
object PerformanceAttributes {
    const val SCREEN_NAME = "screen_name"
    const val SCREEN_CLASS = "screen_class"
    const val API_ENDPOINT = "api_endpoint"
    const val HTTP_METHOD = "http_method"
    const val STATUS_CODE = "status_code"
    const val SUCCESS = "success"
    const val OPERATION_TYPE = "operation_type"
    const val ENTITY_NAME = "entity_name"
    // Add custom attributes as needed
}
```

## 🔧 Dependency Injection

Add `FirebasePerformance` to your repository/ViewModel via Koin:

```kotlin
// In your module definition (e.g., CustomerModule.kt)
val customerModule = module {
    single {
        CustomerRepository(
            customerDao = get(),
            customerApi = get(),
            performance = get(), // ← Inject Firebase Performance
            // ... other dependencies
        )
    }
}

// Usage in repository/ViewModel
class CustomerRepository(
    private val performance: FirebasePerformance,
    // ...
) {
    // Use performance tracking
}
```

## 📊 Firebase Console

View performance data at:
- **Performance Dashboard**: https://console.firebase.google.com → Performance
- **Traces**: View all custom traces and their metrics
- **Network Requests**: Automatic HTTP/HTTPS request tracking (Android/iOS)
- **Screen Rendering**: App start time, screen rendering (automatic on Android/iOS)

**Metrics Available:**
- Trace duration (automatic)
- Custom metrics (via `putMetric()`)
- Success rates (via success/failure attributes)
- P50, P90, P99 percentiles
- Time series graphs

## ⚡ Performance Best Practices

### Do's ✅
- ✅ Track critical user flows (login, checkout, data sync)
- ✅ Track slow operations (API calls > 200ms, DB queries > 100ms)
- ✅ Use consistent trace and attribute names
- ✅ Add meaningful attributes for filtering in console
- ✅ Track success/failure rates
- ✅ Measure individual steps in complex operations

### Don'ts ❌
- ❌ Don't create too many custom traces (Firebase has limits)
- ❌ Don't track trivial operations (< 10ms)
- ❌ Don't include PII in trace names or attributes
- ❌ Don't create traces in tight loops
- ❌ Don't forget to stop traces (use try-finally or helper functions)
- ❌ Don't use dynamic attribute values (use categories instead)

### Limits
- **Max trace name length**: 100 characters
- **Max attribute name length**: 40 characters
- **Max attribute value length**: 100 characters
- **Max attributes per trace**: 5
- **Max metrics per trace**: 32
- **Max custom traces**: 500 per app session

## 🧪 Testing Performance Tracking

### Local Testing

Performance traces are logged to console:

```
Performance: Screen load trace started - Customer
Performance: Screen load trace auto-stopped - Customer
API Request trace: /api/v1/customers (GET) - 245ms - Success
Database Write trace: Customer batch_insert - 120ms - Success
```

### Firebase Console (Development)

1. Run the app in debug mode
2. Navigate through screens and trigger operations
3. Wait 12-24 hours for data to appear in Firebase Console
4. View traces in Performance → Dashboard → Custom Traces

### Production Monitoring

- Enable/disable collection: `performance.setPerformanceCollectionEnabled(true)`
- Data appears faster in production (1-2 hours)
- Automatic network request tracking (no code needed)

## 📝 Example: Complete Implementation

Here's a complete example showing performance tracking in a repository:

```kotlin
class CustomerRepository(
    private val customerDao: CustomerDao,
    private val customerApi: CustomerApi,
    private val performance: FirebasePerformance,
    private val analytics: FirebaseAnalytics,
) {

    /**
     * Fetch customers with automatic performance and analytics tracking
     */
    suspend fun fetchCustomers(): Result<List<Customer>> {
        return try {
            // Track API performance
            val response = trackApiPerformance(
                performance = performance,
                endpoint = "/api/v1/customers",
                method = "GET"
            ) {
                customerApi.getCustomers()
            }

            if (response.data != null) {
                // Track database write performance
                trackPerformance(
                    performance = performance,
                    traceName = PerformanceTraces.DATABASE_WRITE,
                    attributes = mapOf(
                        PerformanceAttributes.OPERATION_TYPE to "batch_insert",
                        PerformanceAttributes.ENTITY_NAME to "Customer"
                    )
                ) {
                    customerDao.insertAll(response.data.map { it.toEntity() })
                }

                // Log analytics event
                analytics.logEvent("customers_fetched", mapOf(
                    "count" to response.data.size.toString()
                ))

                Result.success(response.data)
            } else {
                Result.failure(Exception(response.error?.message ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

## 🔗 Related Documentation

- [FIREBASE_INTEGRATION_GUIDE.md](./FIREBASE_INTEGRATION_GUIDE.md) - Complete Firebase integration overview
- [FCM_SETUP_GUIDE.md](./FCM_SETUP_GUIDE.md) - Firebase Cloud Messaging setup
- [Firebase Performance Documentation](https://firebase.google.com/docs/perf-mon)

---

**Last Updated**: January 2025
**Integration Status**: ✅ Automatic screen tracking, ✅ Helper functions available
**KMP Version**: Kotlin 2.2.21, Compose Multiplatform 1.9.1
**Firebase Performance SDK**: iOS 11.13.0, Android 21.0.2
