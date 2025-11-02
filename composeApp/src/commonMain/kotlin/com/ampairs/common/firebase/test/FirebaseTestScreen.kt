package com.ampairs.common.firebase.test

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ampairs.common.firebase.analytics.AnalyticsEvents
import com.ampairs.common.firebase.analytics.AnalyticsParams
import com.ampairs.common.firebase.analytics.FirebaseAnalytics
import com.ampairs.common.firebase.crashlytics.CrashlyticsKeys
import com.ampairs.common.firebase.crashlytics.FirebaseCrashlytics
import com.ampairs.common.firebase.performance.FirebasePerformance
import com.ampairs.common.firebase.performance.PerformanceAttributes
import com.ampairs.common.firebase.performance.PerformanceTraces
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Test screen for Firebase Analytics, Crashlytics, and Performance Monitoring
 *
 * This screen provides test buttons to verify each Firebase service is working correctly.
 * Use this for integration testing only - remove or disable in production.
 *
 * Usage:
 * 1. Navigate to this screen in your app
 * 2. Tap each test button
 * 3. Verify results in Firebase Console:
 *    - Analytics: https://console.firebase.google.com → Analytics → Events
 *    - Crashlytics: https://console.firebase.google.com → Crashlytics
 *    - Performance: https://console.firebase.google.com → Performance
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirebaseTestScreen() {
    val analytics: FirebaseAnalytics = koinInject()
    val crashlytics: FirebaseCrashlytics = koinInject()
    val performance: FirebasePerformance = koinInject()

    val scope = rememberCoroutineScope()
    var testResults by remember { mutableStateOf<List<String>>(emptyList()) }

    // Track screen view
    LaunchedEffect(Unit) {
        analytics.setCurrentScreen("FirebaseTestScreen", "FirebaseTestScreen")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Firebase Test Screen") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section: Firebase Analytics
            Text(
                "Firebase Analytics",
                style = MaterialTheme.typography.headlineSmall
            )

            Button(
                onClick = {
                    analytics.logEvent(AnalyticsEvents.SELECT_CONTENT, mapOf(
                        AnalyticsParams.CONTENT_TYPE to "test_content",
                        AnalyticsParams.ITEM_ID to "test_item_123"
                    ))
                    testResults = testResults + "✅ Analytics: Logged SELECT_CONTENT event"
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Test Analytics Event")
            }

            Button(
                onClick = {
                    analytics.setUserProperty("test_property", "test_value")
                    analytics.setUserId("test_user_123")
                    testResults = testResults + "✅ Analytics: Set user property and user ID"
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Test User Properties")
            }

            Divider()

            // Section: Firebase Crashlytics
            Text(
                "Firebase Crashlytics",
                style = MaterialTheme.typography.headlineSmall
            )

            Button(
                onClick = {
                    crashlytics.log("Test log message from Firebase test screen")
                    crashlytics.setCustomKey(CrashlyticsKeys.SCREEN_NAME, "FirebaseTestScreen")
                    crashlytics.setCustomKey(CrashlyticsKeys.ACTION, "test_button_click")
                    crashlytics.setCustomKey("test_key_string", "test_value")
                    crashlytics.setCustomKey("test_key_int", 42)
                    crashlytics.setCustomKey("test_key_boolean", true)
                    testResults = testResults + "✅ Crashlytics: Logged breadcrumbs and custom keys"
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Test Crashlytics Logging")
            }

            Button(
                onClick = {
                    val testException = Exception("Test non-fatal exception from Firebase test screen")
                    crashlytics.recordException(testException)
                    testResults = testResults + "✅ Crashlytics: Recorded non-fatal exception"
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Test Non-Fatal Exception")
            }

            Button(
                onClick = {
                    crashlytics.setUserId("test_user_123")
                    testResults = testResults + "✅ Crashlytics: Set user ID"
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Set Crashlytics User ID")
            }

            OutlinedButton(
                onClick = {
                    crashlytics.log("About to force a test crash!")
                    crashlytics.setCustomKey("force_crash", true)
                    // Force a crash - app will restart
                    throw RuntimeException("🧪 Test crash from Firebase test screen - THIS IS EXPECTED")
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("⚠️ FORCE CRASH (Test Only)")
            }

            Text(
                "⚠️ Force crash will restart the app. Check Firebase Console after 5 minutes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )

            Divider()

            // Section: Firebase Performance
            Text(
                "Firebase Performance",
                style = MaterialTheme.typography.headlineSmall
            )

            Button(
                onClick = {
                    scope.launch {
                        val trace = performance.newTrace(PerformanceTraces.SCREEN_LOAD)
                        trace.putAttribute(PerformanceAttributes.SCREEN_NAME, "FirebaseTestScreen")
                        trace.start()

                        // Simulate some work
                        delay(1000)
                        trace.putMetric("items_loaded", 10)

                        trace.stop()
                        testResults = testResults + "✅ Performance: Completed SCREEN_LOAD trace (1 second)"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Test Performance Trace")
            }

            Button(
                onClick = {
                    scope.launch {
                        val trace = performance.newTrace(PerformanceTraces.API_REQUEST)
                        trace.putAttribute(PerformanceAttributes.API_ENDPOINT, "/api/test")
                        trace.putAttribute(PerformanceAttributes.HTTP_METHOD, "GET")
                        trace.start()

                        try {
                            // Simulate API call
                            delay(500)
                            trace.putAttribute(PerformanceAttributes.STATUS_CODE, "200")
                            trace.putMetric("response_size", 1024)
                        } catch (e: Exception) {
                            trace.putAttribute(PerformanceAttributes.STATUS_CODE, "error")
                        } finally {
                            trace.stop()
                            testResults = testResults + "✅ Performance: Completed API_REQUEST trace (500ms)"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Test API Request Trace")
            }

            Button(
                onClick = {
                    scope.launch {
                        val trace = performance.newTrace("custom_test_trace")
                        trace.putAttribute("custom_attribute", "custom_value")
                        trace.start()

                        delay(250)
                        trace.putMetric("custom_metric", 42)
                        trace.incrementMetric("counter", 1)
                        trace.incrementMetric("counter", 1)

                        trace.stop()
                        testResults = testResults + "✅ Performance: Completed custom trace with metrics"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Test Custom Trace")
            }

            Divider()

            // Results Section
            if (testResults.isNotEmpty()) {
                Text(
                    "Test Results:",
                    style = MaterialTheme.typography.titleMedium
                )

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        testResults.forEach { result ->
                            Text(
                                result,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Button(
                    onClick = { testResults = emptyList() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear Results")
                }
            }

            Divider()

            // Instructions
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "How to Verify:",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        "1. Tap test buttons above",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "2. Open Firebase Console (https://console.firebase.google.com)",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "3. Check each section:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "   • Analytics → Events (may take 24 hours)",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "   • Crashlytics (appears in 5-10 minutes)",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "   • Performance (may take 12-24 hours)",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
