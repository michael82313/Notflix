package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface VeloraRoute {
    @Serializable
    data object Home : VeloraRoute

    @Serializable
    data object Explore : VeloraRoute

    @Serializable
    data object MyList : VeloraRoute

    @Serializable
    data object Settings : VeloraRoute

    @Serializable
    data object Admin : VeloraRoute

    @Serializable
    data class Details(val contentId: String) : VeloraRoute

    @Serializable
    data class Player(
        val contentId: String,
        val episodeId: String? = null,
        val seasonNumber: Int? = null,
        val episodeNumber: Int? = null,
        val isTrailer: Boolean = false
    ) : VeloraRoute
}
