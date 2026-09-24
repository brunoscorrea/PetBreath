package com.petbreath.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.petbreath.app.MainActivity
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.petbreath.app.ui.home.HomeScreen
import com.petbreath.app.ui.learn.LearnScreen
import com.petbreath.app.ui.measure.MeasureScreen
import com.petbreath.app.ui.medication.MedicationsScreen
import com.petbreath.app.ui.onboarding.OnboardingScreen
import com.petbreath.app.ui.pet.PetDetailScreen
import com.petbreath.app.ui.pet.PetEditScreen
import com.petbreath.app.ui.reminder.RemindersScreen
import com.petbreath.app.ui.settings.SettingsScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val PET = "pet/{petId}"
    const val PET_EDIT = "pet_edit?petId={petId}"
    const val MEASURE = "measure/{petId}"
    const val MEDICATIONS = "medications/{petId}"
    const val REMINDERS = "reminders/{petId}"
    const val LEARN = "learn"
    const val SETTINGS = "settings"

    fun pet(id: Long) = "pet/$id"
    fun petEdit(id: Long? = null) = if (id == null) "pet_edit" else "pet_edit?petId=$id"
    fun measure(id: Long) = "measure/$id"
    fun medications(id: Long) = "medications/$id"
    fun reminders(id: Long) = "reminders/$id"
}

/** A destination requested from outside the UI, e.g. by tapping a notification. */
data class DeepLinkRequest(val petId: Long, val destination: String)

private val petIdArg = listOf(navArgument("petId") { type = NavType.LongType })

@Composable
fun PetBreathNavHost(
    showOnboarding: Boolean,
    onAcceptDisclaimer: () -> Unit,
    deepLink: DeepLinkRequest?,
    onDeepLinkHandled: () -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    // Fixed for the lifetime of this host so accepting the disclaimer doesn't rebuild the graph.
    val startDestination = remember { if (showOnboarding) Routes.ONBOARDING else Routes.HOME }
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onAccept = {
                onAcceptDisclaimer()
                navController.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
            })
        }
        composable(Routes.HOME) {
            HomeScreen(
                onAddPet = { navController.navigate(Routes.petEdit()) },
                onOpenPet = { navController.navigate(Routes.pet(it)) },
                onMeasure = { navController.navigate(Routes.measure(it)) },
                onLearn = { navController.navigate(Routes.LEARN) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.PET, arguments = petIdArg) { entry ->
            val petId = entry.arguments?.getLong("petId") ?: return@composable
            PetDetailScreen(
                onBack = { navController.popBackStack() },
                onMeasure = { navController.navigate(Routes.measure(petId)) },
                onEditPet = { navController.navigate(Routes.petEdit(petId)) },
                onMedications = { navController.navigate(Routes.medications(petId)) },
                onReminders = { navController.navigate(Routes.reminders(petId)) },
            )
        }
        composable(
            Routes.PET_EDIT,
            arguments = listOf(navArgument("petId") { type = NavType.LongType; defaultValue = -1L }),
        ) {
            PetEditScreen(
                onBack = { navController.popBackStack() },
                onSaved = { id, isNew ->
                    if (isNew) {
                        navController.navigate(Routes.pet(id)) { popUpTo(Routes.HOME) }
                    } else {
                        navController.popBackStack()
                    }
                },
            )
        }
        composable(Routes.MEASURE, arguments = petIdArg) {
            MeasureScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
        composable(Routes.MEDICATIONS, arguments = petIdArg) {
            MedicationsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.REMINDERS, arguments = petIdArg) {
            RemindersScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.LEARN) { LearnScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.SETTINGS) { SettingsScreen(onBack = { navController.popBackStack() }) }
    }

    LaunchedEffect(deepLink, showOnboarding) {
        val request = deepLink ?: return@LaunchedEffect
        if (showOnboarding) return@LaunchedEffect
        val route = when (request.destination) {
            MainActivity.DESTINATION_MEASURE -> Routes.measure(request.petId)
            MainActivity.DESTINATION_MEDICATIONS -> Routes.medications(request.petId)
            else -> Routes.pet(request.petId)
        }
        navController.navigate(Routes.pet(request.petId)) { popUpTo(Routes.HOME) }
        if (route != Routes.pet(request.petId)) navController.navigate(route)
        onDeepLinkHandled()
    }
}
