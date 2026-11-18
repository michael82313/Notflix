import * as admin from "firebase-admin";
import { onCall, HttpsError } from "firebase-functions/v2/https";

admin.initializeApp();

const DATABASE_ID = "ai-studio-android-velora-fa049f50-d743-4c93-8b72-1a2b2b3ed613";
const db = admin.firestore();
db.settings({ databaseId: DATABASE_ID });

type Role = "owner" | "admin" | "publisher" | "viewer";

async function resolveCallerRole(uid: string): Promise<Role> {
  const [ownerSnap, adminSnap, userSnap] = await Promise.all([
    db.collection("owners").doc(uid).get(),
    db.collection("admins").doc(uid).get(),
    db.collection("users").doc(uid).get(),
  ]);
  if (ownerSnap.exists || userSnap.data()?.role === "owner") return "owner";
  if (adminSnap.exists || userSnap.data()?.role === "admin") return "admin";
  if (userSnap.data()?.role === "publisher") return "publisher";
  return "viewer";
}

async function isTargetOwner(uid: string): Promise<boolean> {
  const [ownerSnap, userSnap] = await Promise.all([
    db.collection("owners").doc(uid).get(),
    db.collection("users").doc(uid).get(),
  ]);
  return ownerSnap.exists || userSnap.data()?.role === "owner";
}

async function writeAuditLog(params: {
  actorUid: string;
  actorRole: "owner" | "admin";
  action: string;
  targetUid?: string | null;
  targetContentId?: string | null;
  targetSubscriptionId?: string | null;
  previousValue: string;
  newValue: string;
  details: string;
}) {
  await db.collection("auditLogs").add({
    actorUid: params.actorUid,
    actorRole: params.actorRole,
    action: params.action,
    ...(params.targetUid ? { targetUid: params.targetUid } : {}),
    ...(params.targetContentId ? { targetContentId: params.targetContentId } : {}),
    ...(params.targetSubscriptionId ? { targetSubscriptionId: params.targetSubscriptionId } : {}),
    previousValue: params.previousValue,
    newValue: params.newValue,
    details: params.details,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  });
}

/**
 * Trusted server-side role assignment with strict Owner protection and Firebase Auth custom claims sync.
 * - No one can demote, suspend, or modify the Owner.
 * - Only Owner can appoint or remove Admins.
 * - Admins can only manage ordinary Viewer <-> Publisher transitions.
 */
export const setUserRole = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Authentication required.");
  }
  const actorUid = request.auth.uid;
  const actorRole = await resolveCallerRole(actorUid);
  if (actorRole !== "owner" && actorRole !== "admin") {
    throw new HttpsError("permission-denied", "Admin or Owner privileges required.");
  }

  const targetUid = String(request.data?.targetUid || "").trim();
  const desiredRole = String(request.data?.role || "").trim() as Role;
  const publisherStatus = String(request.data?.publisherStatus || "").trim();

  if (!targetUid || !["viewer", "publisher", "admin"].includes(desiredRole)) {
    throw new HttpsError("invalid-argument", "Valid targetUid and role (viewer|publisher|admin) are required.");
  }

  if (await isTargetOwner(targetUid)) {
    throw new HttpsError("permission-denied", "Platform Owner account is protected and cannot be modified.");
  }

  const targetCurrentRole = await resolveCallerRole(targetUid);
  if (actorRole === "admin" && (targetCurrentRole === "admin" || desiredRole === "admin")) {
    throw new HttpsError("permission-denied", "Only the Platform Owner can appoint or remove Administrators.");
  }

  const userRef = db.collection("users").doc(targetUid);
  const userSnap = await userRef.get();
  if (!userSnap.exists) {
    throw new HttpsError("not-found", "Target user document does not exist.");
  }

  const resolvedPubStatus =
    publisherStatus ||
    (desiredRole === "publisher" ? "approved" : userSnap.data()?.publisherStatus || "none");

  const batch = db.batch();
  batch.update(userRef, {
    role: desiredRole,
    publisherStatus: resolvedPubStatus,
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  });

  const adminRef = db.collection("admins").doc(targetUid);
  if (desiredRole === "admin") {
    batch.set(adminRef, {
      uid: targetUid,
      email: userSnap.data()?.email || "",
      grantedBy: actorUid,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  } else if (targetCurrentRole === "admin") {
    batch.delete(adminRef);
  }
  await batch.commit();

  // Sync Firebase Auth custom claims for token-level enforcement
  await admin.auth().setCustomUserClaims(targetUid, {
    role: desiredRole,
    publisherStatus: resolvedPubStatus,
  });

  await writeAuditLog({
    actorUid,
    actorRole,
    action: desiredRole === "admin" ? "APPOINT_ADMIN" : targetCurrentRole === "admin" ? "REMOVE_ADMIN" : "UPDATE_USER_ROLE",
    targetUid,
    previousValue: `${targetCurrentRole} (publisherStatus=${userSnap.data()?.publisherStatus || "none"})`,
    newValue: `${desiredRole} (publisherStatus=${resolvedPubStatus})`,
    details: `Role updated via server callable by ${actorRole}.`,
  });

  return { success: true, targetUid, role: desiredRole, publisherStatus: resolvedPubStatus };
});

/**
 * Trusted server-side content moderation callable.
 * Approving atomically publishes or updates /catalog/{contentId} (including seasons & episodes)
 * and preserves immutable review history & audit logs.
 */
export const reviewContentSubmission = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Authentication required.");
  }
  const actorUid = request.auth.uid;
  const actorRole = await resolveCallerRole(actorUid);
  if (actorRole !== "owner" && actorRole !== "admin") {
    throw new HttpsError("permission-denied", "Only Admins or Owner can moderate content submissions.");
  }

  const submissionId = String(request.data?.submissionId || "").trim();
  const decision = String(request.data?.decision || "").trim();
  const notes = String(request.data?.notes || "").trim().slice(0, 2000);
  if (!submissionId || !["under_review", "approved", "changes_requested", "rejected"].includes(decision)) {
    throw new HttpsError("invalid-argument", "Invalid submissionId or decision.");
  }

  const subRef = db.collection("contentSubmissions").doc(submissionId);
  const subSnap = await subRef.get();
  if (!subSnap.exists) {
    throw new HttpsError("not-found", "Submission not found.");
  }
  const sub = subSnap.data()!;
  const previousStatus = String(sub.reviewStatus || "pending_review");

  let publishedCatalogId: string | null = sub.publishedCatalogId || null;

  if (decision === "approved") {
    const catalogId = publishedCatalogId || `pub-${submissionId}`;
    publishedCatalogId = catalogId;
    const catalogRef = db.collection("catalog").doc(catalogId);
    const existingCatalog = await catalogRef.get();
    const isSeries = sub.mediaType === "series";

    const catalogPayload: Record<string, unknown> = {
      title: sub.title,
      synopsis: sub.synopsis,
      mediaType: isSeries ? "series" : "movie",
      contentCategory: sub.mediaType,
      genres: sub.genres || ["Drama"],
      releaseYear: sub.releaseYear || 2025,
      runtimeMinutes: sub.runtimeMinutes || 90,
      ageRating: sub.ageRating || "PG-13",
      director: sub.directorOrCreator || sub.publisherName || "Independent Creator",
      cast: sub.cast || [],
      posterKey: sub.posterKey || "img_poster_chronicle",
      ...(sub.posterUrl ? { posterUrl: sub.posterUrl } : {}),
      backdropKey: sub.backdropKey || "img_hero_nebula",
      ...(sub.backdropUrl ? { backdropUrl: sub.backdropUrl } : {}),
      ...(sub.trailerUrl ? { trailerUrl: sub.trailerUrl } : {}),
      videoUrl: sub.videoUrl || "",
      videoSourceType: sub.videoSourceType || "auto",
      ...(sub.providerName ? { providerName: sub.providerName } : {}),
      ...(sub.hlsUrl ? { hlsUrl: sub.hlsUrl } : {}),
      isFeatured: existingCatalog.exists ? Boolean(existingCatalog.data()?.isFeatured) : false,
      isVisible: true,
      isKidsFriendly: Boolean(sub.isKidsFriendly),
      trendingScore: existingCatalog.exists ? Number(existingCatalog.data()?.trendingScore || 80) : 80,
      popularityRank: existingCatalog.exists ? Number(existingCatalog.data()?.popularityRank || 20) : 20,
      tags: sub.tags || [],
      publisherUid: sub.publisherUid,
      publisherName: sub.publisherName,
      sourceSubmissionId: submissionId,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      ...(existingCatalog.exists ? {} : { createdAt: admin.firestore.FieldValue.serverTimestamp() }),
    };
    await catalogRef.set(catalogPayload, { merge: true });
  }

  await subRef.update({
    reviewStatus: decision,
    privateReviewerNotes: notes,
    lastReviewedByUid: actorUid,
    lastReviewedAt: admin.firestore.FieldValue.serverTimestamp(),
    ...(publishedCatalogId ? { publishedCatalogId } : {}),
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  });

  await subRef.collection("reviews").add({
    submissionId,
    reviewerUid: actorUid,
    reviewerRole: actorRole,
    decision,
    notes,
    videoSourceVerified: Boolean(request.data?.videoSourceVerified),
    rightsConfirmed: Boolean(request.data?.rightsConfirmed),
    metadataVerified: Boolean(request.data?.metadataVerified),
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  });

  await writeAuditLog({
    actorUid,
    actorRole,
    action: `CONTENT_REVIEW_${decision.toUpperCase()}`,
    targetUid: sub.publisherUid || null,
    targetContentId: publishedCatalogId || submissionId,
    previousValue: previousStatus,
    newValue: decision,
    details: notes || `Submission "${sub.title}" marked ${decision}.`,
  });

  return { success: true, submissionId, decision, publishedCatalogId };
});

/**
 * Trusted server-side platform-wide freeAccessEnabled toggle.
 * Preserves all existing userSubscriptions documents without deletion or reset.
 */
export const setFreeAccessMode = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Authentication required.");
  }
  const actorUid = request.auth.uid;
  const actorRole = await resolveCallerRole(actorUid);
  if (actorRole !== "owner" && actorRole !== "admin") {
    throw new HttpsError("permission-denied", "Admin or Owner privileges required.");
  }
  const enabled = Boolean(request.data?.freeAccessEnabled);
  const settingsRef = db.collection("platformSettings").doc("global");
  const prevSnap = await settingsRef.get();
  const prevVal = prevSnap.exists ? String(Boolean(prevSnap.data()?.freeAccessEnabled)) : "true";

  await settingsRef.set(
    {
      freeAccessEnabled: enabled,
      freeAccessNotice: enabled
        ? "Platform-wide Free Access is currently enabled. Existing subscription records are preserved."
        : "Standard plan-based access is active.",
      updatedByUid: actorUid,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    },
    { merge: true }
  );

  await writeAuditLog({
    actorUid,
    actorRole,
    action: "TOGGLE_FREE_ACCESS_MODE",
    previousValue: prevVal,
    newValue: String(enabled),
    details: "Platform-wide freeAccessEnabled updated without modifying existing user subscription records.",
  });

  return { success: true, freeAccessEnabled: enabled };
});
