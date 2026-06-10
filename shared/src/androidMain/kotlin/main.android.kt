import ampairsapp.shared.generated.resources.Res
import ampairsapp.shared.generated.resources.press_back_again_to_exit
import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.ampairs.formwidgets.location.LocationServiceMapHandler
import com.ampairs.di.AndroidAppGraph
import com.ampairs.di.AppGraphHolder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

actual fun getPlatformName(): String = "Android"

@Composable
fun MainView() {
    val appGraph = AppGraphHolder.graph ?: error("AppGraph not initialized")
    LocationServiceMapHandler(appGraph.locationService)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var backPressedOnce by remember { mutableStateOf(false) }
    val exitMessage = stringResource(Res.string.press_back_again_to_exit)

    // NavDisplay's internal BackHandler (enabled when backstack > 1) has higher priority
    // because it is registered deeper in the composition tree. This handler only fires
    // when the user is on the last screen (backstack size == 1).
    BackHandler {
        if (backPressedOnce) {
            (context as? Activity)?.finish()
        } else {
            backPressedOnce = true
            Toast.makeText(context, exitMessage, Toast.LENGTH_SHORT).show()
            scope.launch {
                delay(2000L)
                backPressedOnce = false
            }
        }
    }

    App(appGraph = appGraph as AndroidAppGraph)
}
