import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ampairs.customer.ui.components.location.LocationServiceMapHandler

actual fun getPlatformName(): String = "Android"

@Composable
fun MainView() {
    // Handle location service requests (permissions and map selection)
    LocationServiceMapHandler()

    // Main app content
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        // Your content here
        App({})
    }
}
