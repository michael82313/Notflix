const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastTimestamp() {
  return new Date(Date.now() - 1000);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read catalog or users", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("catalog").where("isVisible", "==", true).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("User can create own account as member, cannot self-assign admin role", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();

  const ts = pastTimestamp();
  const validAccount = {
    userId: ALICE_UID,
    email: "alice@example.com",
    displayName: "Alice",
    role: "member",
    languagePreference: "English",
    autoplayNext: true,
    defaultQuality: "Auto",
    subtitlesEnabled: true,
    subtitleSize: "Medium",
    createdAt: ts,
    updatedAt: ts,
  };

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validAccount));

  // Phone-authenticated user with empty email and phoneNumber
  const phoneUid = "phone_user_1";
  const phoneDb = testEnv.authenticatedContext(phoneUid, {
    phone_number: "+15551234567",
  }).firestore();
  await assertSucceeds(
    phoneDb.collection("users").doc(phoneUid).set({
      userId: phoneUid,
      email: "",
      phoneNumber: "+15551234567",
      displayName: "+15551234567",
      role: "member",
      languagePreference: "English",
      autoplayNext: true,
      defaultQuality: "Auto",
      subtitlesEnabled: true,
      subtitleSize: "Medium",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@example.com",
    email_verified: true,
  }).firestore();

  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      ...validAccount,
      userId: BOB_UID,
      email: "bob@example.com",
      role: "admin",
    })
  );
});

test("Cross-user isolation: Bob cannot read or modify Alice's profiles or watchlist", async () => {
  const ts = pastTimestamp();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(ALICE_UID).collection("profiles").doc("p1").set({
      userId: ALICE_UID,
      name: "Alice Main",
      avatarKey: "crimson",
      isKids: false,
      preferredGenres: ["Sci-Fi"],
      language: "English",
      subtitlesEnabled: true,
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("profiles").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("profiles").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("profiles").doc("p1").delete()
  );
});

test("Privilege escalation test: non-admin email cannot spoof admin record or write catalog", async () => {
  const ts = pastTimestamp();
  const spoofDb = testEnv.authenticatedContext("spoof_uid", {
    email: "attacker@example.com",
    email_verified: true,
  }).firestore();

  await assertFails(
    spoofDb.collection("admins").doc("spoof_uid").set({
      uid: "spoof_uid",
      email: "michael.tollackson82313@gmail.com",
      grantedBy: "self",
      createdAt: ts,
    })
  );

  await assertFails(
    spoofDb.collection("catalog").doc("movie_1").set({
      title: "Fake Movie",
      synopsis: "Unauthorized",
      mediaType: "movie",
      genres: ["Sci-Fi"],
      releaseYear: 2025,
      runtimeMinutes: 90,
      ageRating: "PG-13",
      director: "Someone",
      cast: ["Actor A"],
      posterKey: "img_poster_chronicle",
      backdropKey: "img_hero_nebula",
      videoUrl: "https://example.com/v.mp4",
      isFeatured: false,
      isVisible: true,
      isKidsFriendly: false,
      trendingScore: 50,
      popularityRank: 1,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Catalog visibility & admin write protection", async () => {
  const ts = pastTimestamp();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("admins").doc(ADMIN_UID).set({
      uid: ADMIN_UID,
      email: "admin@velora.app",
      grantedBy: "system",
      createdAt: ts,
    });
    await db.collection("catalog").doc("hidden_movie").set({
      title: "Hidden",
      synopsis: "Draft movie",
      mediaType: "movie",
      genres: ["Drama"],
      releaseYear: 2025,
      runtimeMinutes: 100,
      ageRating: "PG",
      director: "Director",
      cast: ["Actor"],
      posterKey: "img_poster_horizon",
      backdropKey: "img_hero_nebula",
      videoUrl: "https://example.com/v.mp4",
      isFeatured: false,
      isVisible: false,
      isKidsFriendly: true,
      trendingScore: 10,
      popularityRank: 5,
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();

  await assertFails(aliceDb.collection("catalog").doc("hidden_movie").get());
  await assertFails(aliceDb.collection("catalog").get());
  await assertSucceeds(aliceDb.collection("catalog").where("isVisible", "==", true).get());

  await assertFails(
    aliceDb.collection("catalog").doc("movie_2").set({
      title: "Movie 2",
      synopsis: "Test",
      mediaType: "movie",
      genres: ["Action"],
      releaseYear: 2025,
      runtimeMinutes: 110,
      ageRating: "PG-13",
      director: "Dir",
      cast: [],
      posterKey: "img_poster_odyssey",
      backdropKey: "img_hero_nebula",
      videoUrl: "https://example.com/v.mp4",
      isFeatured: false,
      isVisible: true,
      isKidsFriendly: false,
      trendingScore: 80,
      popularityRank: 2,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(adminDb.collection("catalog").get());
  await assertSucceeds(
    adminDb.collection("catalog").doc("movie_2").set({
      title: "Movie 2",
      synopsis: "Test",
      mediaType: "movie",
      genres: ["Action"],
      releaseYear: 2025,
      runtimeMinutes: 110,
      ageRating: "PG-13",
      director: "Dir",
      cast: [],
      posterKey: "img_poster_odyssey",
      backdropKey: "img_hero_nebula",
      videoUrl: "https://example.com/v.mp4",
      videoSourceType: "mp4",
      isFeatured: false,
      isVisible: true,
      isKidsFriendly: false,
      trendingScore: 80,
      popularityRank: 2,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Admin catalog videoSourceType validation & TV show seasons/episodes access control", async () => {
  const ts = pastTimestamp();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("admins").doc(ADMIN_UID).set({
      uid: ADMIN_UID,
      email: "admin@notflix.app",
      grantedBy: "system",
      createdAt: ts,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();

  // Admin can create a YouTube-backed movie and update it to HLS
  await assertSucceeds(
    adminDb.collection("catalog").doc("yt_movie").set({
      title: "YouTube Feature",
      synopsis: "Official YouTube embed movie",
      mediaType: "movie",
      genres: ["Sci-Fi"],
      releaseYear: 2026,
      runtimeMinutes: 105,
      ageRating: "PG-13",
      director: "Notflix Studios",
      cast: ["Actor One"],
      posterKey: "img_poster_chronicle",
      posterUrl: "https://example.com/poster.jpg",
      backdropKey: "img_hero_nebula",
      backdropUrl: "https://example.com/backdrop.jpg",
      trailerUrl: "https://youtu.be/dQw4w9WgXcQ",
      videoUrl: "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
      videoSourceType: "youtube",
      isFeatured: true,
      isVisible: true,
      isKidsFriendly: false,
      trendingScore: 95,
      popularityRank: 1,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    adminDb.collection("catalog").doc("yt_movie").update({
      videoSourceType: "hls",
      videoUrl: "https://example.com/streams/master.m3u8",
      hlsUrl: "https://example.com/streams/master.m3u8",
      updatedAt: ts,
    })
  );

  // Invalid videoSourceType is rejected even for admin
  await assertFails(
    adminDb.collection("catalog").doc("bad_source_movie").set({
      title: "Invalid Source",
      synopsis: "Should fail enum check",
      mediaType: "movie",
      genres: ["Drama"],
      releaseYear: 2026,
      runtimeMinutes: 90,
      ageRating: "PG",
      director: "Notflix Studios",
      cast: [],
      posterKey: "img_poster_chronicle",
      backdropKey: "img_hero_nebula",
      videoUrl: "https://example.com/v.mp4",
      videoSourceType: "torrent",
      isFeatured: false,
      isVisible: true,
      isKidsFriendly: false,
      trendingScore: 10,
      popularityRank: 5,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Admin can safely check non-existent catalog/season/episode docs before creating
  await assertSucceeds(adminDb.collection("catalog").doc("show_1").get());

  // Admin can create a TV Show, Season, and Episodes with YouTube/MP4/HLS sources
  await assertSucceeds(
    adminDb.collection("catalog").doc("show_1").set({
      title: "Orbital Echoes",
      synopsis: "Anthology series",
      mediaType: "series",
      genres: ["Sci-Fi", "Thriller"],
      releaseYear: 2026,
      runtimeMinutes: 45,
      ageRating: "TV-14",
      director: "Notflix Studios",
      cast: ["Cast A"],
      posterKey: "img_poster_chronicle",
      backdropKey: "img_hero_nebula",
      videoUrl: "",
      videoSourceType: "mp4",
      isFeatured: false,
      isVisible: true,
      isKidsFriendly: false,
      trendingScore: 90,
      popularityRank: 2,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(adminDb.collection("catalog").doc("show_1").collection("seasons").doc("season-1").get());
  await assertSucceeds(
    adminDb.collection("catalog").doc("show_1").collection("seasons").doc("season-1").set({
      seasonNumber: 1,
      title: "Season 1",
      year: 2026,
      isVisible: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    adminDb.collection("catalog").doc("show_1")
      .collection("seasons").doc("season-1")
      .collection("episodes").doc("s1e1").get()
  );

  await assertSucceeds(
    adminDb.collection("catalog").doc("show_1")
      .collection("seasons").doc("season-1")
      .collection("episodes").doc("s1e1").set({
        seasonNumber: 1,
        episodeNumber: 1,
        title: "Pilot",
        synopsis: "First episode via YouTube",
        runtimeMinutes: 45,
        thumbnailKey: "img_hero_nebula",
        videoUrl: "https://www.youtube.com/watch?v=aqz-KE-bpKQ",
        videoSourceType: "youtube",
        isVisible: true,
        createdAt: ts,
        updatedAt: ts,
      })
  );

  // Admin can save multi-host auto/embed catalog entries & episodes with providerName
  await assertSucceeds(
    adminDb.collection("catalog").doc("movie_vimeo").set({
      title: "Vimeo Showcase",
      synopsis: "Official embedded Vimeo showcase",
      mediaType: "movie",
      genres: ["Documentary"],
      releaseYear: 2026,
      runtimeMinutes: 82,
      ageRating: "PG-13",
      director: "Notflix Studios",
      cast: ["Cast A"],
      posterKey: "img_poster_chronicle",
      backdropKey: "img_hero_nebula",
      videoUrl: "https://vimeo.com/76979871",
      videoSourceType: "auto",
      providerName: "Vimeo",
      isFeatured: false,
      isVisible: true,
      isKidsFriendly: true,
      trendingScore: 88,
      popularityRank: 5,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    adminDb.collection("catalog").doc("show_1")
      .collection("seasons").doc("season-1")
      .collection("episodes").doc("s1e_embed").set({
        seasonNumber: 1,
        episodeNumber: 2,
        title: "Episode Two (Vimeo Embed)",
        synopsis: "Multi-host embed episode",
        runtimeMinutes: 44,
        thumbnailKey: "img_hero_nebula",
        videoUrl: "https://vimeo.com/76979871",
        videoSourceType: "embed",
        providerName: "Vimeo",
        isVisible: true,
        createdAt: ts,
        updatedAt: ts,
      })
  );

  // Non-admin member can read visible seasons/episodes but CANNOT create or edit them
  await assertSucceeds(
    aliceDb.collection("catalog").doc("show_1")
      .collection("seasons").doc("season-1")
      .collection("episodes").where("isVisible", "==", true).get()
  );

  await assertFails(
    aliceDb.collection("catalog").doc("show_1")
      .collection("seasons").doc("season-2").set({
        seasonNumber: 2,
        title: "Season 2",
        year: 2026,
        isVisible: true,
        createdAt: ts,
        updatedAt: ts,
      })
  );

  await assertFails(
    aliceDb.collection("catalog").doc("show_1")
      .collection("seasons").doc("season-1")
      .collection("episodes").doc("s1e2").set({
        seasonNumber: 1,
        episodeNumber: 2,
        title: "Unauthorized Ep",
        synopsis: "Should fail",
        runtimeMinutes: 40,
        thumbnailKey: "img_hero_nebula",
        videoUrl: "https://example.com/ep2.mp4",
        videoSourceType: "mp4",
        isVisible: true,
        createdAt: ts,
        updatedAt: ts,
      })
  );
});

test("Shadow update test: ghost field rejected on user profile", async () => {
  const ts = pastTimestamp();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("profiles").doc("p1").set({
      userId: ALICE_UID,
      name: "Alice",
      avatarKey: "crimson",
      isKids: false,
      preferredGenres: [],
      language: "English",
      subtitlesEnabled: true,
      isAdminOverride: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Subscription architecture: user auto-enrolls in permanent Free plan only and cannot escalate", async () => {
  const ts = pastTimestamp();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();

  await assertFails(
    unauthDb
      .collection("subscriptionPlans")
      .where("isActive", "==", true)
      .where("isPublic", "==", true)
      .where("isArchived", "==", false)
      .get()
  );
  await assertFails(unauthDb.collection("userSubscriptions").doc(ALICE_UID).get());

  // User can enroll themselves in permanent Free plan
  await assertSucceeds(
    aliceDb.collection("userSubscriptions").doc(ALICE_UID).set({
      userId: ALICE_UID,
      planId: "free",
      planName: "Free",
      status: "active",
      isPermanentFree: true,
      assignedBy: "system",
      startsAt: ts,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // User cannot enroll themselves in paid plan or read another user's subscription
  await assertFails(
    bobDb.collection("userSubscriptions").doc(BOB_UID).set({
      userId: BOB_UID,
      planId: "ultimate",
      planName: "Ultimate",
      status: "active",
      isPermanentFree: false,
      assignedBy: "self",
      startsAt: ts,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(bobDb.collection("userSubscriptions").doc(ALICE_UID).get());
  await assertFails(aliceDb.collection("userSubscriptions").doc(ALICE_UID).delete());
});

test("Subscription plans: permanent Free plan cannot be disabled or deleted even by admin", async () => {
  const ts = pastTimestamp();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("admins").doc(ADMIN_UID).set({
      uid: ADMIN_UID,
      email: "admin@velora.app",
      grantedBy: "system",
      createdAt: ts,
    });
    await db.collection("subscriptionPlans").doc("free").set({
      tierCode: "free",
      name: "Free",
      description: "Permanent free streaming tier",
      isPermanentFree: true,
      isActive: true,
      isPublic: true,
      isArchived: false,
      monthlyPriceCents: 0,
      yearlyPriceCents: 0,
      maxQuality: "Auto",
      maxSimultaneousStreams: 4,
      maxProfiles: 5,
      featuresSummary: ["Full catalog", "Multi-profile"],
      displayOrder: 0,
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("subscriptionPlans").doc("premium").set({
      tierCode: "premium",
      name: "Premium",
      description: "Future expansion tier blueprint",
      isPermanentFree: false,
      isActive: false,
      isPublic: false,
      isArchived: false,
      monthlyPriceCents: 0,
      yearlyPriceCents: 0,
      maxQuality: "4K",
      maxSimultaneousStreams: 4,
      maxProfiles: 5,
      featuresSummary: ["Future tier"],
      displayOrder: 2,
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();

  // Member can query active+public+non-archived plans, cannot view inactive future plan directly
  await assertSucceeds(
    aliceDb
      .collection("subscriptionPlans")
      .where("isActive", "==", true)
      .where("isPublic", "==", true)
      .where("isArchived", "==", false)
      .get()
  );
  await assertFails(aliceDb.collection("subscriptionPlans").doc("premium").get());

  // Admin cannot disable or delete permanent Free plan
  await assertFails(
    adminDb.collection("subscriptionPlans").doc("free").update({
      isActive: false,
      updatedAt: ts,
    })
  );
  await assertFails(adminDb.collection("subscriptionPlans").doc("free").delete());

  // Admin can manage inactive blueprint plans
  await assertSucceeds(adminDb.collection("subscriptionPlans").get());
  await assertSucceeds(
    adminDb.collection("subscriptionPlans").doc("premium").collection("features").doc("f1").set({
      featureKey: "full_catalog",
      label: "Full Catalog Access",
      description: "Access all available movies and TV shows",
      enabled: true,
      displayOrder: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Catalog discovery metadata & Coming Soon items allowed for admins only", async () => {
  const ts = pastTimestamp();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("admins").doc(ADMIN_UID).set({
      uid: ADMIN_UID,
      email: "admin@velora.app",
      grantedBy: "system",
      createdAt: ts,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();

  const comingSoonPayload = {
    title: "Solstice 2099",
    synopsis: "Upcoming sci-fi expedition to Europa.",
    mediaType: "movie",
    genres: ["Sci-Fi", "Adventure"],
    releaseYear: 2027,
    runtimeMinutes: 128,
    ageRating: "PG-13",
    director: "Elena Vance",
    cast: ["Aria Chen", "Devon Brooks"],
    posterKey: "img_hero_nebula",
    backdropKey: "img_hero_nebula",
    trailerUrl: "https://example.com/trailer.mp4",
    videoUrl: "",
    videoSourceType: "auto",
    isFeatured: false,
    isVisible: true,
    isKidsFriendly: false,
    trendingScore: 880,
    popularityRank: 12,
    tags: ["Recently Announced", "Space"],
    keywords: ["europa", "orbit"],
    isStaffPick: true,
    isAwardWinning: false,
    isComingSoon: true,
    expectedReleaseDate: "Jan 22, 2027",
    createdAt: ts,
    updatedAt: ts,
  };

  await assertFails(aliceDb.collection("catalog").doc("solstice-2099").set(comingSoonPayload));
  await assertSucceeds(adminDb.collection("catalog").doc("solstice-2099").set(comingSoonPayload));

  // Admin can update leavingSoonAt without losing immutable createdAt
  await assertSucceeds(
    adminDb.collection("catalog").doc("solstice-2099").update({
      isComingSoon: false,
      leavingSoonAt: "Dec 31, 2027",
      updatedAt: ts,
    })
  );

  // Member can read visible Coming Soon metadata
  await assertSucceeds(aliceDb.collection("catalog").where("isVisible", "==", true).get());
});

test("Homepage configuration & row rules: signed-in users read, only admins write or delete", async () => {
  const ts = pastTimestamp();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("admins").doc(ADMIN_UID).set({
      uid: ADMIN_UID,
      email: "admin@velora.app",
      grantedBy: "system",
      createdAt: ts,
    });
  });

  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();

  const settingsPayload = {
    maxItemsPerRow: 12,
    maxBecauseYouWatchedRows: 5,
    enabledGenres: ["Sci-Fi", "Horror", "Anime", "Comedy"],
    updatedAt: ts,
  };
  const rowPayload = {
    rowKey: "because_you_watched",
    title: "Because You Watched",
    enabled: true,
    displayOrder: 2,
    maxItems: 10,
    personalized: true,
    updatedAt: ts,
  };

  // Unauthenticated reads fail
  await assertFails(unauthDb.collection("homepageConfig").doc("global").get());
  await assertFails(unauthDb.collection("homepageConfig").doc("global").collection("rows").get());

  // Member writes fail
  await assertFails(aliceDb.collection("homepageConfig").doc("global").set(settingsPayload));
  await assertFails(
    aliceDb.collection("homepageConfig").doc("global").collection("rows").doc("because_you_watched").set(rowPayload)
  );

  // Admin writes succeed
  await assertSucceeds(adminDb.collection("homepageConfig").doc("global").set(settingsPayload));
  await assertSucceeds(
    adminDb.collection("homepageConfig").doc("global").collection("rows").doc("because_you_watched").set(rowPayload)
  );

  // Signed-in member can read settings & row rules, cannot delete
  await assertSucceeds(aliceDb.collection("homepageConfig").doc("global").get());
  await assertSucceeds(aliceDb.collection("homepageConfig").doc("global").collection("rows").get());
  await assertFails(
    aliceDb.collection("homepageConfig").doc("global").collection("rows").doc("because_you_watched").delete()
  );

  // Invalid row rule bounds fail even for admin
  await assertFails(
    adminDb.collection("homepageConfig").doc("global").collection("rows").doc("bad_row").set({
      ...rowPayload,
      maxItems: 99,
    })
  );
});

// =========================================================================
// MASTER GOVERNANCE, OWNER PROTECTION, PUBLISHER, SUBSCRIPTION & AUDIT TESTS
// =========================================================================

const OWNER_UID = "owner_001";
const ADMIN_TWO_UID = "admin_002";
const PUBLISHER_UID = "publisher_001";
const PUBLISHER_TWO_UID = "publisher_002";

function baseUserDoc(uid, email, role = "viewer", publisherStatus = "none", ts = pastTimestamp()) {
  return {
    userId: uid,
    email,
    displayName: uid,
    role,
    publisherStatus,
    isSuspended: false,
    languagePreference: "English",
    autoplayNext: true,
    defaultQuality: "Auto",
    subtitlesEnabled: true,
    subtitleSize: "Medium",
    createdAt: ts,
    updatedAt: ts,
  };
}

function baseSubmissionDoc(publisherUid, overrides = {}, ts = pastTimestamp()) {
  return {
    publisherUid,
    publisherName: "Indie Film Co",
    mediaType: "movie",
    title: "Midnight Meridian",
    synopsis: "A slow-burn sci-fi feature cleared for streaming.",
    genres: ["Sci-Fi", "Drama"],
    releaseYear: 2026,
    runtimeMinutes: 104,
    ageRating: "PG-13",
    directorOrCreator: "A. Director",
    cast: ["Lead One", "Lead Two"],
    posterKey: "img_poster_chronicle",
    backdropKey: "img_hero_nebula",
    videoUrl: "https://example.com/streams/meridian.m3u8",
    videoSourceType: "hls",
    isKidsFriendly: false,
    tags: ["Indie"],
    seasons: [],
    distributionAuthorized: true,
    distributionRightsStatement: "I confirm I hold the legal rights to distribute this title.",
    reviewStatus: "pending_review",
    privateReviewerNotes: "",
    isRevision: false,
    revisionNumber: 1,
    createdAt: ts,
    updatedAt: ts,
    ...overrides,
  };
}

test("Owner security: Owner can appoint/remove Admins; Admins CANNOT modify/demote/suspend/delete Owner or manage Admins", async () => {
  const ts = pastTimestamp();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("owners").doc(OWNER_UID).set({
      uid: OWNER_UID,
      email: "michael.tollackson82313@gmail.com",
      establishedBy: "owner_bootstrap",
      createdAt: ts,
    });
    await db.collection("users").doc(OWNER_UID).set(baseUserDoc(OWNER_UID, "michael.tollackson82313@gmail.com", "owner", "none", ts));
    await db.collection("admins").doc(ADMIN_UID).set({
      uid: ADMIN_UID,
      email: "admin1@notflix.app",
      grantedBy: OWNER_UID,
      createdAt: ts,
    });
    await db.collection("users").doc(ADMIN_UID).set(baseUserDoc(ADMIN_UID, "admin1@notflix.app", "admin", "none", ts));
    await db.collection("users").doc(ADMIN_TWO_UID).set(baseUserDoc(ADMIN_TWO_UID, "admin2@notflix.app", "viewer", "none", ts));
    await db.collection("users").doc(ALICE_UID).set(baseUserDoc(ALICE_UID, "alice@example.com", "viewer", "none", ts));
  });

  const ownerDb = testEnv.authenticatedContext(OWNER_UID, { email: "michael.tollackson82313@gmail.com" }).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: "admin1@notflix.app" }).firestore();

  // Immutable /owners/{uid}: nobody (not even Owner or Admin) can update or delete Owner record
  await assertFails(ownerDb.collection("owners").doc(OWNER_UID).update({ email: "changed@notflix.app" }));
  await assertFails(ownerDb.collection("owners").doc(OWNER_UID).delete());
  await assertFails(adminDb.collection("owners").doc(OWNER_UID).delete());

  // Admin CANNOT demote, suspend, modify, or delete Owner account
  await assertFails(adminDb.collection("users").doc(OWNER_UID).update({ role: "viewer", updatedAt: ts }));
  await assertFails(adminDb.collection("users").doc(OWNER_UID).update({ isSuspended: true, updatedAt: ts }));
  await assertFails(adminDb.collection("users").doc(OWNER_UID).delete());

  // Admin CANNOT appoint or remove other Admins
  await assertFails(
    adminDb.collection("admins").doc(ADMIN_TWO_UID).set({
      uid: ADMIN_TWO_UID,
      email: "admin2@notflix.app",
      grantedBy: ADMIN_UID,
      createdAt: ts,
    })
  );
  await assertFails(adminDb.collection("users").doc(ADMIN_TWO_UID).update({ role: "admin", updatedAt: ts }));
  await assertFails(adminDb.collection("users").doc(ALICE_UID).update({ role: "owner", updatedAt: ts }));

  // Owner CAN appoint and later remove an Admin
  await assertSucceeds(
    ownerDb.collection("admins").doc(ADMIN_TWO_UID).set({
      uid: ADMIN_TWO_UID,
      email: "admin2@notflix.app",
      grantedBy: OWNER_UID,
      createdAt: ts,
    })
  );
  await assertSucceeds(ownerDb.collection("users").doc(ADMIN_TWO_UID).update({ role: "admin", updatedAt: ts }));

  // Admin CANNOT demote, suspend, or delete another Admin
  await assertFails(adminDb.collection("admins").doc(ADMIN_TWO_UID).delete());
  await assertFails(adminDb.collection("users").doc(ADMIN_TWO_UID).update({ role: "viewer", updatedAt: ts }));
  await assertFails(adminDb.collection("users").doc(ADMIN_TWO_UID).update({ isSuspended: true, updatedAt: ts }));
  await assertFails(adminDb.collection("users").doc(ADMIN_TWO_UID).delete());

  // Owner CAN demote/remove Admin 2
  await assertSucceeds(ownerDb.collection("admins").doc(ADMIN_TWO_UID).delete());
  await assertSucceeds(ownerDb.collection("users").doc(ADMIN_TWO_UID).update({ role: "viewer", updatedAt: ts }));

  // Admin CAN promote Viewer -> Publisher and suspend ordinary Viewer
  await assertSucceeds(
    adminDb.collection("users").doc(ALICE_UID).update({ role: "publisher", publisherStatus: "approved", updatedAt: ts })
  );
  await assertSucceeds(adminDb.collection("users").doc(ALICE_UID).update({ isSuspended: true, updatedAt: ts }));
});

test("Publisher application & submission security: no self-approval, strict publisher isolation, no direct catalog bypass", async () => {
  const ts = pastTimestamp();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("admins").doc(ADMIN_UID).set({
      uid: ADMIN_UID,
      email: "admin1@notflix.app",
      grantedBy: OWNER_UID,
      createdAt: ts,
    });
    await db.collection("users").doc(ALICE_UID).set(baseUserDoc(ALICE_UID, "alice@example.com", "viewer", "none", ts));
    await db.collection("users").doc(BOB_UID).set(baseUserDoc(BOB_UID, "bob@example.com", "viewer", "pending", ts));
    await db.collection("users").doc(PUBLISHER_UID).set(baseUserDoc(PUBLISHER_UID, "pub1@studio.com", "publisher", "approved", ts));
    await db.collection("users").doc(PUBLISHER_TWO_UID).set(baseUserDoc(PUBLISHER_TWO_UID, "pub2@studio.com", "publisher", "approved", ts));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const pub1Db = testEnv.authenticatedContext(PUBLISHER_UID).firestore();
  const pub2Db = testEnv.authenticatedContext(PUBLISHER_TWO_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();

  // Viewer can submit a private publisher application with status='pending' ONLY
  await assertSucceeds(
    aliceDb.collection("publisherApplications").doc(ALICE_UID).set({
      userId: ALICE_UID,
      publisherName: "Alice Films",
      contactEmail: "alice@example.com",
      organizationDescription: "Independent producer",
      intendedContentDetails: "Short films",
      status: "pending",
      reviewerNotes: "",
      submittedAt: ts,
      updatedAt: ts,
    })
  );

  // Viewer CANNOT self-approve publisher application or read another user's application
  await assertFails(
    bobDb.collection("publisherApplications").doc(BOB_UID).set({
      userId: BOB_UID,
      publisherName: "Bob Films",
      contactEmail: "bob@example.com",
      organizationDescription: "Trying to self-approve",
      intendedContentDetails: "Movies",
      status: "approved",
      reviewerNotes: "",
      submittedAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(bobDb.collection("publisherApplications").doc(ALICE_UID).get());
  await assertFails(aliceDb.collection("publisherApplications").doc(ALICE_UID).update({ status: "approved", updatedAt: ts }));

  // Unapproved Viewer & pending Applicant CANNOT submit content
  await assertFails(aliceDb.collection("contentSubmissions").doc("unauth_sub").set(baseSubmissionDoc(ALICE_UID, {}, ts)));
  await assertFails(bobDb.collection("contentSubmissions").doc("pending_applicant_sub").set(baseSubmissionDoc(BOB_UID, {}, ts)));

  // Approved Publisher CAN submit content ONLY as pending_review with distributionAuthorized == true
  await assertFails(
    pub1Db.collection("contentSubmissions").doc("sub_no_rights").set(
      baseSubmissionDoc(PUBLISHER_UID, { distributionAuthorized: false }, ts)
    )
  );
  await assertFails(
    pub1Db.collection("contentSubmissions").doc("sub_self_approved").set(
      baseSubmissionDoc(PUBLISHER_UID, { reviewStatus: "approved" }, ts)
    )
  );
  await assertSucceeds(
    pub1Db.collection("contentSubmissions").doc("sub_1").set(baseSubmissionDoc(PUBLISHER_UID, {}, ts))
  );

  // Publisher CANNOT write directly to public /catalog
  await assertFails(
    pub1Db.collection("catalog").doc("bypass_movie").set({
      title: "Bypass",
      synopsis: "Should fail",
      mediaType: "movie",
      genres: ["Drama"],
      releaseYear: 2026,
      runtimeMinutes: 90,
      ageRating: "PG-13",
      director: "Pub",
      cast: [],
      posterKey: "img_poster_chronicle",
      backdropKey: "img_hero_nebula",
      videoUrl: "https://example.com/v.mp4",
      isFeatured: false,
      isVisible: true,
      isKidsFriendly: false,
      trendingScore: 10,
      popularityRank: 1,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Viewers and OTHER Publishers CANNOT read or edit Publisher 1's submission
  await assertFails(aliceDb.collection("contentSubmissions").doc("sub_1").get());
  await assertFails(aliceDb.collection("contentSubmissions").get());
  await assertFails(pub2Db.collection("contentSubmissions").doc("sub_1").get());
  await assertFails(pub2Db.collection("contentSubmissions").doc("sub_1").update({ title: "Hijacked", updatedAt: ts }));

  // Publisher 1 CAN edit own pending_review submission (cannot self-approve)
  await assertSucceeds(
    pub1Db.collection("contentSubmissions").doc("sub_1").update({ title: "Midnight Meridian (Cut 2)", updatedAt: ts })
  );
  await assertFails(
    pub1Db.collection("contentSubmissions").doc("sub_1").update({ reviewStatus: "approved", updatedAt: ts })
  );

  // Admin requests changes -> Publisher can edit & resubmit back to pending_review
  await assertSucceeds(
    adminDb.collection("contentSubmissions").doc("sub_1").update({
      reviewStatus: "changes_requested",
      privateReviewerNotes: "Please verify audio sync.",
      lastReviewedByUid: ADMIN_UID,
      lastReviewedAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    pub1Db.collection("contentSubmissions").doc("sub_1").update({
      reviewStatus: "pending_review",
      synopsis: "Updated audio-synced cut.",
      updatedAt: ts,
    })
  );

  // Admin approves submission & logs review history
  await assertSucceeds(
    adminDb.collection("contentSubmissions").doc("sub_1").update({
      reviewStatus: "approved",
      privateReviewerNotes: "Cleared for public catalog.",
      lastReviewedByUid: ADMIN_UID,
      lastReviewedAt: ts,
      publishedCatalogId: "midnight-meridian",
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    adminDb.collection("contentSubmissions").doc("sub_1").collection("reviews").doc("rev_1").set({
      submissionId: "sub_1",
      reviewerUid: ADMIN_UID,
      reviewerRole: "admin",
      decision: "approved",
      notes: "Cleared for public catalog.",
      videoSourceVerified: true,
      rightsConfirmed: true,
      metadataVerified: true,
      createdAt: ts,
    })
  );

  // Publisher CANNOT edit an already-approved submission in-place (must submit a separate revision)
  await assertFails(
    pub1Db.collection("contentSubmissions").doc("sub_1").update({
      title: "Live Tamper Attempt",
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    pub1Db.collection("contentSubmissions").doc("sub_1_rev2").set(
      baseSubmissionDoc(PUBLISHER_UID, {
        title: "Midnight Meridian Director's Cut",
        isRevision: true,
        revisionNumber: 2,
        publishedCatalogId: "midnight-meridian",
      }, ts)
    )
  );
});

test("Subscription management & Free Access toggle: Admin/Owner full control; Viewer restricted to public active plans", async () => {
  const ts = pastTimestamp();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("admins").doc(ADMIN_UID).set({
      uid: ADMIN_UID,
      email: "admin1@notflix.app",
      grantedBy: OWNER_UID,
      createdAt: ts,
    });
    await db.collection("subscriptionPlans").doc("free").set({
      tierCode: "free",
      name: "Free",
      description: "Permanent free tier",
      isPermanentFree: true,
      isActive: true,
      isPublic: true,
      isArchived: false,
      billingInterval: "free",
      monthlyPriceCents: 0,
      yearlyPriceCents: 0,
      maxQuality: "Auto",
      maxSimultaneousStreams: 4,
      maxProfiles: 5,
      featuresSummary: ["Free streaming"],
      displayOrder: 0,
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("subscriptionPlans").doc("plus").set({
      tierCode: "plus",
      name: "Plus",
      description: "Public active monthly tier",
      isPermanentFree: false,
      isActive: true,
      isPublic: true,
      isArchived: false,
      billingInterval: "monthly",
      monthlyPriceCents: 999,
      yearlyPriceCents: 9900,
      maxQuality: "1080p",
      maxSimultaneousStreams: 2,
      maxProfiles: 5,
      featuresSummary: ["1080p streaming"],
      displayOrder: 1,
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("subscriptionPlans").doc("vip_private").set({
      tierCode: "vip",
      name: "VIP Internal",
      description: "Admin-assignable private tier",
      isPermanentFree: false,
      isActive: true,
      isPublic: false,
      isArchived: false,
      billingInterval: "yearly",
      monthlyPriceCents: 0,
      yearlyPriceCents: 19900,
      maxQuality: "4K",
      maxSimultaneousStreams: 6,
      maxProfiles: 8,
      featuresSummary: ["Admin only"],
      displayOrder: 2,
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("userSubscriptions").doc(ALICE_UID).set({
      userId: ALICE_UID,
      planId: "free",
      planName: "Free",
      status: "active",
      isPermanentFree: true,
      assignedBy: "system",
      startsAt: ts,
      expiresAt: null,
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();

  // Viewer CANNOT read private plan or modify subscription plans
  await assertFails(aliceDb.collection("subscriptionPlans").doc("vip_private").get());
  await assertFails(aliceDb.collection("subscriptionPlans").doc("plus").update({ monthlyPriceCents: 0, updatedAt: ts }));

  // Viewer CAN self-switch to an enabled, public, non-archived plan
  await assertSucceeds(
    aliceDb.collection("userSubscriptions").doc(ALICE_UID).update({
      planId: "plus",
      planName: "Plus",
      status: "active",
      isPermanentFree: false,
      assignedBy: "self",
      expiresAt: null,
      updatedAt: ts,
    })
  );

  // Viewer CANNOT self-switch to a private (isPublic == false) plan
  await assertFails(
    aliceDb.collection("userSubscriptions").doc(ALICE_UID).update({
      planId: "vip_private",
      planName: "VIP Internal",
      status: "active",
      isPermanentFree: false,
      assignedBy: "self",
      expiresAt: null,
      updatedAt: ts,
    })
  );

  // Admin CAN manually assign private plan and archive plans (hard delete denied)
  await assertSucceeds(
    adminDb.collection("userSubscriptions").doc(ALICE_UID).update({
      planId: "vip_private",
      planName: "VIP Internal",
      status: "active",
      isPermanentFree: false,
      assignedBy: ADMIN_UID,
      expiresAt: null,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    adminDb.collection("subscriptionPlans").doc("vip_private").update({
      isArchived: true,
      isActive: false,
      isPublic: false,
      updatedAt: ts,
    })
  );
  await assertFails(adminDb.collection("subscriptionPlans").doc("vip_private").delete());

  // Platform Free Access toggle: Admin/Owner can toggle; Viewer can read but cannot modify
  await assertFails(
    aliceDb.collection("platformSettings").doc("global").set({
      freeAccessEnabled: true,
      freeAccessNotice: "Hacked",
      updatedByUid: ALICE_UID,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    adminDb.collection("platformSettings").doc("global").set({
      freeAccessEnabled: true,
      freeAccessNotice: "Free access weekend enabled!",
      updatedByUid: ADMIN_UID,
      updatedAt: ts,
    })
  );
  await assertSucceeds(aliceDb.collection("platformSettings").doc("global").get());
});

test("Audit log security: append-only & readable by Owner/Admin only; zero access for Viewer/Publisher; immutable forever", async () => {
  const ts = pastTimestamp();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("owners").doc(OWNER_UID).set({
      uid: OWNER_UID,
      email: "michael.tollackson82313@gmail.com",
      establishedBy: "owner_bootstrap",
      createdAt: ts,
    });
    await db.collection("admins").doc(ADMIN_UID).set({
      uid: ADMIN_UID,
      email: "admin1@notflix.app",
      grantedBy: OWNER_UID,
      createdAt: ts,
    });
    await db.collection("users").doc(PUBLISHER_UID).set(baseUserDoc(PUBLISHER_UID, "pub1@studio.com", "publisher", "approved", ts));
  });

  const ownerDb = testEnv.authenticatedContext(OWNER_UID, { email: "michael.tollackson82313@gmail.com" }).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  const pubDb = testEnv.authenticatedContext(PUBLISHER_UID).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  const logEntry = {
    actorUid: ADMIN_UID,
    actorRole: "admin",
    action: "content_approved",
    targetContentId: "midnight-meridian",
    previousValue: "pending_review",
    newValue: "approved",
    details: "Approved submission sub_1",
    createdAt: ts,
  };

  // Viewer & Publisher CANNOT create or read audit logs
  await assertFails(aliceDb.collection("auditLogs").doc("log_bad_1").set({ ...logEntry, actorUid: ALICE_UID }));
  await assertFails(pubDb.collection("auditLogs").doc("log_bad_2").set({ ...logEntry, actorUid: PUBLISHER_UID }));
  await assertFails(aliceDb.collection("auditLogs").get());
  await assertFails(pubDb.collection("auditLogs").get());

  // Admin cannot spoof actorRole='owner'
  await assertFails(adminDb.collection("auditLogs").doc("log_spoof").set({ ...logEntry, actorRole: "owner" }));

  // Admin CAN append valid audit log and Owner/Admin can read
  await assertSucceeds(adminDb.collection("auditLogs").doc("log_1").set(logEntry));
  await assertSucceeds(adminDb.collection("auditLogs").get());
  await assertSucceeds(ownerDb.collection("auditLogs").get());

  // Audit logs are strictly immutable: NOT EVEN Owner can update or delete them
  await assertFails(adminDb.collection("auditLogs").doc("log_1").update({ details: "Tampered" }));
  await assertFails(adminDb.collection("auditLogs").doc("log_1").delete());
  await assertFails(ownerDb.collection("auditLogs").doc("log_1").update({ details: "Tampered by Owner" }));
  await assertFails(ownerDb.collection("auditLogs").doc("log_1").delete());
});


