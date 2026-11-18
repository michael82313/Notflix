package com.example.ui.state

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppRole
import com.example.data.model.AuditLogEntry
import com.example.data.model.CatalogItem
import com.example.data.model.ContentRating
import com.example.data.model.ContentReviewDecision
import com.example.data.model.ContentReviewStatus
import com.example.data.model.ContentSubmission
import com.example.data.model.Episode
import com.example.data.model.Genre
import com.example.data.model.HomepageRowRule
import com.example.data.model.HomepageSettings
import com.example.data.model.PlanFeature
import com.example.data.model.PlatformSettings
import com.example.data.model.PlaybackProgress
import com.example.data.model.PublisherApplication
import com.example.data.model.PublisherStatus
import com.example.data.model.RoleGovernance
import com.example.data.model.SearchHistoryItem
import com.example.data.model.Season
import com.example.data.model.SeasonDraft
import com.example.data.model.SubscriptionCatalogDefaults
import com.example.data.model.SubscriptionPlan
import com.example.data.model.UiState
import com.example.data.model.UserAccount
import com.example.data.model.UserNotification
import com.example.data.model.UserProfile
import com.example.data.model.UserSubscription
import com.example.data.model.WatchlistItem
import com.example.data.repository.AuthRepository
import com.example.data.repository.CatalogRepository
import com.example.data.repository.GovernanceRepository
import com.example.data.repository.MediaAssetCategory
import com.example.data.repository.MediaUploadRepository
import com.example.data.repository.MediaUploadState
import com.example.data.repository.ProfileRepository
import com.example.data.repository.SubscriptionRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HomeCategoryFilter(val label: String) {
    ALL("All"),
    MOVIES("Movies"),
    SERIES("TV Shows"),
    NEW_POPULAR("New & Popular"),
    KIDS("Kids Safe")
}

enum class SortOption(val label: String) {
    TRENDING("Trending"),
    NEWEST("Release Year"),
    TITLE_AZ("Title A-Z"),
    RUNTIME("Runtime")
}

data class ContinueWatchingEntry(
    val progress: PlaybackProgress,
    val catalogItem: CatalogItem
)

class VeloraViewModel(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val catalogRepository: CatalogRepository,
    private val subscriptionRepository: SubscriptionRepository,
    val currentUserId: String,
    private val mediaUploadRepository: MediaUploadRepository? = null,
    private val governanceRepository: GovernanceRepository? = null
) : ViewModel() {

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isSavingCatalog = MutableStateFlow(false)
    val isSavingCatalog: StateFlow<Boolean> = _isSavingCatalog.asStateFlow()

    private val _mediaUploadStates = MutableStateFlow<Map<String, MediaUploadState>>(emptyMap())
    val mediaUploadStates: StateFlow<Map<String, MediaUploadState>> = _mediaUploadStates.asStateFlow()

    private val _isLoadingSeriesStructure = MutableStateFlow(false)
    val isLoadingSeriesStructure: StateFlow<Boolean> = _isLoadingSeriesStructure.asStateFlow()

    private val _editingSeriesSeasons = MutableStateFlow<List<SeasonDraft>>(emptyList())
    val editingSeriesSeasons: StateFlow<List<SeasonDraft>> = _editingSeriesSeasons.asStateFlow()

    private val _hasSelectedProfile = MutableStateFlow(false)
    val hasSelectedProfile: StateFlow<Boolean> = _hasSelectedProfile.asStateFlow()

    private val _selectedProfileId = MutableStateFlow<String?>(null)

    private val _homeCategory = MutableStateFlow(HomeCategoryFilter.ALL)
    val homeCategory: StateFlow<HomeCategoryFilter> = _homeCategory.asStateFlow()

    private val _homeGenreFilter = MutableStateFlow<String?>(null)
    val homeGenreFilter: StateFlow<String?> = _homeGenreFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _exploreMediaFilter = MutableStateFlow("all") // all | movie | series
    val exploreMediaFilter: StateFlow<String> = _exploreMediaFilter.asStateFlow()

    private val _exploreGenreFilter = MutableStateFlow<String?>(null)
    val exploreGenreFilter: StateFlow<String?> = _exploreGenreFilter.asStateFlow()

    private val _exploreSort = MutableStateFlow(SortOption.TRENDING)
    val exploreSort: StateFlow<SortOption> = _exploreSort.asStateFlow()

    val effectiveRole: StateFlow<AppRole> = authRepository.observeEffectiveRole(currentUserId)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            if (authRepository.isBootstrappedOwner()) AppRole.OWNER else AppRole.VIEWER
        )

    val isOwner: StateFlow<Boolean> = effectiveRole
        .map { it.isOwner }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), authRepository.isBootstrappedOwner())

    val isAdmin: StateFlow<Boolean> = effectiveRole
        .map { it.isAdminOrOwner }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), authRepository.isBootstrappedAdmin())

    val accountState: StateFlow<UserAccount?> = authRepository.observeUserAccount(currentUserId)
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val isApprovedPublisher: StateFlow<Boolean> = combine(
        effectiveRole,
        accountState
    ) { role, acct ->
        RoleGovernance.isApprovedPublisherAccess(
            effectiveRole = role,
            publisherStatus = acct?.parsedPublisherStatus ?: PublisherStatus.NONE
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), authRepository.isBootstrappedOwner())

    val myPublisherApplication: StateFlow<PublisherApplication?> =
        (governanceRepository?.observeMyPublisherApplication(currentUserId) ?: flowOf(null))
            .catch { emit(null) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val allPublisherApplications: StateFlow<List<PublisherApplication>> = isAdmin
        .flatMapLatest { admin ->
            if (admin && governanceRepository != null) {
                governanceRepository.observeAllPublisherApplications().catch { emit(emptyList()) }
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val mySubmissions: StateFlow<List<ContentSubmission>> = isApprovedPublisher
        .flatMapLatest { approved ->
            if (approved && governanceRepository != null) {
                governanceRepository.observeMySubmissions(currentUserId).catch { emit(emptyList()) }
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val allSubmissions: StateFlow<List<ContentSubmission>> = isAdmin
        .flatMapLatest { admin ->
            if (admin && governanceRepository != null) {
                governanceRepository.observeAllSubmissionsForModeration().catch { emit(emptyList()) }
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val platformSettings: StateFlow<PlatformSettings> =
        (governanceRepository?.observePlatformSettings() ?: flowOf(PlatformSettings()))
            .catch { emit(PlatformSettings()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), PlatformSettings())

    @OptIn(ExperimentalCoroutinesApi::class)
    val auditLogs: StateFlow<List<AuditLogEntry>> = isAdmin
        .flatMapLatest { admin ->
            if (admin && governanceRepository != null) {
                governanceRepository.observeAuditLogs().catch { emit(emptyList()) }
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val profilesState: StateFlow<UiState<List<UserProfile>>> = profileRepository.observeProfiles(currentUserId)
        .map<List<UserProfile>, UiState<List<UserProfile>>> { UiState.Success(it) }
        .catch { error ->
            emit(UiState.Error(unwrapFirestoreMessage(error, "Failed to load profiles")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val profiles: StateFlow<List<UserProfile>> = profilesState
        .map { (it as? UiState.Success)?.data ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val activeProfile: StateFlow<UserProfile?> = combine(
        profiles,
        _selectedProfileId
    ) { profileList, selectedId ->
        profileList.firstOrNull { it.id == selectedId } ?: profileList.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val rawCatalogState: StateFlow<UiState<List<CatalogItem>>> = isAdmin
        .flatMapLatest { admin ->
            catalogRepository.observeCatalog(includeHiddenForAdmin = admin)
                .map<List<CatalogItem>, UiState<List<CatalogItem>>> { UiState.Success(it) }
                .catch { error ->
                    emit(UiState.Error(unwrapFirestoreMessage(error, "Failed to load catalog")))
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState.Loading)

    val rawCatalog: StateFlow<List<CatalogItem>> = rawCatalogState
        .map { (it as? UiState.Success)?.data ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val catalog: StateFlow<List<CatalogItem>> = combine(
        rawCatalog,
        activeProfile
    ) { items, profile ->
        items.filter { item ->
            item.isVisible && (profile?.isKids != true || item.isKidsFriendly)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val genres: StateFlow<List<Genre>> = isAdmin
        .flatMapLatest { admin ->
            catalogRepository.observeGenres(includeInactiveForAdmin = admin)
                .catch { emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val watchlist: StateFlow<List<WatchlistItem>> = activeProfile
        .flatMapLatest { profile ->
            if (profile == null || profile.id.isBlank()) flowOf(emptyList())
            else profileRepository.observeWatchlist(currentUserId, profile.id).catch { emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val progressList: StateFlow<List<PlaybackProgress>> = activeProfile
        .flatMapLatest { profile ->
            if (profile == null || profile.id.isBlank()) flowOf(emptyList())
            else profileRepository.observeProgress(currentUserId, profile.id).catch { emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val ratings: StateFlow<List<ContentRating>> = activeProfile
        .flatMapLatest { profile ->
            if (profile == null || profile.id.isBlank()) flowOf(emptyList())
            else profileRepository.observeRatings(currentUserId, profile.id).catch { emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchHistory: StateFlow<List<SearchHistoryItem>> = activeProfile
        .flatMapLatest { profile ->
            if (profile == null || profile.id.isBlank()) flowOf(emptyList())
            else profileRepository.observeSearchHistory(currentUserId, profile.id).catch { emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val notifications: StateFlow<List<UserNotification>> = authRepository.observeNotifications(currentUserId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val adminUsers: StateFlow<List<UserAccount>> = isAdmin
        .flatMapLatest { admin ->
            if (admin) catalogRepository.observeAllUsersForAdmin().catch { emit(emptyList()) }
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val userSubscription: StateFlow<UserSubscription> =
        subscriptionRepository.observeUserSubscription(currentUserId)
            .catch { emit(SubscriptionCatalogDefaults.defaultFreeSubscription(currentUserId)) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                SubscriptionCatalogDefaults.defaultFreeSubscription(currentUserId)
            )

    @OptIn(ExperimentalCoroutinesApi::class)
    val subscriptionPlans: StateFlow<List<SubscriptionPlan>> = isAdmin
        .flatMapLatest { admin ->
            subscriptionRepository.observeSubscriptionPlans(includeInactiveForAdmin = admin)
                .catch {
                    emit(
                        if (admin) SubscriptionCatalogDefaults.allBlueprintTiers
                        else listOf(SubscriptionCatalogDefaults.freePlan)
                    )
                }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            listOf(SubscriptionCatalogDefaults.freePlan)
        )

    val freePlanFeatures: StateFlow<List<PlanFeature>> =
        subscriptionRepository.observePlanFeatures("free")
            .catch { emit(SubscriptionCatalogDefaults.freePlanFeatures) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                SubscriptionCatalogDefaults.freePlanFeatures
            )

    @OptIn(ExperimentalCoroutinesApi::class)
    val adminUserSubscriptions: StateFlow<List<UserSubscription>> = isAdmin
        .flatMapLatest { admin ->
            if (admin) subscriptionRepository.observeAllUserSubscriptionsForAdmin().catch { emit(emptyList()) }
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val homepageSettings: StateFlow<HomepageSettings> = catalogRepository.observeHomepageSettings()
        .catch { emit(HomepageSettings()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomepageSettings())

    val homepageRowRules: StateFlow<List<HomepageRowRule>> = catalogRepository.observeHomepageRowRules()
        .catch { emit(HomeFeedRowBuilder.defaultRowRules()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeFeedRowBuilder.defaultRowRules())

    private val _seriesEpisodesCache = MutableStateFlow<Map<String, List<Episode>>>(emptyMap())
    val seriesEpisodesById: StateFlow<Map<String, List<Episode>>> = _seriesEpisodesCache.asStateFlow()

    val continueWatching: StateFlow<List<ContinueWatchingEntry>> = combine(
        progressList,
        catalog,
        seriesEpisodesById
    ) { progress, items, episodesMap ->
        CompletionTracker.analyzeWatchHistory(
            catalog = items,
            progressList = progress,
            episodesBySeriesId = episodesMap
        ).continueWatching
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val recommendations: StateFlow<List<CatalogItem>> = combine(
        catalog,
        activeProfile,
        progressList,
        ratings
    ) { items, profile, progress, ratingList ->
        catalogRepository.computeRecommendations(items, profile, progress, ratingList)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val searchResults: StateFlow<List<CatalogItem>> = combine(
        catalog,
        _searchQuery,
        _exploreMediaFilter,
        _exploreGenreFilter,
        _exploreSort
    ) { items, query, mediaFilter, genreFilter, sort ->
        val q = query.trim().lowercase()
        val filtered = items.filter { item ->
            val matchesType = when (mediaFilter) {
                "movie" -> item.mediaType == "movie"
                "series" -> item.mediaType == "series"
                else -> true
            }
            val matchesGenre = genreFilter == null || item.genres.any { it.equals(genreFilter, ignoreCase = true) }
            val matchesQuery = q.isBlank() ||
                item.title.lowercase().contains(q) ||
                item.synopsis.lowercase().contains(q) ||
                item.director.lowercase().contains(q) ||
                item.cast.any { it.lowercase().contains(q) } ||
                item.genres.any { it.lowercase().contains(q) }
            matchesType && matchesGenre && matchesQuery
        }
        when (sort) {
            SortOption.TRENDING -> filtered.sortedByDescending { it.trendingScore }
            SortOption.NEWEST -> filtered.sortedByDescending { it.releaseYear }
            SortOption.TITLE_AZ -> filtered.sortedBy { it.title }
            SortOption.RUNTIME -> filtered.sortedByDescending { it.runtimeMinutes }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    init {
        bootstrapSession()
        observeWatchedSeriesEpisodes()
    }

    private fun observeWatchedSeriesEpisodes() {
        viewModelScope.launch {
            combine(progressList, rawCatalog) { progress, allItems ->
                val watchedIds = progress.map { it.contentId }.toSet()
                allItems.filter { it.isSeries && it.id in watchedIds }.map { it.id }
            }.collect { watchedSeriesIds ->
                val current = _seriesEpisodesCache.value
                val missing = watchedSeriesIds.filter { it !in current }
                if (missing.isNotEmpty()) {
                    val loaded = mutableMapOf<String, List<Episode>>()
                    missing.forEach { seriesId ->
                        loaded[seriesId] = catalogRepository.loadVisibleEpisodesForSeries(seriesId)
                    }
                    _seriesEpisodesCache.value = _seriesEpisodesCache.value + loaded
                }
            }
        }
    }

    private fun bootstrapSession() {
        viewModelScope.launch {
            authRepository.ensureAccountInitialized()
            subscriptionRepository.ensureFreeSubscription()
            val savedProfileId = profileRepository.getSavedActiveProfileId(currentUserId)
            val createdOrExisting = profileRepository.ensureDefaultProfilesExist(
                userId = currentUserId,
                defaultName = "Main"
            ).getOrNull().orEmpty()
            if (savedProfileId != null && createdOrExisting.any { it.id == savedProfileId }) {
                _selectedProfileId.value = savedProfileId
            } else if (createdOrExisting.isNotEmpty()) {
                _selectedProfileId.value = createdOrExisting.first().id
            }
            if (authRepository.isBootstrappedAdmin()) {
                catalogRepository.seedDefaultCatalogIfEmpty()
                subscriptionRepository.seedSubscriptionArchitectureIfEmpty()
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun selectProfile(profile: UserProfile) {
        _selectedProfileId.value = profile.id
        _hasSelectedProfile.value = true
        viewModelScope.launch {
            profileRepository.setSavedActiveProfileId(currentUserId, profile.id)
        }
    }

    fun openProfileSwitcher() {
        _hasSelectedProfile.value = false
    }

    fun setHomeCategory(filter: HomeCategoryFilter) {
        _homeCategory.value = filter
    }

    fun setHomeGenreFilter(genre: String?) {
        _homeGenreFilter.value = if (_homeGenreFilter.value == genre) null else genre
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setExploreMediaFilter(mediaFilter: String) {
        _exploreMediaFilter.value = mediaFilter
    }

    fun setExploreGenreFilter(genre: String?) {
        _exploreGenreFilter.value = if (_exploreGenreFilter.value == genre) null else genre
    }

    fun setExploreSort(sort: SortOption) {
        _exploreSort.value = sort
    }

    fun resetExploreFilters() {
        _searchQuery.value = ""
        _exploreMediaFilter.value = "all"
        _exploreGenreFilter.value = null
        _exploreSort.value = SortOption.TRENDING
    }

    fun recordSearch(query: String) {
        val profileId = activeProfile.value?.id ?: return
        viewModelScope.launch {
            profileRepository.recordSearchQuery(profileId, query)
        }
    }

    fun deleteSearchHistory(queryId: String) {
        val profileId = activeProfile.value?.id ?: return
        viewModelScope.launch {
            profileRepository.deleteSearchHistoryItem(profileId, queryId)
        }
    }

    fun createProfile(
        name: String,
        avatarKey: String,
        isKids: Boolean,
        preferredGenres: List<String>
    ) {
        viewModelScope.launch {
            val result = profileRepository.createProfile(
                name = name,
                avatarKey = avatarKey,
                isKids = isKids,
                preferredGenres = preferredGenres
            )
            result.onSuccess { newId ->
                _selectedProfileId.value = newId
                _statusMessage.value = "Profile \"$name\" created"
            }.onFailure {
                _statusMessage.value = it.localizedMessage ?: "Failed to create profile"
            }
        }
    }

    fun updateProfile(
        profileId: String,
        name: String,
        avatarKey: String,
        isKids: Boolean,
        preferredGenres: List<String>,
        language: String,
        subtitlesEnabled: Boolean
    ) {
        viewModelScope.launch {
            profileRepository.updateProfile(
                profileId = profileId,
                name = name,
                avatarKey = avatarKey,
                isKids = isKids,
                preferredGenres = preferredGenres,
                language = language,
                subtitlesEnabled = subtitlesEnabled
            ).onSuccess {
                _statusMessage.value = "Profile updated"
            }.onFailure {
                _statusMessage.value = it.localizedMessage ?: "Failed to update profile"
            }
        }
    }

    fun deleteProfile(profileId: String) {
        viewModelScope.launch {
            if (profiles.value.size <= 1) {
                _statusMessage.value = "At least one profile is required"
                return@launch
            }
            profileRepository.deleteProfile(profileId)
                .onSuccess {
                    if (_selectedProfileId.value == profileId) {
                        _selectedProfileId.value = profiles.value.firstOrNull { it.id != profileId }?.id
                    }
                    _statusMessage.value = "Profile removed"
                }
                .onFailure {
                    _statusMessage.value = it.localizedMessage ?: "Could not delete profile"
                }
        }
    }

    fun toggleWatchlist(item: CatalogItem) {
        val profileId = activeProfile.value?.id ?: return
        val currentlyInList = watchlist.value.any { it.contentId == item.id }
        viewModelScope.launch {
            profileRepository.toggleWatchlist(profileId, item, currentlyInList)
                .onSuccess { added ->
                    _statusMessage.value = if (added) {
                        "Added \"${item.title}\" to My List"
                    } else {
                        "Removed \"${item.title}\" from My List"
                    }
                }
                .onFailure {
                    _statusMessage.value = it.localizedMessage ?: "Failed to update My List"
                }
        }
    }

    fun savePlaybackProgress(
        contentId: String,
        episodeId: String?,
        seasonNumber: Int?,
        episodeNumber: Int?,
        title: String,
        positionMs: Long,
        durationMs: Long
    ) {
        val profileId = activeProfile.value?.id ?: return
        viewModelScope.launch {
            profileRepository.savePlaybackProgress(
                profileId = profileId,
                contentId = contentId,
                episodeId = episodeId,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber,
                title = title,
                positionMs = positionMs,
                durationMs = durationMs
            )
        }
    }

    fun deleteHistoryEntry(progressId: String) {
        val profileId = activeProfile.value?.id ?: return
        viewModelScope.launch {
            profileRepository.deleteProgressItem(profileId, progressId)
                .onSuccess { _statusMessage.value = "Removed from watch history" }
        }
    }

    fun clearWatchHistory() {
        val profileId = activeProfile.value?.id ?: return
        val ids = progressList.value.map { it.id }.filter { it.isNotBlank() }
        if (ids.isEmpty()) return
        viewModelScope.launch {
            profileRepository.clearAllProgress(profileId, ids)
                .onSuccess { _statusMessage.value = "Watch history cleared" }
        }
    }

    fun setLikeDislike(contentId: String, liked: Int) {
        val profileId = activeProfile.value?.id ?: return
        val existingStars = ratings.value.firstOrNull { it.contentId == contentId }?.stars ?: 0
        viewModelScope.launch {
            profileRepository.saveRating(profileId, contentId, liked, existingStars)
        }
    }

    fun setStarRating(contentId: String, stars: Int) {
        val profileId = activeProfile.value?.id ?: return
        val existingLiked = ratings.value.firstOrNull { it.contentId == contentId }?.liked ?: 0
        val autoLike = if (stars >= 4) 1 else if (stars in 1..2) -1 else existingLiked
        viewModelScope.launch {
            profileRepository.saveRating(profileId, contentId, autoLike, stars)
                .onSuccess { _statusMessage.value = "Rated $stars/5 stars" }
        }
    }

    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch {
            authRepository.markNotificationRead(currentUserId, notificationId)
        }
    }

    fun updatePreferences(
        displayName: String,
        languagePreference: String,
        autoplayNext: Boolean,
        defaultQuality: String,
        subtitlesEnabled: Boolean,
        subtitleSize: String
    ) {
        viewModelScope.launch {
            authRepository.updatePreferences(
                userId = currentUserId,
                displayName = displayName,
                languagePreference = languagePreference,
                autoplayNext = autoplayNext,
                defaultQuality = defaultQuality,
                subtitlesEnabled = subtitlesEnabled,
                subtitleSize = subtitleSize
            ).onSuccess {
                _statusMessage.value = "Playback & account settings saved"
            }.onFailure {
                _statusMessage.value = it.localizedMessage ?: "Failed to save settings"
            }
        }
    }

    fun observeSeasons(contentId: String): Flow<List<Season>> =
        catalogRepository.observeSeasons(contentId, includeHiddenForAdmin = isAdmin.value)
            .catch { emit(emptyList()) }

    fun observeEpisodes(contentId: String, seasonId: String, seasonNumber: Int): Flow<List<Episode>> =
        catalogRepository.observeEpisodes(contentId, seasonId, seasonNumber, includeHiddenForAdmin = isAdmin.value)
            .catch { emit(emptyList()) }

    fun getSimilarTitles(item: CatalogItem): List<CatalogItem> =
        catalogRepository.computeSimilarTitles(
            target = item,
            catalog = rawCatalog.value,
            isKids = activeProfile.value?.isKids == true
        )

    // --- Admin Actions ---

    private fun requireAdminAction(): Boolean {
        if (!isAdmin.value) {
            _statusMessage.value = "Only administrators can modify catalog content."
            return false
        }
        return true
    }

    fun seedCatalogNow() {
        if (!requireAdminAction()) return
        viewModelScope.launch {
            catalogRepository.seedDefaultCatalogIfEmpty()
                .onSuccess { seeded ->
                    _statusMessage.value = if (seeded) {
                        "Default streaming catalog seeded to Firestore"
                    } else {
                        "Catalog already exists in Firestore"
                    }
                }
                .onFailure {
                    _statusMessage.value = it.localizedMessage ?: "Admin seed failed"
                }
        }
    }

    fun prepareSeriesEditor(contentId: String?) {
        if (contentId.isNullOrBlank()) {
            _editingSeriesSeasons.value = listOf(
                SeasonDraft(
                    season = Season(id = "season-1", seasonNumber = 1, title = "Season 1", isVisible = true),
                    episodes = emptyList()
                )
            )
            _isLoadingSeriesStructure.value = false
            return
        }
        viewModelScope.launch {
            _isLoadingSeriesStructure.value = true
            catalogRepository.loadSeriesStructure(contentId)
                .onSuccess { loaded ->
                    _editingSeriesSeasons.value = loaded.ifEmpty {
                        listOf(
                            SeasonDraft(
                                season = Season(id = "season-1", seasonNumber = 1, title = "Season 1", isVisible = true),
                                episodes = emptyList()
                            )
                        )
                    }
                }
                .onFailure {
                    _statusMessage.value = it.localizedMessage ?: "Failed to load seasons & episodes"
                }
            _isLoadingSeriesStructure.value = false
        }
    }

    fun saveCatalogItem(item: CatalogItem, onComplete: () -> Unit = {}) {
        if (!requireAdminAction()) return
        viewModelScope.launch {
            _isSavingCatalog.value = true
            catalogRepository.saveCatalogItem(item)
                .onSuccess {
                    _statusMessage.value = "Saved \"${item.title}\" to Firestore"
                    onComplete()
                }
                .onFailure {
                    _statusMessage.value = it.localizedMessage ?: "Failed to save title"
                }
            _isSavingCatalog.value = false
        }
    }

    fun saveSeriesWithSeasons(
        series: CatalogItem,
        seasonDrafts: List<SeasonDraft>,
        removedSeasonIds: Set<String> = emptySet(),
        removedEpisodeRefs: List<Pair<String, String>> = emptyList(),
        onComplete: () -> Unit = {}
    ) {
        if (!requireAdminAction()) return
        viewModelScope.launch {
            _isSavingCatalog.value = true
            catalogRepository.saveSeriesWithSeasons(
                series = series,
                seasonDrafts = seasonDrafts,
                removedSeasonIds = removedSeasonIds,
                removedEpisodeRefs = removedEpisodeRefs
            ).onSuccess {
                val epCount = seasonDrafts.sumOf { it.episodes.size }
                _statusMessage.value = "Saved \"${series.title}\" (${seasonDrafts.size} season(s), $epCount episode(s)) to Firestore"
                onComplete()
            }.onFailure {
                _statusMessage.value = it.localizedMessage ?: "Failed to save TV show"
            }
            _isSavingCatalog.value = false
        }
    }

    fun deleteCatalogItem(contentId: String, title: String) {
        if (!requireAdminAction()) return
        viewModelScope.launch {
            catalogRepository.deleteCatalogItem(contentId)
                .onSuccess {
                    _statusMessage.value = "Deleted \"$title\""
                }
                .onFailure {
                    _statusMessage.value = it.localizedMessage ?: "Failed to delete title"
                }
        }
    }

    fun addSeason(contentId: String, season: Season) {
        if (!requireAdminAction()) return
        viewModelScope.launch {
            catalogRepository.addSeason(contentId, season)
                .onSuccess { _statusMessage.value = "Added Season ${season.seasonNumber}" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to add season" }
        }
    }

    fun addEpisode(contentId: String, seasonId: String, episode: Episode) {
        if (!requireAdminAction()) return
        viewModelScope.launch {
            catalogRepository.addEpisode(contentId, seasonId, episode)
                .onSuccess { _statusMessage.value = "Added Episode ${episode.episodeNumber}" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to add episode" }
        }
    }

    fun saveGenre(genre: Genre) {
        if (!requireAdminAction()) return
        viewModelScope.launch {
            catalogRepository.saveGenre(genre)
                .onSuccess { _statusMessage.value = "Saved genre \"${genre.name}\"" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to save genre" }
        }
    }

    fun deleteGenre(genreId: String) {
        if (!requireAdminAction()) return
        viewModelScope.launch {
            catalogRepository.deleteGenre(genreId)
                .onSuccess { _statusMessage.value = "Genre removed" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to delete genre" }
        }
    }

    fun saveHomepageSettings(settings: HomepageSettings) {
        if (!requireAdminAction()) return
        viewModelScope.launch {
            catalogRepository.saveHomepageSettings(settings)
                .onSuccess { _statusMessage.value = "Homepage global settings saved" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to save homepage settings" }
        }
    }

    fun saveHomepageRowRules(rules: List<HomepageRowRule>) {
        if (!requireAdminAction()) return
        viewModelScope.launch {
            catalogRepository.saveHomepageRowRules(rules)
                .onSuccess { _statusMessage.value = "Homepage row layout saved" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to save row layout" }
        }
    }

    fun resetHomepageLayout() {
        if (!requireAdminAction()) return
        viewModelScope.launch {
            catalogRepository.resetHomepageLayout()
                .onSuccess { _statusMessage.value = "Homepage layout reset to defaults" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to reset layout" }
        }
    }

    fun setUserAdminRole(target: UserAccount, makeAdmin: Boolean) {
        val desired = if (makeAdmin) AppRole.ADMIN else AppRole.VIEWER
        assignUserRole(target, desired)
    }

    fun assignUserRole(
        target: UserAccount,
        desiredRole: AppRole,
        desiredPublisherStatus: PublisherStatus? = null
    ) {
        if (!requireAdminAction()) return
        val gov = governanceRepository
        if (gov == null) {
            _statusMessage.value = "Governance service unavailable."
            return
        }
        viewModelScope.launch {
            gov.assignUserRole(
                target = target,
                desiredRole = desiredRole,
                desiredPublisherStatus = desiredPublisherStatus,
                actorRole = effectiveRole.value
            ).onSuccess {
                _statusMessage.value = "Updated ${target.displayName.ifBlank { target.email }} to ${desiredRole.label}"
            }.onFailure {
                _statusMessage.value = it.localizedMessage ?: "Failed to update user role"
            }
        }
    }

    fun updatePublisherStatus(target: UserAccount, newStatus: PublisherStatus) {
        if (!requireAdminAction()) return
        val gov = governanceRepository ?: return
        viewModelScope.launch {
            gov.updatePublisherStatus(target, newStatus, effectiveRole.value)
                .onSuccess {
                    _statusMessage.value = "Publisher status set to ${newStatus.label} for ${target.displayName.ifBlank { target.email }}"
                }
                .onFailure {
                    _statusMessage.value = it.localizedMessage ?: "Failed to update publisher status"
                }
        }
    }

    fun setUserSuspended(target: UserAccount, suspended: Boolean) {
        if (!requireAdminAction()) return
        val gov = governanceRepository
        viewModelScope.launch {
            val result = if (gov != null) {
                gov.setUserSuspendedWithAudit(target, suspended, effectiveRole.value)
            } else {
                catalogRepository.setUserSuspended(target, suspended)
            }
            result.onSuccess {
                _statusMessage.value = if (suspended) {
                    "Suspended account ${target.displayName}"
                } else {
                    "Restored account ${target.displayName}"
                }
            }.onFailure {
                _statusMessage.value = it.localizedMessage ?: "Failed to update suspension status"
            }
        }
    }

    // --- Publisher Application & Content Submission Actions ---

    fun submitPublisherApplicationTracker(
        publisherName: String,
        contactEmail: String,
        organizationDescription: String,
        intendedContentDetails: String,
        onComplete: () -> Unit = {}
    ) {
        val gov = governanceRepository ?: return
        viewModelScope.launch {
            gov.submitPublisherApplicationTracker(
                publisherName = publisherName,
                contactEmail = contactEmail,
                organizationDescription = organizationDescription,
                intendedContentDetails = intendedContentDetails
            ).onSuccess {
                _statusMessage.value = "Application status recorded as Pending Review. Notflix will contact you by email with its decision."
                onComplete()
            }.onFailure {
                _statusMessage.value = it.localizedMessage ?: "Could not record application status"
            }
        }
    }

    fun reviewPublisherApplication(
        application: PublisherApplication,
        newStatus: PublisherStatus,
        reviewerNotes: String
    ) {
        if (!requireAdminAction()) return
        val gov = governanceRepository ?: return
        viewModelScope.launch {
            gov.reviewPublisherApplication(
                application = application,
                newStatus = newStatus,
                reviewerNotes = reviewerNotes,
                actorRole = effectiveRole.value
            ).onSuccess {
                _statusMessage.value = "Application marked ${newStatus.label}"
            }.onFailure {
                _statusMessage.value = it.localizedMessage ?: "Failed to update publisher application"
            }
        }
    }

    fun submitPublisherContent(submission: ContentSubmission, onComplete: () -> Unit = {}) {
        if (!isApprovedPublisher.value) {
            _statusMessage.value = "Only approved Publishers/Distributors can submit content."
            return
        }
        val gov = governanceRepository ?: return
        viewModelScope.launch {
            _isSavingCatalog.value = true
            val result = if (submission.id.isBlank()) {
                gov.submitNewContent(submission).map { Unit }
            } else {
                gov.updateOwnSubmission(submission)
            }
            result.onSuccess {
                _statusMessage.value = "Submitted \"${submission.title}\" for content review."
                onComplete()
            }.onFailure {
                _statusMessage.value = it.localizedMessage ?: "Submission failed"
            }
            _isSavingCatalog.value = false
        }
    }

    fun submitApprovedContentRevision(
        original: ContentSubmission,
        revised: ContentSubmission,
        onComplete: () -> Unit = {}
    ) {
        if (!isApprovedPublisher.value) {
            _statusMessage.value = "Only approved Publishers/Distributors can submit revisions."
            return
        }
        val gov = governanceRepository ?: return
        viewModelScope.launch {
            _isSavingCatalog.value = true
            gov.submitRevisionForApprovedContent(original, revised)
                .onSuccess {
                    _statusMessage.value = "Revision submitted for review. The live public title remains unchanged until approved."
                    onComplete()
                }
                .onFailure {
                    _statusMessage.value = it.localizedMessage ?: "Failed to submit revision"
                }
            _isSavingCatalog.value = false
        }
    }

    fun observeReviewHistory(submissionId: String): Flow<List<ContentReviewDecision>> =
        governanceRepository?.observeReviewHistory(submissionId) ?: flowOf(emptyList())

    fun reviewContentSubmission(
        submission: ContentSubmission,
        decision: ContentReviewStatus,
        privateNotes: String,
        videoSourceVerified: Boolean,
        rightsConfirmed: Boolean,
        metadataVerified: Boolean,
        onComplete: () -> Unit = {}
    ) {
        if (!requireAdminAction()) return
        val gov = governanceRepository ?: return
        viewModelScope.launch {
            _isSavingCatalog.value = true
            gov.reviewContentSubmission(
                submission = submission,
                decision = decision,
                privateNotes = privateNotes,
                videoSourceVerified = videoSourceVerified,
                rightsConfirmed = rightsConfirmed,
                metadataVerified = metadataVerified,
                actorRole = effectiveRole.value
            ).onSuccess {
                _statusMessage.value = when (decision) {
                    ContentReviewStatus.APPROVED -> "Approved & published \"${submission.title}\" to Notflix."
                    ContentReviewStatus.CHANGES_REQUESTED -> "Requested changes for \"${submission.title}\"."
                    ContentReviewStatus.REJECTED -> "Rejected \"${submission.title}\"."
                    else -> "Marked \"${submission.title}\" as ${decision.label}."
                }
                onComplete()
            }.onFailure {
                _statusMessage.value = it.localizedMessage ?: "Failed to submit review decision"
            }
            _isSavingCatalog.value = false
        }
    }

    // --- Subscription Plan & Free Access Management ---

    fun saveSubscriptionPlan(plan: SubscriptionPlan, onComplete: () -> Unit = {}) {
        if (!requireAdminAction()) return
        viewModelScope.launch {
            subscriptionRepository.saveSubscriptionPlan(plan)
                .onSuccess { planId ->
                    governanceRepository?.writeAuditLog(
                        actorUid = currentUserId,
                        actorRole = effectiveRole.value,
                        action = if (plan.id.isBlank()) "CREATE_SUBSCRIPTION_PLAN" else "UPDATE_SUBSCRIPTION_PLAN",
                        targetSubscriptionId = planId,
                        previousValue = plan.id.ifBlank { "new" },
                        newValue = "${plan.name} (${plan.formattedPriceLabel()}, active=${plan.isActive}, public=${plan.isPublic})",
                        details = "Saved subscription plan $planId"
                    )
                    _statusMessage.value = "Saved plan \"${plan.name}\""
                    onComplete()
                }
                .onFailure {
                    _statusMessage.value = it.localizedMessage ?: "Failed to save subscription plan"
                }
        }
    }

    fun archiveSubscriptionPlan(plan: SubscriptionPlan, archived: Boolean) {
        if (!requireAdminAction()) return
        viewModelScope.launch {
            subscriptionRepository.archiveSubscriptionPlan(plan, archived)
                .onSuccess {
                    governanceRepository?.writeAuditLog(
                        actorUid = currentUserId,
                        actorRole = effectiveRole.value,
                        action = if (archived) "ARCHIVE_SUBSCRIPTION_PLAN" else "UNARCHIVE_SUBSCRIPTION_PLAN",
                        targetSubscriptionId = plan.id,
                        previousValue = "archived=${plan.isArchived}",
                        newValue = "archived=$archived",
                        details = "${if (archived) "Archived" else "Restored"} plan ${plan.name}"
                    )
                    _statusMessage.value = if (archived) "Archived \"${plan.name}\" (existing subscribers preserved)" else "Restored \"${plan.name}\""
                }
                .onFailure {
                    _statusMessage.value = it.localizedMessage ?: "Failed to archive plan"
                }
        }
    }

    fun assignUserSubscription(targetUserId: String, plan: SubscriptionPlan, status: String = "active") {
        if (!requireAdminAction()) return
        viewModelScope.launch {
            subscriptionRepository.assignUserSubscription(targetUserId, plan, status)
                .onSuccess {
                    governanceRepository?.writeAuditLog(
                        actorUid = currentUserId,
                        actorRole = effectiveRole.value,
                        action = "ASSIGN_USER_SUBSCRIPTION",
                        targetUid = targetUserId,
                        targetSubscriptionId = plan.id.ifBlank { plan.tierCode },
                        previousValue = "manual_update",
                        newValue = "${plan.name} ($status)",
                        details = "Admin assigned ${plan.name} ($status) to user $targetUserId"
                    )
                    _statusMessage.value = "Assigned ${plan.name} ($status) to user"
                }
                .onFailure {
                    _statusMessage.value = it.localizedMessage ?: "Failed to assign subscription"
                }
        }
    }

    fun updateUserSubscriptionStatus(targetUserId: String, newStatus: String) {
        if (!requireAdminAction()) return
        viewModelScope.launch {
            subscriptionRepository.updateUserSubscriptionStatus(targetUserId, newStatus)
                .onSuccess {
                    governanceRepository?.writeAuditLog(
                        actorUid = currentUserId,
                        actorRole = effectiveRole.value,
                        action = "UPDATE_USER_SUBSCRIPTION_STATUS",
                        targetUid = targetUserId,
                        previousValue = "status_change",
                        newValue = newStatus,
                        details = "Updated subscription status to $newStatus"
                    )
                    _statusMessage.value = "Subscription status updated to $newStatus"
                }
                .onFailure {
                    _statusMessage.value = it.localizedMessage ?: "Failed to update subscription status"
                }
        }
    }

    fun selectPublicPlanForSelf(plan: SubscriptionPlan) {
        viewModelScope.launch {
            subscriptionRepository.selectPublicPlanForSelf(plan)
                .onSuccess {
                    _statusMessage.value = "Switched to ${plan.name} plan"
                }
                .onFailure {
                    _statusMessage.value = it.localizedMessage ?: "Could not switch plan"
                }
        }
    }

    fun setFreeAccessEnabled(enabled: Boolean) {
        if (!requireAdminAction()) return
        val gov = governanceRepository ?: return
        viewModelScope.launch {
            gov.setFreeAccessEnabled(enabled, effectiveRole.value)
                .onSuccess {
                    _statusMessage.value = if (enabled) {
                        "Platform-wide Free Access enabled (existing subscriptions preserved)"
                    } else {
                        "Platform-wide Free Access disabled (plan-based access active)"
                    }
                }
                .onFailure {
                    _statusMessage.value = it.localizedMessage ?: "Failed to update Free Access mode"
                }
        }
    }

    fun observeUserProfilesForAdmin(userId: String): Flow<List<UserProfile>> =
        profileRepository.observeProfiles(userId)
            .catch { emit(emptyList()) }

    fun adminCreateProfile(
        targetUserId: String,
        name: String,
        avatarKey: String,
        isKids: Boolean,
        preferredGenres: List<String>
    ) {
        if (!requireAdminAction()) return
        viewModelScope.launch {
            profileRepository.createProfileForUser(
                targetUserId = targetUserId,
                name = name,
                avatarKey = avatarKey,
                isKids = isKids,
                preferredGenres = preferredGenres
            ).onSuccess {
                _statusMessage.value = "Created profile \"$name\""
            }.onFailure {
                _statusMessage.value = it.localizedMessage ?: "Failed to create profile"
            }
        }
    }

    fun adminUpdateProfile(
        targetUserId: String,
        profileId: String,
        name: String,
        avatarKey: String,
        isKids: Boolean,
        preferredGenres: List<String>,
        language: String,
        subtitlesEnabled: Boolean
    ) {
        if (!requireAdminAction()) return
        viewModelScope.launch {
            profileRepository.updateProfileForUser(
                targetUserId = targetUserId,
                profileId = profileId,
                name = name,
                avatarKey = avatarKey,
                isKids = isKids,
                preferredGenres = preferredGenres,
                language = language,
                subtitlesEnabled = subtitlesEnabled
            ).onSuccess {
                _statusMessage.value = "Updated profile \"$name\""
            }.onFailure {
                _statusMessage.value = it.localizedMessage ?: "Failed to update profile"
            }
        }
    }

    fun adminDeleteProfile(targetUserId: String, profileId: String) {
        if (!requireAdminAction()) return
        viewModelScope.launch {
            profileRepository.deleteProfileForUser(targetUserId, profileId)
                .onSuccess {
                    _statusMessage.value = "Removed user profile"
                }
                .onFailure {
                    _statusMessage.value = it.localizedMessage ?: "Failed to delete user profile"
                }
        }
    }

    fun uploadMediaFile(
        fieldKey: String,
        uri: Uri,
        category: MediaAssetCategory,
        contentFolderHint: String,
        onUploaded: (downloadUrl: String, mimeType: String?, fileName: String) -> Unit
    ) {
        if (!requireAdminAction()) return
        val uploader = mediaUploadRepository
        if (uploader == null) {
            _statusMessage.value = "Media upload service unavailable."
            return
        }
        viewModelScope.launch {
            uploader.uploadMedia(uri, category, contentFolderHint).collect { state ->
                _mediaUploadStates.value = _mediaUploadStates.value + (fieldKey to state)
                when (state) {
                    is MediaUploadState.Success -> {
                        onUploaded(state.downloadUrl, state.mimeType, state.fileName)
                        _statusMessage.value = "${category.label} uploaded (${state.fileName})"
                    }
                    is MediaUploadState.Error -> {
                        _statusMessage.value = state.message
                    }
                    else -> Unit
                }
            }
        }
    }

    fun clearMediaUploadState(fieldKey: String? = null) {
        if (fieldKey == null) {
            _mediaUploadStates.value = emptyMap()
        } else {
            _mediaUploadStates.value = _mediaUploadStates.value - fieldKey
        }
    }

    fun syncSubscriptionSchemasNow() {
        if (!requireAdminAction()) return
        viewModelScope.launch {
            subscriptionRepository.seedSubscriptionArchitectureIfEmpty()
                .onSuccess { seeded ->
                    _statusMessage.value = if (seeded) {
                        "Subscription plan architecture synced to Firestore"
                    } else {
                        "Subscription plan architecture already synced"
                    }
                }
                .onFailure {
                    _statusMessage.value = it.localizedMessage ?: "Failed to sync subscription plans"
                }
        }
    }

    private fun unwrapFirestoreMessage(error: Throwable, fallback: String): String {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        return (firestoreError ?: error).localizedMessage ?: fallback
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5000L
    }
}
