package com.rotguard.app

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rotguard.app.designsystem.FloatingPillNavBar
import com.rotguard.app.designsystem.NavBarItems
import com.rotguard.app.designsystem.VerdantCanvasBackdrop
import com.rotguard.app.designsystem.VerdantTheme
import com.rotguard.app.feature.apps.GuardedAppsScreen
import com.rotguard.app.feature.apps.GuardedAppsViewModel
import com.rotguard.app.feature.dashboard.DashboardAction
import com.rotguard.app.feature.dashboard.DashboardEffect
import com.rotguard.app.feature.dashboard.DashboardScreen
import com.rotguard.app.feature.dashboard.DashboardViewModel
import com.rotguard.app.feature.insights.InsightsAction
import com.rotguard.app.feature.insights.InsightsScreen
import com.rotguard.app.feature.insights.InsightsViewModel
import com.rotguard.app.feature.inspiration.InspirationAction
import com.rotguard.app.feature.inspiration.InspirationEffect
import com.rotguard.app.feature.inspiration.InspirationScreen
import com.rotguard.app.feature.inspiration.InspirationViewModel
import com.rotguard.app.feature.onboarding.OnboardingAction
import com.rotguard.app.feature.onboarding.OnboardingEffect
import com.rotguard.app.feature.onboarding.OnboardingScreen
import com.rotguard.app.feature.onboarding.OnboardingViewModel
import com.rotguard.app.feature.settings.SettingsAction
import com.rotguard.app.feature.settings.SettingsEffect
import com.rotguard.app.feature.settings.SettingsScreen
import com.rotguard.app.feature.settings.SettingsViewModel
import com.rotguard.app.feature.shield.FrictionFallbackActivity
import com.rotguard.app.feature.tutorial.GuidedPermission
import com.rotguard.app.feature.tutorial.PermissionTutorialActivity

private const val ROUTE_ONBOARDING = "onboarding"
private const val ROUTE_MAIN = "main"
private const val ROUTE_INSPIRATION = "inspiration"

private enum class MainTab(@StringRes val labelRes: Int) {
    HOME(R.string.nav_home),
    APPS(R.string.nav_apps),
    INSIGHTS(R.string.nav_insights),
    SETTINGS(R.string.nav_settings),
}

@Composable
fun RotGuardNavGraph(
    launchState: RotGuardLaunchState,
) {
    when (launchState) {
        RotGuardLaunchState.Loading -> VerdantCanvasBackdrop {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = VerdantTheme.colors.mossAccent)
            }
        }
        is RotGuardLaunchState.Ready -> RotGuardNavHost(
            startRoute = if (launchState.showOnboardingFirst) ROUTE_ONBOARDING else ROUTE_MAIN,
        )
    }
}

@Composable
private fun RotGuardNavHost(startRoute: String) {
    val navController = rememberNavController()
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = startRoute,
    ) {
        composable(ROUTE_ONBOARDING) {
            OnboardingRoute(
                onCompleted = {
                    navController.navigate(ROUTE_MAIN) {
                        popUpTo(ROUTE_ONBOARDING) { inclusive = true }
                    }
                },
                context = context,
            )
        }
        composable(ROUTE_MAIN) {
            MainShellRoute(
                onReplayOnboarding = {
                    navController.navigate(ROUTE_ONBOARDING)
                },
                onOpenInspiration = {
                    navController.navigate(ROUTE_INSPIRATION)
                },
                context = context,
            )
        }
        composable(ROUTE_INSPIRATION) {
            InspirationRoute(
                onBack = { navController.popBackStack() },
                context = context,
            )
        }
    }
}

@Composable
private fun OnboardingRoute(
    onCompleted: () -> Unit,
    context: Context,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ObserveResume { viewModel.onAction(OnboardingAction.PermissionsRefreshed) }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                OnboardingEffect.OpenAccessibilitySettings ->
                    launchPermissionTutorial(context, GuidedPermission.ACCESSIBILITY)
                OnboardingEffect.OpenOverlaySettings ->
                    launchPermissionTutorial(context, GuidedPermission.OVERLAY)
                OnboardingEffect.NavigateToDashboard -> onCompleted()
            }
        }
    }

    OnboardingScreen(state = uiState, onAction = viewModel::onAction)
}

@Composable
private fun InspirationRoute(
    onBack: () -> Unit,
    context: Context,
    viewModel: InspirationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                InspirationEffect.NavigateBack -> onBack()
                InspirationEffect.LaunchBlockerPreview -> {
                    context.startActivity(FrictionFallbackActivity.createDemoIntent(context))
                }
            }
        }
    }

    InspirationScreen(state = uiState, onAction = viewModel::onAction)
}

@Composable
private fun MainShellRoute(
    onReplayOnboarding: () -> Unit,
    onOpenInspiration: () -> Unit,
    context: Context,
) {
    var activeTab by rememberSaveable { mutableStateOf(MainTab.HOME) }
    val dashboardVm: DashboardViewModel = hiltViewModel()
    val appsVm: GuardedAppsViewModel = hiltViewModel()
    val insightsVm: InsightsViewModel = hiltViewModel()
    val settingsVm: SettingsViewModel = hiltViewModel()

    ObserveResume {
        dashboardVm.onAction(DashboardAction.RefreshRequested)
        insightsVm.onAction(InsightsAction.RefreshRequested)
        settingsVm.onAction(SettingsAction.RefreshRequested)
    }

    HandleMainShellEffects(
        dashboardVm = dashboardVm,
        settingsVm = settingsVm,
        onSelectTab = { activeTab = it },
        onReplayOnboarding = onReplayOnboarding,
        onOpenInspiration = onOpenInspiration,
        context = context,
    )

    Scaffold(
        containerColor = VerdantTheme.colors.canvas,
        bottomBar = {
            FloatingPillNavBar(
                items = NavBarItems(MainTab.entries.map { stringResource(it.labelRes) }),
                activeIndex = activeTab.ordinal,
                onSelectIndex = { index ->
                    val selected = MainTab.entries[index]
                    activeTab = selected
                    refreshTabOnSelect(selected, dashboardVm, insightsVm, settingsVm)
                },
            )
        },
    ) { innerPadding ->
        // Top inset only: tab content scrolls behind the floating bar and fades out under it.
        Box(modifier = Modifier.padding(top = innerPadding.calculateTopPadding())) {
            MainShellTabContent(
                activeTab = activeTab,
                dashboardVm = dashboardVm,
                appsVm = appsVm,
                insightsVm = insightsVm,
                settingsVm = settingsVm,
            )
        }
    }
}

@Composable
private fun HandleMainShellEffects(
    dashboardVm: DashboardViewModel,
    settingsVm: SettingsViewModel,
    onSelectTab: (MainTab) -> Unit,
    onReplayOnboarding: () -> Unit,
    onOpenInspiration: () -> Unit,
    context: Context,
) {
    LaunchedEffect(dashboardVm) {
        dashboardVm.effects.collect { effect ->
            when (effect) {
                DashboardEffect.LaunchDemoShield -> {
                    context.startActivity(FrictionFallbackActivity.createDemoIntent(context))
                }
                DashboardEffect.NavigateToGuardedApps -> onSelectTab(MainTab.APPS)
                DashboardEffect.NavigateToSettingsPermissions -> onSelectTab(MainTab.SETTINGS)
                DashboardEffect.NavigateToInspiration -> onOpenInspiration()
            }
        }
    }

    LaunchedEffect(settingsVm) {
        settingsVm.effects.collect { effect ->
            when (effect) {
                SettingsEffect.OpenAccessibilitySettings ->
                    launchPermissionTutorial(context, GuidedPermission.ACCESSIBILITY)
                SettingsEffect.OpenOverlaySettings ->
                    launchPermissionTutorial(context, GuidedPermission.OVERLAY)
                SettingsEffect.OpenUsageAccessSettings ->
                    launchPermissionTutorial(context, GuidedPermission.USAGE_ACCESS)
                SettingsEffect.NavigateToInspiration -> onOpenInspiration()
                SettingsEffect.ReplayOnboarding -> onReplayOnboarding()
            }
        }
    }
}

@Composable
private fun MainShellTabContent(
    activeTab: MainTab,
    dashboardVm: DashboardViewModel,
    appsVm: GuardedAppsViewModel,
    insightsVm: InsightsViewModel,
    settingsVm: SettingsViewModel,
) {
    when (activeTab) {
        MainTab.HOME -> {
            val state by dashboardVm.uiState.collectAsStateWithLifecycle()
            DashboardScreen(state = state, onAction = dashboardVm::onAction)
        }
        MainTab.APPS -> {
            val state by appsVm.uiState.collectAsStateWithLifecycle()
            GuardedAppsScreen(state = state, onAction = appsVm::onAction)
        }
        MainTab.INSIGHTS -> {
            val state by insightsVm.uiState.collectAsStateWithLifecycle()
            InsightsScreen(state = state, onAction = insightsVm::onAction)
        }
        MainTab.SETTINGS -> {
            val state by settingsVm.uiState.collectAsStateWithLifecycle()
            SettingsScreen(state = state, onAction = settingsVm::onAction)
        }
    }
}

private fun refreshTabOnSelect(
    selected: MainTab,
    dashboardVm: DashboardViewModel,
    insightsVm: InsightsViewModel,
    settingsVm: SettingsViewModel,
) {
    when (selected) {
        MainTab.HOME -> dashboardVm.onAction(DashboardAction.RefreshRequested)
        MainTab.INSIGHTS -> insightsVm.onAction(InsightsAction.RefreshRequested)
        MainTab.SETTINGS -> settingsVm.onAction(SettingsAction.RefreshRequested)
        MainTab.APPS -> Unit
    }
}

@Composable
private fun ObserveResume(onResumed: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onResumed()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

private fun launchPermissionTutorial(context: Context, permission: GuidedPermission) {
    context.startActivity(PermissionTutorialActivity.createIntent(context, permission))
}
