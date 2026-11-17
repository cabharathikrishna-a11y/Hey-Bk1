package com.example.system

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import com.example.R
import com.example.domain.model.MediaAttachment
import com.example.domain.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class MediaStorageHelper(private val context: Context) {

    suspend fun copyIntoAppStorage(sourceUri: Uri, type: MediaType): MediaAttachment? =
        withContext(Dispatchers.IO) {
            runCatching {
                val mediaDir = File(context.filesDir, "media").apply { mkdirs() }
                val displayName = queryDisplayName(sourceUri)
                    ?: "${type.name.lowercase()}_${System.currentTimeMillis()}"
                val ext = displayName.substringAfterLast('.', missingDelimiterValue = when (type) {
                    MediaType.PHOTO -> "jpg"
                    MediaType.VIDEO -> "mp4"
                    MediaType.AUDIO -> "m4a"
                    MediaType.DOCUMENT -> "bin"
                })
                val id = "med_${UUID.randomUUID()}"
                val destFile = File(mediaDir, "$id.$ext")
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: return@withContext null

                MediaAttachment(
                    id = id,
                    entryId = "",
                    type = type,
                    localUri = Uri.fromFile(destFile).toString(),
                    displayName = displayName,
                    durationMs = 0L,
                    createdAt = System.currentTimeMillis()
                )
            }.getOrNull()
        }

    fun createCameraOutputUri(isVideo: Boolean): Pair<Uri, File> {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val ext = if (isVideo) "mp4" else "jpg"
        val file = File(dir, "cap_${System.currentTimeMillis()}.$ext")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return uri to file
    }

    fun createAudioRecordingFile(): File {
        val mediaDir = File(context.filesDir, "media").apply { mkdirs() }
        return File(mediaDir, "voice_${System.currentTimeMillis()}.m4a")
    }

    fun resolveContactSelection(contactUri: Uri): Pair<String, String?>? {
        return runCatching {
            val projection = arrayOf(
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.LOOKUP_KEY
            )
            context.contentResolver.query(contactUri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx) else null
                    if (!name.isNullOrBlank()) {
                        return@runCatching name to contactUri.toString()
                    }
                }
            }
            null
        }.getOrNull()
    }

    fun pinEntryToHomeScreen(entryId: String, title: String): Boolean {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) return false
        val shortcutIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(EXTRA_ENTRY_ID, entryId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val shortcut = ShortcutInfoCompat.Builder(context, "entry_$entryId")
            .setShortLabel(title.take(22).ifBlank { "Hey Bk Entry" })
            .setLongLabel(title.take(50).ifBlank { "Hey Bk Journal Entry" })
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(shortcutIntent)
            .build()
        return ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
    }

    private fun queryDisplayName(uri: Uri): String? {
        if (uri.scheme == "file") return uri.lastPathSegment
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) cursor.getString(idx) else null
                    } else null
                }
        }.getOrNull()
    }

    companion object {
        const val EXTRA_ENTRY_ID = "shortcut_entry_id"
    }
}
