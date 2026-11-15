package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.data.repository.AuthRepository
import com.example.data.repository.CatalogRepository
import com.example.data.repository.MediaUploadRepository
import com.example.data.repository.ProfileRepository
import com.example.data.repository.SubscriptionRepository
import com.example.data.repository.authStateFlow
import com.example.ui.components.VeloraLogo
import com.example.ui.navigation.VeloraRoute
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.DetailsScreen
import com.example.ui.theme.VeloraBackground
import com.example.ui.theme.VeloraCrimson
import com.example.ui.theme.VeloraError
import com.example.ui.theme.VeloraOnBackground
import com.example.ui.theme.VeloraOnSurfaceMuted
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MyListAndActivityScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.ProfileSelectorScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SignInScreen
import com.example.ui.screens.signOutFromVelora
import com.example.ui.state.VeloraViewModel
import com.example.ui.theme.VeloraBackground
import com.example.ui.theme.VeloraCrimson
import com.example.ui.theme.VeloraOnBackground
import com.example.ui.theme.VeloraOnSurfaceMuted
import com.example.ui.theme.VeloraSurface
import com.example.ui.theme.VeloraTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            VeloraTheme {
                VeloraAppNavigation(windowSizeClass.widthSizeClass)
            }
        }
    }
}

private data class NavDestinationItem(
    val route: VeloraRoute,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun VeloraAppNavigation(
    widthSizeClass: WindowWidthSizeClass,
    auth: FirebaseAuth = Firebase.auth
) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        SignInScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            AuthenticatedVeloraApp(
                currentUserId = user.uid,
                widthSizeClass = widthSizeClass
            )
        }
    }
}

@Composable
private fun AuthenticatedVeloraApp(
    currentUserId: String,
    widthSizeClass: WindowWidthSizeClass
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val navController = rememberNavController()

    val viewModel: VeloraViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                VeloraViewModel(
                    authRepository = AuthRepository(db),
                    profileRepository = ProfileRepository(db, appContext = app),
                    catalogRepository = CatalogRepository(db),
                    subscriptionRepository = SubscriptionRepository(db),
                    currentUserId = currentUserId,
                    mediaUploadRepository = MediaUploadRepository(app)
                )
            }
        }
    )

    val hasSelectedProfile by viewModel.hasSelectedProfile.collectAsStateWithLifecycle()
    val profilesState by viewModel.profilesState.collectAsStateWithLifecycle()
    val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()
    val account by viewModel.accountState.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val catalogState by viewModel.rawCatalogState.collectAsStateWithLifecycle()
    val rawCatalog by viewModel.rawCatalog.collectAsStateWithLifecycle()
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()
    val genres by viewModel.genres.collectAsStateWithLifecycle()
    val watchlist by viewModel.watchlist.collectAsStateWithLifecycle()
    val progressList by viewModel.progressList.collectAsStateWithLifecycle()
    val ratings by viewModel.ratings.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val adminUsers by viewModel.adminUsers.collectAsStateWithLifecycle()
    val userSubscription by viewModel.userSubscription.collectAsStateWithLifecycle()
    val subscriptionPlans by viewModel.subscriptionPlans.collectAsStateWithLifecycle()
    val freePlanFeatures by viewModel.freePlanFeatures.collectAsStateWithLifecycle()
    val adminUserSubscriptions by viewModel.adminUserSubscriptions.collectAsStateWithLifecycle()
    val continueWatching by viewModel.continueWatching.collectAsStateWithLifecycle()
    val recommendations by viewModel.recommendations.collectAsStateWithLifecycle()
    val homeCategory by viewModel.homeCategory.collectAsStateWithLifecycle()
    val homeGenre by viewModel.homeGenreFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val exploreMediaFilter by viewModel.exploreMediaFilter.collectAsStateWithLifecycle()
    val exploreGenreFilter by viewModel.exploreGenreFilter.collectAsStateWithLifecycle()
    val exploreSort by viewModel.exploreSort.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSavingCatalog by viewModel.isSavingCatalog.collectAsStateWithLifecycle()
    val mediaUploadStates by viewModel.mediaUploadStates.collectAsStateWithLifecycle()
    val isLoadingSeriesStructure by viewModel.isLoadingSeriesStructure.collectAsStateWithLifecycle()
    val editingSeriesSeasons by viewModel.editingSeriesSeasons.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    if (account?.isSuspended == true) {
        androidx.compose.material3.Surface(
            modifier = Modifier.fillMaxSize(),
            color = VeloraBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AdminPanelSettings,
                    contentDescription = null,
                    tint = VeloraError,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Account Suspended",
                    style = MaterialTheme.typography.headlineMedium,
                    color = VeloraOnBackground
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Your Notflix account has been suspended by an administrator. Please contact support for assistance.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VeloraOnSurfaceMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { signOutFromVelora(credentialManager, scope) },
                    colors = ButtonDefaults.buttonColors(containerColor = VeloraCrimson)
                ) {
                    Text("Sign Out")
                }
            }
        }
        return
    }

    if (!hasSelectedProfile) {
        ProfileSelectorScreen(
            profilesState = profilesState,
            genres = genres,
            isAdmin = isAdmin,
            accountIdentifier = account?.email?.takeIf { it.isNotBlank() } ?: account?.phoneNumber,
            onSelectProfile = { viewModel.selectProfile(it) },
            onOpenAdminDashboard = { navController.navigate(VeloraRoute.Admin) },
            onSignOut = { signOutFromVelora(credentialManager, scope) },
            onCreateProfile = { name, avatar, isKids, g -> viewModel.createProfile(name, avatar, isKids, g) },
            onUpdateProfile = { id, name, avatar, isKids, g, lang, subs ->
                viewModel.updateProfile(id, name, avatar, isKids, g, lang, subs)
            },
            onDeleteProfile = { viewModel.deleteProfile(it) }
        )
        return
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ""
    val isPlayerOrDetails = currentRoute.contains("Player") || currentRoute.contains("Details") || currentRoute.contains("Admin")
    var myListInitialTab by remember { mutableIntStateOf(0) }

    val navItems = remember(isAdmin) {
        buildList {
            add(NavDestinationItem(VeloraRoute.Home, "Home", Icons.Filled.Home, Icons.Outlined.Home))
            add(NavDestinationItem(VeloraRoute.Explore, "Explore", Icons.Filled.Search, Icons.Outlined.Search))
            add(NavDestinationItem(VeloraRoute.MyList, "My List", Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder))
            add(NavDestinationItem(VeloraRoute.Settings, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings))
            if (isAdmin) {
                add(NavDestinationItem(VeloraRoute.Admin, "Admin", Icons.Filled.AdminPanelSettings, Icons.Outlined.AdminPanelSettings))
            }
        }
    }

    val useRail = widthSizeClass != WindowWidthSizeClass.Compact && !isPlayerOrDetails
    val watchlistIds = remember(watchlist) { watchlist.map { it.contentId }.toSet() }

    Scaffold(
        containerColor = VeloraBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!useRail && !isPlayerOrDetails) {
                NavigationBar(
                    containerColor = VeloraSurface,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    navItems.forEach { dest ->
                        val selected = currentRoute.contains(dest.route::class.simpleName ?: "")
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (dest.route == VeloraRoute.MyList) myListInitialTab = 0
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                    contentDescription = dest.label
                                )
                            },
                            label = { Text(dest.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = VeloraCrimson,
                                selectedTextColor = VeloraOnBackground,
                                unselectedIconColor = VeloraOnSurfaceMuted,
                                unselectedTextColor = VeloraOnSurfaceMuted,
                                indicatorColor = VeloraCrimson.copy(alpha = 0.16f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(VeloraBackground)
        ) {
            if (useRail) {
                NavigationRail(
                    containerColor = VeloraSurface,
                    header = {
                        Box(Modifier.padding(vertical = 12.dp)) {
                            VeloraLogo(compact = true)
                        }
                    }
                ) {
                    navItems.forEach { dest ->
                        val selected = currentRoute.contains(dest.route::class.simpleName ?: "")
                        NavigationRailItem(
                            selected = selected,
                            onClick = {
                                if (dest.route == VeloraRoute.MyList) myListInitialTab = 0
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                    contentDescription = dest.label
                                )
                            },
                            label = { Text(dest.label) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = VeloraCrimson,
                                selectedTextColor = VeloraOnBackground,
                                unselectedIconColor = VeloraOnSurfaceMuted,
                                unselectedTextColor = VeloraOnSurfaceMuted,
                                indicatorColor = VeloraCrimson.copy(alpha = 0.16f)
                            )
                        )
                    }
                }
            }

            NavHost(
                navController = navController,
                startDestination = VeloraRoute.Home,
                modifier = Modifier.weight(1f).fillMaxSize()
            ) {
                composable<VeloraRoute.Home> {
                    HomeScreen(
                        catalogState = catalogState,
                        catalog = catalog,
                        recommendations = recommendations,
                        continueWatching = continueWatching,
                        watchlistIds = watchlistIds,
                        genres = genres,
                        activeProfile = activeProfile,
                        selectedCategory = homeCategory,
                        selectedGenre = homeGenre,
                        unreadNotifications = notifications.count { !it.read },
                        isAdmin = isAdmin,
                        accountIdentifier = account?.email?.takeIf { it.isNotBlank() } ?: account?.phoneNumber,
                        onSelectCategory = viewModel::setHomeCategory,
                        onSelectGenre = viewModel::setHomeGenreFilter,
                        onSwitchProfileClick = viewModel::openProfileSwitcher,
                        onOpenSettings = { navController.navigate(VeloraRoute.Settings) },
                        onOpenAdminDashboard = { navController.navigate(VeloraRoute.Admin) },
                        onSignOut = { signOutFromVelora(credentialManager, scope) },
                        onNotificationsClick = {
                            myListInitialTab = 2
                            navController.navigate(VeloraRoute.MyList)
                        },
                        onOpenDetails = { item -> navController.navigate(VeloraRoute.Details(item.id)) },
                        onPlayItem = { item, entry ->
                            navController.navigate(
                                VeloraRoute.Player(
                                    contentId = item.id,
                                    episodeId = entry?.progress?.episodeId,
                                    seasonNumber = entry?.progress?.seasonNumber,
                                    episodeNumber = entry?.progress?.episodeNumber,
                                    isTrailer = false
                                )
                            )
                        },
                        onToggleWatchlist = viewModel::toggleWatchlist
                    )
                }

                composable<VeloraRoute.Explore> {
                    ExploreScreen(
                        query = searchQuery,
                        mediaFilter = exploreMediaFilter,
                        genreFilter = exploreGenreFilter,
                        sortOption = exploreSort,
                        results = searchResults,
                        genres = genres,
                        searchHistory = searchHistory,
                        watchlistIds = watchlistIds,
                        onQueryChange = viewModel::updateSearchQuery,
                        onSubmitQuery = viewModel::recordSearch,
                        onDeleteHistory = viewModel::deleteSearchHistory,
                        onMediaFilterChange = viewModel::setExploreMediaFilter,
                        onGenreFilterChange = viewModel::setExploreGenreFilter,
                        onSortChange = viewModel::setExploreSort,
                        onResetFilters = viewModel::resetExploreFilters,
                        onOpenDetails = { item -> navController.navigate(VeloraRoute.Details(item.id)) },
                        onPlayItem = { item -> navController.navigate(VeloraRoute.Player(contentId = item.id)) },
                        onToggleWatchlist = viewModel::toggleWatchlist
                    )
                }

                composable<VeloraRoute.MyList> {
                    MyListAndActivityScreen(
                        initialTab = myListInitialTab,
                        catalog = catalog,
                        watchlist = watchlist,
                        progressList = progressList,
                        notifications = notifications,
                        onOpenDetails = { item -> navController.navigate(VeloraRoute.Details(item.id)) },
                        onPlayProgress = { item, entry ->
                            navController.navigate(
                                VeloraRoute.Player(
                                    contentId = item.id,
                                    episodeId = entry.episodeId,
                                    seasonNumber = entry.seasonNumber,
                                    episodeNumber = entry.episodeNumber
                                )
                            )
                        },
                        onToggleWatchlist = viewModel::toggleWatchlist,
                        onDeleteHistoryEntry = viewModel::deleteHistoryEntry,
                        onClearHistory = viewModel::clearWatchHistory,
                        onMarkNotificationRead = viewModel::markNotificationRead
                    )
                }

                composable<VeloraRoute.Settings> {
                    SettingsScreen(
                        account = account,
                        activeProfile = activeProfile,
                        userSubscription = userSubscription,
                        freePlanFeatures = freePlanFeatures,
                        isAdmin = isAdmin,
                        onSwitchProfile = viewModel::openProfileSwitcher,
                        onOpenAdminDashboard = { navController.navigate(VeloraRoute.Admin) },
                        onSavePreferences = viewModel::updatePreferences,
                        onSignOut = { signOutFromVelora(credentialManager, scope) }
                    )
                }

                composable<VeloraRoute.Admin> {
                    AdminDashboardScreen(
                        isAdmin = isAdmin,
                        catalog = rawCatalog,
                        genres = genres,
                        users = adminUsers,
                        subscriptionPlans = subscriptionPlans,
                        userSubscriptions = adminUserSubscriptions,
                        isSavingCatalog = isSavingCatalog,
                        isLoadingSeriesSeasons = isLoadingSeriesStructure,
                        loadedSeriesSeasons = editingSeriesSeasons,
                        onBack = { navController.popBackStack() },
                        onSeedCatalog = viewModel::seedCatalogNow,
                        onSyncSubscriptionSchemas = viewModel::syncSubscriptionSchemasNow,
                        onPrepareSeriesEditor = viewModel::prepareSeriesEditor,
                        onSaveCatalogItem = viewModel::saveCatalogItem,
                        onSaveSeriesWithSeasons = viewModel::saveSeriesWithSeasons,
                        onDeleteCatalogItem = viewModel::deleteCatalogItem,
                        onAddSeason = viewModel::addSeason,
                        onAddEpisode = viewModel::addEpisode,
                        onSaveGenre = viewModel::saveGenre,
                        onDeleteGenre = viewModel::deleteGenre,
                        onToggleUserAdmin = viewModel::setUserAdminRole
                    )
                }

                composable<VeloraRoute.Details> { entry ->
                    val route = entry.toRoute<VeloraRoute.Details>()
                    val item = rawCatalog.firstOrNull { it.id == route.contentId }
                    if (item == null) {
                        Box(Modifier.fillMaxSize().background(VeloraBackground))
                    } else {
                        val seasonsFlow = remember(item.id) { viewModel.observeSeasons(item.id) }
                        DetailsScreen(
                            item = item,
                            inWatchlist = item.id in watchlistIds,
                            rating = ratings.firstOrNull { it.contentId == item.id },
                            progressList = progressList,
                            similarTitles = viewModel.getSimilarTitles(item),
                            seasonsFlow = seasonsFlow,
                            episodesFlowProvider = { seasonId, seasonNum ->
                                viewModel.observeEpisodes(item.id, seasonId, seasonNum)
                            },
                            onBack = { navController.popBackStack() },
                            onPlayMovieOrDefault = {
                                navController.navigate(VeloraRoute.Player(contentId = item.id))
                            },
                            onPlayTrailer = {
                                navController.navigate(VeloraRoute.Player(contentId = item.id, isTrailer = true))
                            },
                            onPlayEpisode = { ep ->
                                navController.navigate(
                                    VeloraRoute.Player(
                                        contentId = item.id,
                                        episodeId = ep.id,
                                        seasonNumber = ep.seasonNumber,
                                        episodeNumber = ep.episodeNumber
                                    )
                                )
                            },
                            onToggleWatchlist = { viewModel.toggleWatchlist(item) },
                            onLikeDislike = { liked -> viewModel.setLikeDislike(item.id, liked) },
                            onStarRate = { stars -> viewModel.setStarRating(item.id, stars) },
                            onOpenDetails = { nextItem ->
                                navController.navigate(VeloraRoute.Details(nextItem.id))
                            }
                        )
                    }
                }

                composable<VeloraRoute.Player> { entry ->
                    val route = entry.toRoute<VeloraRoute.Player>()
                    val item = rawCatalog.firstOrNull { it.id == route.contentId }
                    if (item != null) {
                        val seasonNum = route.seasonNumber ?: 1
                        val episodesFlow = remember(item.id, seasonNum) {
                            viewModel.observeEpisodes(item.id, "season-$seasonNum", seasonNum)
                        }
                        val episodes by episodesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
                        val activeEp = route.episodeId?.let { eid -> episodes.firstOrNull { it.id == eid } }
                            ?: if (item.isSeries) episodes.firstOrNull() else null
                        val nextEp = if (activeEp != null) {
                            episodes.firstOrNull { it.episodeNumber == activeEp.episodeNumber + 1 }
                        } else null

                        val streamUrl = when {
                            route.isTrailer && !item.trailerUrl.isNullOrBlank() -> item.trailerUrl
                            activeEp != null -> activeEp.preferredStreamUrl
                            else -> item.preferredStreamUrl
                        }
                        val streamSourceType = when {
                            route.isTrailer -> null
                            activeEp != null -> activeEp.videoSourceType
                            else -> item.videoSourceType
                        }
                        val savedProgress = progressList.firstOrNull {
                            it.contentId == item.id && (route.episodeId == null || it.episodeId == route.episodeId)
                        }
                        val subtitleLabel = when {
                            route.isTrailer -> "Official Preview Trailer"
                            activeEp != null -> "S${activeEp.seasonNumber} E${activeEp.episodeNumber}: ${activeEp.title}"
                            else -> "${item.releaseYear} • ${item.ageRating}"
                        }

                        PlayerScreen(
                            contentId = item.id,
                            title = item.title,
                            subtitleLabel = subtitleLabel,
                            streamUrl = streamUrl,
                            streamSourceType = streamSourceType,
                            episodeId = activeEp?.id ?: route.episodeId,
                            seasonNumber = activeEp?.seasonNumber ?: route.seasonNumber,
                            episodeNumber = activeEp?.episodeNumber ?: route.episodeNumber,
                            initialPositionMs = if (route.isTrailer) 0L else (savedProgress?.positionMs ?: 0L),
                            defaultQuality = account?.defaultQuality ?: "Auto",
                            defaultSubtitlesEnabled = activeProfile?.subtitlesEnabled ?: (account?.subtitlesEnabled ?: true),
                            autoplayNext = account?.autoplayNext ?: true,
                            nextEpisode = if (route.isTrailer) null else nextEp,
                            onSaveProgress = { pos, dur ->
                                if (!route.isTrailer && dur > 0L) {
                                    viewModel.savePlaybackProgress(
                                        contentId = item.id,
                                        episodeId = activeEp?.id ?: route.episodeId,
                                        seasonNumber = activeEp?.seasonNumber ?: route.seasonNumber,
                                        episodeNumber = activeEp?.episodeNumber ?: route.episodeNumber,
                                        title = activeEp?.let { "${item.title}: ${it.title}" } ?: item.title,
                                        positionMs = pos,
                                        durationMs = dur
                                    )
                                }
                            },
                            onPlayNextEpisode = { next ->
                                navController.navigate(
                                    VeloraRoute.Player(
                                        contentId = item.id,
                                        episodeId = next.id,
                                        seasonNumber = next.seasonNumber,
                                        episodeNumber = next.episodeNumber
                                    )
                                ) {
                                    popUpTo(route) { inclusive = true }
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
