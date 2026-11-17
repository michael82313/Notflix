package com.example.data.seed

import com.example.data.model.CatalogItem
import com.example.data.model.Episode
import com.example.data.model.Genre
import com.example.data.model.Season

object SeedCatalog {
    val defaultGenres: List<Genre> = listOf(
        Genre(id = "sci-fi", name = "Sci-Fi", slug = "sci-fi", displayOrder = 1, isActive = true),
        Genre(id = "action", name = "Action", slug = "action", displayOrder = 2, isActive = true),
        Genre(id = "thriller", name = "Thriller", slug = "thriller", displayOrder = 3, isActive = true),
        Genre(id = "fantasy", name = "Fantasy", slug = "fantasy", displayOrder = 4, isActive = true),
        Genre(id = "drama", name = "Drama", slug = "drama", displayOrder = 5, isActive = true),
        Genre(id = "animation", name = "Animation", slug = "animation", displayOrder = 6, isActive = true),
        Genre(id = "adventure", name = "Adventure", slug = "adventure", displayOrder = 7, isActive = true),
        Genre(id = "documentary", name = "Documentary", slug = "documentary", displayOrder = 8, isActive = true),
        Genre(id = "horror", name = "Horror", slug = "horror", displayOrder = 9, isActive = true),
        Genre(id = "mystery", name = "Mystery", slug = "mystery", displayOrder = 10, isActive = true),
        Genre(id = "comedy", name = "Comedy", slug = "comedy", displayOrder = 11, isActive = true),
        Genre(id = "anime", name = "Anime", slug = "anime", displayOrder = 12, isActive = true),
        Genre(id = "romance", name = "Romance", slug = "romance", displayOrder = 13, isActive = true),
    )

    val defaultItems: List<CatalogItem> = listOf(
        CatalogItem(
            id = "tears-of-steel",
            title = "Tears of Steel",
            synopsis = "In a neon-soaked future Amsterdam, a small band of resistance fighters and cybernetic specialists race to reprogram a rogue orbital defense satellite before it reshapes humanity.",
            mediaType = "movie",
            genres = listOf("Sci-Fi", "Action"),
            releaseYear = 2025,
            runtimeMinutes = 12,
            ageRating = "PG-13",
            director = "Ian Hubert",
            cast = listOf("Derek de Lint", "Sergio Hasselbaink", "Vanja Rukavina", "Denise Rebergen"),
            posterKey = "img_hero_nebula",
            backdropKey = "img_hero_nebula",
            trailerUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            hlsUrl = null,
            isFeatured = true,
            isVisible = true,
            isKidsFriendly = false,
            trendingScore = 980,
            popularityRank = 1,
            tags = listOf("Sci-Fi", "Cyberpunk", "Superhero"),
            keywords = listOf("futuristic", "amsterdam", "ai", "orbital"),
            isStaffPick = true,
            isAwardWinning = true
        ),
        CatalogItem(
            id = "sintel-chronicles",
            title = "Sintel",
            synopsis = "A lone wanderer crosses frozen mountain passes and ancient citadels in relentless pursuit of the young dragon she once rescued and named Scales.",
            mediaType = "movie",
            genres = listOf("Fantasy", "Adventure"),
            releaseYear = 2024,
            runtimeMinutes = 15,
            ageRating = "PG",
            director = "Colin Levy",
            cast = listOf("Halina Reijn", "Thom Hoffman"),
            posterKey = "img_poster_odyssey",
            backdropKey = "img_poster_odyssey",
            trailerUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
            hlsUrl = null,
            isFeatured = true,
            isVisible = true,
            isKidsFriendly = true,
            trendingScore = 940,
            popularityRank = 2,
            tags = listOf("Feel-Good", "Short Film", "Award-Winning"),
            keywords = listOf("dragon", "journey", "quest"),
            isAwardWinning = true
        ),
        CatalogItem(
            id = "midnight-bridge",
            title = "Midnight Bridge",
            synopsis = "When an encrypted transit ledger vanishes in the rain-drenched lower ward, an investigative archivist uncovers a citywide conspiracy spanning three decades.",
            mediaType = "series",
            genres = listOf("Thriller", "Drama", "Mystery"),
            releaseYear = 2026,
            runtimeMinutes = 46,
            ageRating = "TV-14",
            director = "Elena Vance",
            cast = listOf("Marcus Sterling", "Nadia Kurosawa", "Devon Brooks"),
            posterKey = "img_poster_chronicle",
            backdropKey = "img_poster_chronicle",
            trailerUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
            hlsUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            isFeatured = true,
            isVisible = true,
            isKidsFriendly = false,
            trendingScore = 965,
            popularityRank = 1,
            tags = listOf("Crime", "Binge-Worthy", "Suspense"),
            keywords = listOf("conspiracy", "detective", "neo-noir"),
            isStaffPick = true
        ),
        CatalogItem(
            id = "coastal-horizon",
            title = "Pacific Horizon",
            synopsis = "Two estranged siblings reunite for one final coastal rally from Big Sur to Astoria, confronting family secrets along every cliffside hairpin.",
            mediaType = "series",
            genres = listOf("Drama", "Adventure"),
            releaseYear = 2025,
            runtimeMinutes = 42,
            ageRating = "TV-PG",
            director = "Julian Mercer",
            cast = listOf("Clara Bennett", "Liam O'Connor", "Sora Takahashi"),
            posterKey = "img_poster_horizon",
            backdropKey = "img_poster_horizon",
            trailerUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4",
            hlsUrl = null,
            isFeatured = false,
            isVisible = true,
            isKidsFriendly = true,
            trendingScore = 910,
            popularityRank = 2
        ),
        CatalogItem(
            id = "big-buck-bunny",
            title = "Big Buck Bunny",
            synopsis = "A gentle giant rabbit living in a sunlit meadow springs into ingenious slapstick action after three mischievous woodland critters disrupt his peaceful morning.",
            mediaType = "movie",
            genres = listOf("Animation", "Adventure"),
            releaseYear = 2024,
            runtimeMinutes = 10,
            ageRating = "G",
            director = "Sacha Goedegebure",
            cast = listOf("Notflix Animation Ensemble"),
            posterKey = "img_poster_odyssey",
            backdropKey = "img_poster_odyssey",
            trailerUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            hlsUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            isFeatured = false,
            isVisible = true,
            isKidsFriendly = true,
            trendingScore = 920,
            popularityRank = 3
        ),
        CatalogItem(
            id = "elephants-dream",
            title = "Machina Obscura",
            synopsis = "Inside an infinite clockwork metropolis, two companions debate whether the machine around them is a sanctuary or an illusion waiting to be dismantled.",
            mediaType = "movie",
            genres = listOf("Sci-Fi", "Thriller"),
            releaseYear = 2023,
            runtimeMinutes = 11,
            ageRating = "PG-13",
            director = "Bassam Kurdali",
            cast = listOf("Tygo Gernandt", "Cas Jansen"),
            posterKey = "img_poster_chronicle",
            backdropKey = "img_hero_nebula",
            trailerUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
            hlsUrl = null,
            isFeatured = false,
            isVisible = true,
            isKidsFriendly = false,
            trendingScore = 870,
            popularityRank = 4
        ),
        CatalogItem(
            id = "orbital-zero",
            title = "Orbital Zero",
            synopsis = "A deep-space relay crew stationed above a scarlet nebula intercepts an unscheduled transmission from an expedition presumed lost twenty years earlier.",
            mediaType = "series",
            genres = listOf("Sci-Fi", "Thriller"),
            releaseYear = 2026,
            runtimeMinutes = 52,
            ageRating = "TV-14",
            director = "Aria Chen",
            cast = listOf("Kaelen Vance", "Priya Nair", "Jonas Lindqvist"),
            posterKey = "img_hero_nebula",
            backdropKey = "img_hero_nebula",
            trailerUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            hlsUrl = "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8",
            isFeatured = false,
            isVisible = true,
            isKidsFriendly = false,
            trendingScore = 955,
            popularityRank = 3,
            tags = listOf("Binge-Worthy", "Space", "Mystery"),
            keywords = listOf("nebula", "transmission", "station")
        ),
        CatalogItem(
            id = "aurora-crown",
            title = "The Aurora Crown",
            synopsis = "Young cartographers unlock a celestial astrolabe that charts hidden valleys beneath the northern lights in this sweeping multi-part fantasy saga.",
            mediaType = "series",
            genres = listOf("Fantasy", "Adventure"),
            releaseYear = 2025,
            runtimeMinutes = 38,
            ageRating = "TV-PG",
            director = "Rowan Thorne",
            cast = listOf("Freya Lind", "Mateo Silva", "Amina Diallo"),
            posterKey = "img_poster_odyssey",
            backdropKey = "img_poster_odyssey",
            trailerUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
            hlsUrl = null,
            isFeatured = false,
            isVisible = true,
            isKidsFriendly = true,
            trendingScore = 895,
            popularityRank = 4
        ),
        CatalogItem(
            id = "redline-run",
            title = "Redline Run",
            synopsis = "An inside look at endurance drivers pushing electric and classic prototypes through alpine passes and desert salt flats against impossible weather.",
            mediaType = "movie",
            genres = listOf("Documentary", "Action"),
            releaseYear = 2025,
            runtimeMinutes = 78,
            ageRating = "PG",
            director = "Lucas Moreau",
            cast = listOf("Hanna Berg", "Diego Alvarez"),
            posterKey = "img_poster_horizon",
            backdropKey = "img_poster_horizon",
            trailerUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
            hlsUrl = null,
            isFeatured = false,
            isVisible = true,
            isKidsFriendly = true,
            trendingScore = 840,
            popularityRank = 5
        ),
        CatalogItem(
            id = "crimson-signal",
            title = "Crimson Signal",
            synopsis = "When emergency sirens fall silent across the harbor district, a sound engineer isolates a sub-audible frequency hidden inside live radio broadcasts.",
            mediaType = "movie",
            genres = listOf("Thriller", "Drama"),
            releaseYear = 2026,
            runtimeMinutes = 104,
            ageRating = "PG-13",
            director = "Nora Halvorsen",
            cast = listOf("Maya Lin", "Victor Shaw", "Elena Novak"),
            posterKey = "img_poster_chronicle",
            backdropKey = "img_poster_chronicle",
            trailerUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            hlsUrl = null,
            isFeatured = false,
            isVisible = true,
            isKidsFriendly = false,
            trendingScore = 885,
            popularityRank = 6
        ),
        CatalogItem(
            id = "meadow-tales",
            title = "Meadow Tales",
            synopsis = "Short animated fables celebrating curiosity, friendship, and tiny backyard discoveries for viewers of every age.",
            mediaType = "series",
            genres = listOf("Animation", "Adventure"),
            releaseYear = 2025,
            runtimeMinutes = 22,
            ageRating = "TV-G",
            director = "Samira Patel",
            cast = listOf("Pip", "Bramble", "Clover"),
            posterKey = "img_poster_odyssey",
            backdropKey = "img_poster_odyssey",
            trailerUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            hlsUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            isFeatured = false,
            isVisible = true,
            isKidsFriendly = true,
            trendingScore = 860,
            popularityRank = 5
        ),
        CatalogItem(
            id = "velocity-atlas",
            title = "Velocity Atlas",
            synopsis = "A kinetic documentary anthology exploring iconic mountain roads, test tracks, and the craftspeople keeping classic machines alive.",
            mediaType = "series",
            genres = listOf("Documentary", "Drama"),
            releaseYear = 2024,
            runtimeMinutes = 35,
            ageRating = "TV-PG",
            director = "Owen Gallagher",
            cast = listOf("Theo Grant", "Camille Laurent"),
            posterKey = "img_poster_horizon",
            backdropKey = "img_poster_horizon",
            trailerUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WhatCarCanYouGetForAGrand.mp4",
            hlsUrl = null,
            isFeatured = false,
            isVisible = true,
            isKidsFriendly = true,
            trendingScore = 815,
            popularityRank = 6
        ),
        CatalogItem(
            id = "eclipse-protocol",
            title = "Eclipse Protocol",
            synopsis = "During a total solar eclipse, an international observatory team has four minutes to recalibrate a gravitational lens array before the window closes.",
            mediaType = "movie",
            genres = listOf("Sci-Fi", "Drama"),
            releaseYear = 2026,
            runtimeMinutes = 118,
            ageRating = "PG-13",
            director = "Devon K. Park",
            cast = listOf("Sienna Cole", "Idris Mensah", "Helena Varga"),
            posterKey = "img_hero_nebula",
            backdropKey = "img_hero_nebula",
            trailerUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoy.mp4",
            hlsUrl = null,
            isFeatured = false,
            isVisible = true,
            isKidsFriendly = false,
            trendingScore = 905,
            popularityRank = 7
        ),
        CatalogItem(
            id = "skyline-drift",
            title = "Skyline Drift",
            synopsis = "High-altitude glider pilots and coastal weather chasers team up to map uncharted jet streams across the Pacific Northwest.",
            mediaType = "movie",
            genres = listOf("Action", "Adventure"),
            releaseYear = 2024,
            runtimeMinutes = 96,
            ageRating = "PG",
            director = "Hannah Kim",
            cast = listOf("Noah Mercer", "Tessa Rhodes"),
            posterKey = "img_poster_horizon",
            backdropKey = "img_poster_horizon",
            trailerUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
            hlsUrl = null,
            isFeatured = false,
            isVisible = true,
            isKidsFriendly = true,
            trendingScore = 830,
            popularityRank = 8
        ),
        CatalogItem(
            id = "hollow-signal",
            title = "Hollow Signal",
            synopsis = "A night-shift storm spotter alone in an abandoned mountain relay station hears an eerie distress call repeating her own voice from decades earlier.",
            mediaType = "movie",
            genres = listOf("Horror", "Mystery", "Thriller"),
            releaseYear = 2026,
            runtimeMinutes = 98,
            ageRating = "PG-13",
            director = "Marcus Vance",
            cast = listOf("Claire Ashford", "Julian Ward"),
            posterKey = "img_poster_chronicle",
            backdropKey = "img_poster_chronicle",
            trailerUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
            hlsUrl = null,
            isFeatured = false,
            isVisible = true,
            isKidsFriendly = false,
            trendingScore = 915,
            popularityRank = 9,
            tags = listOf("Supernatural", "Dark Suspense"),
            keywords = listOf("haunted", "radio", "mountain", "ghost")
        ),
        CatalogItem(
            id = "blackwater-manor",
            title = "Blackwater Manor",
            synopsis = "Inheriting a misty coastal estate, a genealogist unlocks sealed wing corridors where every portrait hides an unsolved disappearance.",
            mediaType = "series",
            genres = listOf("Mystery", "Horror", "Drama"),
            releaseYear = 2026,
            runtimeMinutes = 48,
            ageRating = "TV-14",
            director = "Soren Lindholm",
            cast = listOf("Eleanor Brooks", "Theo Ashford", "Maya Vance"),
            posterKey = "img_poster_chronicle",
            backdropKey = "img_hero_nebula",
            trailerUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
            hlsUrl = null,
            isFeatured = false,
            isVisible = true,
            isKidsFriendly = false,
            trendingScore = 890,
            popularityRank = 10,
            tags = listOf("Binge-Worthy", "Gothic", "Crime"),
            keywords = listOf("mansion", "mystery", "disappearance")
        ),
        CatalogItem(
            id = "turbo-couriers",
            title = "Turbo Couriers",
            synopsis = "Three chaotic bike messengers accidentally swap briefcases with a rival culinary crew, sparking a cross-town comedy chase before dinner service.",
            mediaType = "movie",
            genres = listOf("Comedy", "Adventure", "Romance"),
            releaseYear = 2025,
            runtimeMinutes = 92,
            ageRating = "PG",
            director = "Nadia Okafor",
            cast = listOf("Leo Kim", "Sam Rivera", "Chloe Park"),
            posterKey = "img_poster_odyssey",
            backdropKey = "img_poster_horizon",
            trailerUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
            hlsUrl = null,
            isFeatured = false,
            isVisible = true,
            isKidsFriendly = true,
            trendingScore = 875,
            popularityRank = 11,
            tags = listOf("Feel-Good", "Rom-Com", "Fast-Paced"),
            keywords = listOf("messengers", "comedy", "city", "chase"),
            isStaffPick = true
        ),
        CatalogItem(
            id = "starblade-academy",
            title = "Starblade Academy",
            synopsis = "A clumsy scholarship student discovers an ancient mech sword and teams up with misfit prodigies in a high-spirited anime comedy saga.",
            mediaType = "series",
            genres = listOf("Anime", "Comedy", "Action"),
            releaseYear = 2026,
            runtimeMinutes = 24,
            ageRating = "TV-PG",
            director = "Kenji Sato",
            cast = listOf("Hana Takahashi", "Renji Sato", "Koa Lee"),
            posterKey = "img_poster_odyssey",
            backdropKey = "img_hero_nebula",
            trailerUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            hlsUrl = null,
            isFeatured = false,
            isVisible = true,
            isKidsFriendly = true,
            trendingScore = 925,
            popularityRank = 4,
            tags = listOf("Anime", "Feel-Good", "Binge-Worthy", "Superhero"),
            keywords = listOf("academy", "anime", "mecha", "comedy")
        ),
        CatalogItem(
            id = "solstice-2099",
            title = "Solstice 2099",
            synopsis = "Two orbital architects uncover a buried solar vault beneath Europa that could save Earth's dying grid—if they survive reentry.",
            mediaType = "movie",
            genres = listOf("Sci-Fi", "Adventure"),
            releaseYear = 2027,
            runtimeMinutes = 128,
            ageRating = "PG-13",
            director = "Elena Vance",
            cast = listOf("Aria Chen", "Devon Brooks"),
            posterKey = "img_hero_nebula",
            backdropKey = "img_hero_nebula",
            trailerUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
            videoUrl = "",
            hlsUrl = null,
            isFeatured = false,
            isVisible = true,
            isKidsFriendly = false,
            trendingScore = 880,
            popularityRank = 12,
            tags = listOf("Recently Announced", "Space"),
            keywords = listOf("europa", "orbit", "expedition"),
            isComingSoon = true,
            expectedReleaseDate = "Jan 22, 2027"
        )
    )

    fun defaultSeasonsForSeries(contentId: String): List<Season> {
        return when (contentId) {
            "midnight-bridge" -> listOf(
                Season(id = "season-1", seasonNumber = 1, title = "Season 1: Lower Ward", year = 2025, isVisible = true),
                Season(id = "season-2", seasonNumber = 2, title = "Season 2: Blackout", year = 2026, isVisible = true)
            )
            "orbital-zero" -> listOf(
                Season(id = "season-1", seasonNumber = 1, title = "Season 1: First Contact", year = 2026, isVisible = true)
            )
            "coastal-horizon" -> listOf(
                Season(id = "season-1", seasonNumber = 1, title = "Season 1: Highway One", year = 2025, isVisible = true)
            )
            "aurora-crown" -> listOf(
                Season(id = "season-1", seasonNumber = 1, title = "Season 1: The Astrolabe", year = 2025, isVisible = true)
            )
            "meadow-tales" -> listOf(
                Season(id = "season-1", seasonNumber = 1, title = "Season 1: Spring Meadow", year = 2025, isVisible = true)
            )
            "velocity-atlas" -> listOf(
                Season(id = "season-1", seasonNumber = 1, title = "Season 1: Alpine Passes", year = 2024, isVisible = true)
            )
            "blackwater-manor" -> listOf(
                Season(id = "season-1", seasonNumber = 1, title = "Season 1: The East Wing", year = 2026, isVisible = true)
            )
            "starblade-academy" -> listOf(
                Season(id = "season-1", seasonNumber = 1, title = "Season 1: Mech Orientation", year = 2026, isVisible = true)
            )
            else -> emptyList()
        }
    }

    fun defaultEpisodesForSeason(contentId: String, seasonNumber: Int): List<Episode> {
        return when (contentId) {
            "midnight-bridge" -> if (seasonNumber == 1) {
                listOf(
                    Episode(
                        id = "s1e1", seasonNumber = 1, episodeNumber = 1,
                        title = "The Encrypted Manifest",
                        synopsis = "Archivist Mira Vance discovers a missing transit ledger inside a decommissioned metro relay.",
                        runtimeMinutes = 46, thumbnailKey = "img_poster_chronicle",
                        videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                        hlsUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", isVisible = true
                    ),
                    Episode(
                        id = "s1e2", seasonNumber = 1, episodeNumber = 2,
                        title = "Neon Rain",
                        synopsis = "A coded broadcast from the harbor bridge points toward a forgotten engineering division.",
                        runtimeMinutes = 44, thumbnailKey = "img_poster_chronicle",
                        videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
                        isVisible = true
                    ),
                    Episode(
                        id = "s1e3", seasonNumber = 1, episodeNumber = 3,
                        title = "Glass Meridian",
                        synopsis = "Mira and Marcus race across the suspended rail line before the midnight lockdown triggers.",
                        runtimeMinutes = 48, thumbnailKey = "img_hero_nebula",
                        videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                        isVisible = true
                    )
                )
            } else {
                listOf(
                    Episode(
                        id = "s2e1", seasonNumber = 2, episodeNumber = 1,
                        title = "Grid Down",
                        synopsis = "Three months after the Meridian leak, the city's backup sub-station goes dark.",
                        runtimeMinutes = 49, thumbnailKey = "img_poster_chronicle",
                        videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                        isVisible = true
                    ),
                    Episode(
                        id = "s2e2", seasonNumber = 2, episodeNumber = 2,
                        title = "Echo Protocol",
                        synopsis = "An unlikely ally hands over the master key to the harbor vault.",
                        runtimeMinutes = 47, thumbnailKey = "img_poster_horizon",
                        videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                        isVisible = true
                    )
                )
            }
            "orbital-zero" -> listOf(
                Episode(
                    id = "s1e1", seasonNumber = 1, episodeNumber = 1,
                    title = "Carrier Wave",
                    synopsis = "Commander Kaelen detects an impossible acoustic signature echoing from the outer rings.",
                    runtimeMinutes = 52, thumbnailKey = "img_hero_nebula",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                    hlsUrl = "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8",
                    isVisible = true
                ),
                Episode(
                    id = "s1e2", seasonNumber = 1, episodeNumber = 2,
                    title = "Red Shift",
                    synopsis = "The crew calibrates the deep-space dish while solar storms threaten station telemetry.",
                    runtimeMinutes = 50, thumbnailKey = "img_hero_nebula",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                    isVisible = true
                )
            )
            "coastal-horizon" -> listOf(
                Episode(
                    id = "s1e1", seasonNumber = 1, episodeNumber = 1,
                    title = "Mile Marker Zero",
                    synopsis = "Clara and Liam pack their father's restored coupe and leave Monterey at dawn.",
                    runtimeMinutes = 42, thumbnailKey = "img_poster_horizon",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4",
                    isVisible = true
                ),
                Episode(
                    id = "s1e2", seasonNumber = 1, episodeNumber = 2,
                    title = "Fog Line",
                    synopsis = "An unexpected detour near Cape Perpetua brings an old promise back to light.",
                    runtimeMinutes = 41, thumbnailKey = "img_poster_horizon",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
                    isVisible = true
                )
            )
            "aurora-crown" -> listOf(
                Episode(
                    id = "s1e1", seasonNumber = 1, episodeNumber = 1,
                    title = "Compass of Starlight",
                    synopsis = "Freya deciphers the first brass ring of the astrolabe above the glacial fjord.",
                    runtimeMinutes = 38, thumbnailKey = "img_poster_odyssey",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                    isVisible = true
                ),
                Episode(
                    id = "s1e2", seasonNumber = 1, episodeNumber = 2,
                    title = "The High Pass",
                    synopsis = "Crossing the aurora bridge unlocks a hidden valley untouched for centuries.",
                    runtimeMinutes = 40, thumbnailKey = "img_poster_odyssey",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                    isVisible = true
                )
            )
            "meadow-tales" -> listOf(
                Episode(
                    id = "s1e1", seasonNumber = 1, episodeNumber = 1,
                    title = "Morning in Cloverbrook",
                    synopsis = "Pip and Bramble organize the great acorn harvest while dodging playful squirrels.",
                    runtimeMinutes = 22, thumbnailKey = "img_poster_odyssey",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    hlsUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                    isVisible = true
                ),
                Episode(
                    id = "s1e2", seasonNumber = 1, episodeNumber = 2,
                    title = "Lantern Festival",
                    synopsis = "Fireflies guide the woodland friends back home before dusk.",
                    runtimeMinutes = 20, thumbnailKey = "img_poster_odyssey",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                    isVisible = true
                )
            )
            "velocity-atlas" -> listOf(
                Episode(
                    id = "s1e1", seasonNumber = 1, episodeNumber = 1,
                    title = "Stelvio Dawn",
                    synopsis = "Exploring the forty-eight switchbacks of northern Italy at sunrise.",
                    runtimeMinutes = 35, thumbnailKey = "img_poster_horizon",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WhatCarCanYouGetForAGrand.mp4",
                    isVisible = true
                )
            )
            "blackwater-manor" -> listOf(
                Episode(
                    id = "s1e1", seasonNumber = 1, episodeNumber = 1,
                    title = "Key to the Salt Corridor",
                    synopsis = "Eleanor arrives at Blackwater Manor as coastal fog cuts off the causeway.",
                    runtimeMinutes = 48, thumbnailKey = "img_poster_chronicle",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                    isVisible = true
                )
            )
            "starblade-academy" -> listOf(
                Episode(
                    id = "s1e1", seasonNumber = 1, episodeNumber = 1,
                    title = "Cafeteria Mech Duel",
                    synopsis = "Renji accidentally activates the academy founder's heirloom blade during lunch.",
                    runtimeMinutes = 24, thumbnailKey = "img_poster_odyssey",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    isVisible = true
                ),
                Episode(
                    id = "s1e2", seasonNumber = 1, episodeNumber = 2,
                    title = "Detention Orbit",
                    synopsis = "Squad Zero has forty minutes to pass obstacle training without crashing.",
                    runtimeMinutes = 24, thumbnailKey = "img_poster_odyssey",
                    videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                    isVisible = true
                )
            )
            else -> emptyList()
        }
    }
}
