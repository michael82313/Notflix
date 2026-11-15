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

test("Email spoofing test: unverified admin email cannot write catalog", async () => {
  const ts = pastTimestamp();
  const spoofDb = testEnv.authenticatedContext("spoof_uid", {
    email: "michael.tollackson82313@gmail.com",
    email_verified: false,
  }).firestore();

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

  await assertFails(unauthDb.collection("subscriptionPlans").where("isActive", "==", true).get());
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

  // Member can query active plans, cannot view inactive future plan directly
  await assertSucceeds(aliceDb.collection("subscriptionPlans").where("isActive", "==", true).get());
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
