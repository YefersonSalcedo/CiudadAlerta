package com.ciudadalerta.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ciudadalerta.ui.auth.LoginScreen
import com.ciudadalerta.ui.detail.IncidentDetailScreen
import com.ciudadalerta.ui.map.MapScreen
import com.ciudadalerta.ui.myreports.MyReportsScreen
import com.ciudadalerta.ui.notifications.NotificationsScreen
import com.ciudadalerta.ui.profile.ProfileScreen
import com.ciudadalerta.ui.report.CreateReportScreen
import com.ciudadalerta.ui.report.DuplicateDetectionScreen
import com.ciudadalerta.ui.report.GeminiAnalysisScreen
import com.ciudadalerta.ui.report.ReportConfirmationScreen
import com.ciudadalerta.ui.report.ReportViewModel
import com.google.firebase.auth.FirebaseAuth

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Map : Screen("map")
    object CreateReport : Screen("create_report")
    object GeminiAnalysis : Screen("gemini_analysis")
    object DuplicateDetection : Screen("duplicate_detection")
    object ReportConfirmation : Screen("report_confirmation/{id}") {
        fun createRoute(id: String) = "report_confirmation/$id"
    }
    object IncidentDetail : Screen("incident_detail/{id}") {
        fun createRoute(id: String) = "incident_detail/$id"
    }
    object MyReports : Screen("my_reports")
    object Notifications : Screen("notifications")
    object Profile : Screen("profile")
}

@Composable
fun CiudadAlertaNavGraph(navController: NavHostController) {
    val startDestination = remember {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null && !user.isAnonymous) Screen.Map.route else Screen.Login.route
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Login.route) {
            LoginScreen(onLoginSuccess = {
                navController.navigate(Screen.Map.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Map.route) {
            MapScreen(
                onNavigateToNewReport = { navController.navigate(Screen.CreateReport.route) },
                onNavigateToDetail = { id -> navController.navigate(Screen.IncidentDetail.createRoute(id)) },
                onNavigateToMyReports = { navController.navigateTab(Screen.MyReports.route) },
                onNavigateToNotifications = { navController.navigateTab(Screen.Notifications.route) },
                onNavigateToProfile = { navController.navigateTab(Screen.Profile.route) }
            )
        }
        composable(Screen.CreateReport.route) { backStackEntry ->
            val reportViewModel: ReportViewModel = viewModel(backStackEntry)
            CreateReportScreen(
                reportViewModel = reportViewModel,
                onBack = { navController.popBackStack() },
                onAnalyzeWithAI = { navController.navigate(Screen.GeminiAnalysis.route) }
            )
        }
        composable(Screen.GeminiAnalysis.route) { backStackEntry ->
            val parentEntry = remember(backStackEntry) { navController.getBackStackEntry(Screen.CreateReport.route) }
            val reportViewModel: ReportViewModel = viewModel(parentEntry)
            GeminiAnalysisScreen(
                reportViewModel = reportViewModel,
                onBack = { navController.popBackStack() },
                onApproved = { navController.navigate(Screen.DuplicateDetection.route) },
                onRetake = { navController.popBackStack() }
            )
        }
        composable(Screen.DuplicateDetection.route) { backStackEntry ->
            val parentEntry = remember(backStackEntry) { navController.getBackStackEntry(Screen.CreateReport.route) }
            val reportViewModel: ReportViewModel = viewModel(parentEntry)
            DuplicateDetectionScreen(
                reportViewModel = reportViewModel,
                onBack = { navController.popBackStack() },
                onMatchConfirmed = { id ->
                    navController.navigate(Screen.ReportConfirmation.createRoute(id)) {
                        popUpTo(Screen.CreateReport.route) { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = Screen.ReportConfirmation.route,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) {
            ReportConfirmationScreen(
                onViewOnMap = { navController.navigate(Screen.Map.route) { popUpTo(Screen.Map.route) { inclusive = true } } },
                onGoToMyReports = { navController.navigate(Screen.MyReports.route) },
                onCreateAnother = { navController.navigate(Screen.CreateReport.route) }
            )
        }
        composable(
            route = Screen.IncidentDetail.route,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            IncidentDetailScreen(incidenciaId = id, onBack = { navController.popBackStack() })
        }
        composable(Screen.MyReports.route) {
            MyReportsScreen(
                onNavigateToMap = { navController.navigateTab(Screen.Map.route) },
                onNavigateToNotifications = { navController.navigateTab(Screen.Notifications.route) },
                onNavigateToProfile = { navController.navigateTab(Screen.Profile.route) },
                onSelectReport = { id -> navController.navigate(Screen.IncidentDetail.createRoute(id)) },
                onNewReport = { navController.navigate(Screen.CreateReport.route) }
            )
        }
        composable(Screen.Notifications.route) {
            NotificationsScreen(
                onNavigateToMap = { navController.navigateTab(Screen.Map.route) },
                onNavigateToMyReports = { navController.navigateTab(Screen.MyReports.route) },
                onNavigateToProfile = { navController.navigateTab(Screen.Profile.route) }
            )
        }
        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateToMap = { navController.navigateTab(Screen.Map.route) },
                onNavigateToMyReports = { navController.navigateTab(Screen.MyReports.route) },
                onNavigateToNotifications = { navController.navigateTab(Screen.Notifications.route) },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}

private fun NavHostController.navigateTab(route: String) {
    navigate(route) {
        launchSingleTop = true
        popUpTo(Screen.Map.route)
    }
}