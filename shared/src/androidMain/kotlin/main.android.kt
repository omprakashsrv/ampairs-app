import androidx.compose.runtime.Composable
import com.ampairs.customer.ui.components.location.LocationServiceMapHandler
import com.ampairs.di.AppGraphHolder

actual fun getPlatformName(): String = "Android"

@Composable
fun MainView() {
    val appGraph = AppGraphHolder.graph ?: error("AppGraph not initialized")
    // Handle location service requests (permissions and map selection)
    LocationServiceMapHandler(appGraph.locationService)

    // Main app content - Material 3 Scaffold handles edge-to-edge automatically
    App(appGraph, {})
}
