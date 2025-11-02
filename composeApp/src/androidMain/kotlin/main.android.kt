import androidx.compose.runtime.Composable
import com.ampairs.customer.ui.components.location.LocationServiceMapHandler

actual fun getPlatformName(): String = "Android"

@Composable
fun MainView() {
    // Handle location service requests (permissions and map selection)
    LocationServiceMapHandler()

    // Main app content
    App({})
}
