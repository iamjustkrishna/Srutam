package space.iamjustkrishna.srutam.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import space.iamjustkrishna.srutam.ui.screens.ChatScreen
import space.iamjustkrishna.srutam.ui.screens.DetailScreen
import space.iamjustkrishna.srutam.ui.screens.FeedScreen

sealed class Screen(val route: String) {
    data object Feed : Screen("feed")
    data object Detail : Screen("detail/{recordingId}") {
        fun createRoute(recordingId: Long) = "detail/$recordingId"
    }
    data object Chat : Screen("chat/{recordingId}") {
        fun createRoute(recordingId: Long) = "chat/$recordingId"
    }
}

@Composable
fun SrutamNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Feed.route
    ) {
        composable(Screen.Feed.route) {
            FeedScreen(
                onRecordingClick = { recordingId ->
                    navController.navigate(Screen.Detail.createRoute(recordingId))
                }
            )
        }

        composable(
            route = Screen.Detail.route,
            arguments = listOf(
                navArgument("recordingId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val recordingId = backStackEntry.arguments?.getLong("recordingId") ?: return@composable
            DetailScreen(
                recordingId = recordingId,
                onNavigateBack = { navController.popBackStack() },
                onShowChat = { navController.navigate(Screen.Chat.createRoute(recordingId)) }
            )
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("recordingId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val recordingId = backStackEntry.arguments?.getLong("recordingId") ?: return@composable
            ChatScreen(
                recordingId = recordingId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
