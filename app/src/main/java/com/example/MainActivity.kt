package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.DjezzyViewModel
import com.example.ui.screens.*
import com.example.ui.theme.DjezzyRed
import com.example.ui.theme.DjezzyTheme

enum class NavigationTab(val label: String, val icon: ImageVector) {
    HOME("الرئيسية", Icons.Default.Home),
    OFFERS("العروض", Icons.Default.ShoppingCart),
    MGM("دعوة 1Go", Icons.Default.CardGiftcard),
    MIGRATION("التحويل", Icons.Default.SwapHoriz),
    HISTORY("السجل", Icons.Default.History),
    SETTINGS("الإعدادات", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: DjezzyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DjezzyTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    val session by viewModel.currentSession.collectAsStateWithLifecycle()
                    val loginUiState by viewModel.loginUiState.collectAsStateWithLifecycle()
                    val homeUiState by viewModel.homeUiState.collectAsStateWithLifecycle()
                    val offersCategory by viewModel.offersCategory.collectAsStateWithLifecycle()
                    val offersSearchQuery by viewModel.offersSearchQuery.collectAsStateWithLifecycle()
                    val isActivatingOffer by viewModel.isActivatingOffer.collectAsStateWithLifecycle()
                    val activatingOfferName by viewModel.activatingOfferName.collectAsStateWithLifecycle()
                    val offerResultMessage by viewModel.offerResultMessage.collectAsStateWithLifecycle()
                    val mgmUiState by viewModel.mgmUiState.collectAsStateWithLifecycle()
                    val migrationUiState by viewModel.migrationUiState.collectAsStateWithLifecycle()
                    val allActivations by viewModel.allActivations.collectAsStateWithLifecycle()
                    val successCount by viewModel.successCount.collectAsStateWithLifecycle()

                    var selectedTab by remember { mutableStateOf(NavigationTab.HOME) }

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        if (session == null) {
                            LoginScreen(
                                state = loginUiState,
                                onPhoneChanged = viewModel::onPhoneInputChanged,
                                onOtpChanged = viewModel::onOtpInputChanged,
                                onRequestOtp = viewModel::requestOtp,
                                onVerifyOtp = viewModel::verifyOtp
                            )
                        } else {
                            val activeSession = session!!
                            Scaffold(
                                modifier = Modifier.fillMaxSize(),
                                bottomBar = {
                                    NavigationBar(
                                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ) {
                                        NavigationTab.values().forEach { tab ->
                                            NavigationBarItem(
                                                selected = selectedTab == tab,
                                                onClick = { selectedTab = tab },
                                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                                label = { Text(tab.label) },
                                                colors = NavigationBarItemDefaults.colors(
                                                    selectedIconColor = DjezzyRed,
                                                    selectedTextColor = DjezzyRed,
                                                    indicatorColor = DjezzyRed.copy(alpha = 0.12f)
                                                ),
                                                modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                                            )
                                        }
                                    }
                                }
                            ) { innerPadding ->
                                val contentModifier = Modifier
                                    .padding(innerPadding)
                                    .fillMaxSize()

                                when (selectedTab) {
                                    NavigationTab.HOME -> {
                                        HomeScreen(
                                            session = activeSession,
                                            state = homeUiState,
                                            onRefresh = { viewModel.refreshHomeData(activeSession) },
                                            onActivateWalk = viewModel::activateWalk2Go,
                                            onDismissBanner = viewModel::dismissHomeBanner,
                                            onNavigateToOffers = { selectedTab = NavigationTab.OFFERS },
                                            onNavigateToMgm = { selectedTab = NavigationTab.MGM },
                                            onNavigateToMigration = { selectedTab = NavigationTab.MIGRATION },
                                            onNavigateToHistory = { selectedTab = NavigationTab.HISTORY },
                                            modifier = contentModifier
                                        )
                                    }
                                    NavigationTab.OFFERS -> {
                                        OffersScreen(
                                            selectedCategory = offersCategory,
                                            searchQuery = offersSearchQuery,
                                            isActivating = isActivatingOffer,
                                            activatingOfferName = activatingOfferName,
                                            resultMessage = offerResultMessage,
                                            onCategorySelected = viewModel::setOffersCategory,
                                            onSearchQueryChanged = viewModel::setOffersSearchQuery,
                                            onActivateOffer = viewModel::activatePaidOffer,
                                            onDismissResultDialog = viewModel::dismissOfferResultDialog,
                                            modifier = contentModifier
                                        )
                                    }
                                    NavigationTab.MGM -> {
                                        MgmScreen(
                                            state = mgmUiState,
                                            onFriendPhoneChanged = viewModel::onFriendPhoneChanged,
                                            onFriendOtpChanged = viewModel::onFriendOtpChanged,
                                            onSendInvitation = viewModel::sendMgmInvitation,
                                            onVerifyFriendOtp = viewModel::verifyFriendOtpAndClaimReward,
                                            onResetState = viewModel::resetMgmState,
                                            modifier = contentModifier
                                        )
                                    }
                                    NavigationTab.MIGRATION -> {
                                        MigrationScreen(
                                            state = migrationUiState,
                                            onLoadOptions = viewModel::loadMigrationOptions,
                                            onExecuteMigration = viewModel::executeMigration,
                                            modifier = contentModifier
                                        )
                                    }
                                    NavigationTab.HISTORY -> {
                                        HistoryStatsScreen(
                                            records = allActivations,
                                            successCount = successCount,
                                            onClearHistory = viewModel::clearHistory,
                                            modifier = contentModifier
                                        )
                                    }
                                    NavigationTab.SETTINGS -> {
                                        SettingsScreen(
                                            session = activeSession,
                                            onLogout = viewModel::logout,
                                            modifier = contentModifier
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
