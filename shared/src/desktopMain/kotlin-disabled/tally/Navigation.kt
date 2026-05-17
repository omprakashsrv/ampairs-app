package com.ampairs.tally

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.ampairs.tally.ui.TallyScreen

fun NavGraphBuilder.tallyNavigation(navigator: NavController) {

    navigation(route = Route.TALLY, startDestination = TallyPath.ROOT) {
        composable(
            // Scene's route path
            route = TallyPath.ROOT,
            // Navigation transition for this scene, this is optional
        ) {
            TallyScreen()
        }
    }
}


object TallyPath {
    const val ROOT = Route.TALLY + "/"
}