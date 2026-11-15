package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.storage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

enum class MediaAssetCategory(val folder: String, val label: String) {
    POSTER("posters", "Poster"),
    BACKDROP("backdrops", "Backdrop"),
    TRAILER("trailers", "Trailer"),
    VIDEO("videos", "Video File"),
    EPISODE("episodes", "Episode Video")
}

sealed interface MediaUploadState {
    data object Idle : MediaUploadState
    data class Uploading(
        val progressFraction: Float,
        val bytesTransferred: Long,
        val totalBytes: Long,
        val label: String
    ) : MediaUploadState

    data class Success(
        val downloadUrl: String,
        val storagePath: String,
        val fileName: String,
        val mimeType: String?
    ) : MediaUploadState

    data class Error(val message: String) : MediaUploadState
}

class MediaUploadRepository(
    private val context: Context,
    private val storage: FirebaseStorage = Firebase.storage,
    private val auth: FirebaseAuth = Firebase.auth
) {
    fun uploadMedia(
        uri: Uri,
        category: MediaAssetCategory,
        contentFolderHint: String
    ): Flow<MediaUploadState> = callbackFlow {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            trySend(MediaUploadState.Error("You must be signed in as an administrator to upload media."))
            close()
            return@callbackFlow
        }

        val mimeType = context.contentResolver.getType(uri)
        val rawFileName = resolveFileName(uri, category, mimeType)
        val sanitizedFileName = rawFileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val folderSlug = contentFolderHint
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "catalog-item" }
            .take(48)

        val storagePath = "catalog/$folderSlug/${category.folder}/${System.currentTimeMillis()}_$sanitizedFileName"
        val ref = storage.reference.child(storagePath)

        val metadata = StorageMetadata.Builder()
            .apply {
                if (!mimeType.isNullOrBlank()) setContentType(mimeType)
            }
            .setCustomMetadata("uploadedBy", currentUser.uid)
            .setCustomMetadata("category", category.folder)
            .build()

        trySend(
            MediaUploadState.Uploading(
                progressFraction = 0f,
                bytesTransferred = 0L,
                totalBytes = 0L,
                label = "Preparing ${category.label} upload..."
            )
        )

        val uploadTask = ref.putFile(uri, metadata)

        uploadTask.addOnProgressListener { snapshot ->
            val total = snapshot.totalByteCount
            val transferred = snapshot.bytesTransferred
            val fraction = if (total > 0L) (transferred.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
            trySend(
                MediaUploadState.Uploading(
                    progressFraction = fraction,
                    bytesTransferred = transferred,
                    totalBytes = total,
                    label = "Uploading ${category.label}..."
                )
            )
        }

        uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                throw task.exception ?: IllegalStateException("Upload failed")
            }
            ref.downloadUrl
        }.addOnSuccessListener { downloadUri ->
            trySend(
                MediaUploadState.Success(
                    downloadUrl = downloadUri.toString(),
                    storagePath = storagePath,
                    fileName = sanitizedFileName,
                    mimeType = mimeType
                )
            )
            close()
        }.addOnFailureListener { error ->
            trySend(MediaUploadState.Error(mapStorageError(error)))
            close()
        }

        awaitClose {
            if (uploadTask.isInProgress) {
                uploadTask.cancel()
            }
        }
    }

    private fun resolveFileName(uri: Uri, category: MediaAssetCategory, mimeType: String?): String {
        val queriedName = runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else null
            }
        }.getOrNull()

        if (!queriedName.isNullOrBlank()) return queriedName

        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            ?: when (category) {
                MediaAssetCategory.POSTER, MediaAssetCategory.BACKDROP -> "jpg"
                else -> "mp4"
            }
        return "${category.folder}_${System.currentTimeMillis()}.$ext"
    }

    private fun mapStorageError(error: Throwable): String {
        val storageEx = generateSequence(error) { it.cause }
            .filterIsInstance<StorageException>()
            .firstOrNull()
        return when (storageEx?.errorCode) {
            StorageException.ERROR_NOT_AUTHORIZED ->
                "Upload blocked by Firebase Storage rules. Verify admin permissions and Storage rules in Firebase Console."
            StorageException.ERROR_OBJECT_NOT_FOUND,
            StorageException.ERROR_BUCKET_NOT_FOUND,
            StorageException.ERROR_PROJECT_NOT_FOUND ->
                "Firebase Storage bucket is not enabled yet. Open Firebase Console -> Build -> Storage -> Get started, or paste an HTTPS media URL."
            StorageException.ERROR_QUOTA_EXCEEDED ->
                "Firebase Storage quota exceeded."
            StorageException.ERROR_CANCELED ->
                "Upload was canceled."
            else -> error.localizedMessage
                ?: "Media upload failed. Ensure Firebase Storage is enabled in Firebase Console or paste an HTTPS URL."
        }
    }
}
