package com.ampairs.tally

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController

@Composable
fun TallyApp() {
    val navigator = rememberNavController()

    MaterialTheme {
        NavHost(
            modifier = Modifier.background(Color.White),
            navController = navigator,
            startDestination = Route.TALLY,
        ) {
            tallyNavigation(navigator)
        }
    }
}


object Route {
    const val TALLY = "/com/ampairs/tally"
}