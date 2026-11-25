package com.rotguard.app.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface InspirationPhotoFileStore {
    suspend fun copyUriToPrivateStorage(uriString: String): String
    suspend fun deletePrivatePhotoFile(filePath: String?)
}

@Singleton
class AndroidInspirationPhotoFileStore @Inject constructor(
    @ApplicationContext private val context: Context,
) : InspirationPhotoFileStore {

    override suspend fun copyUriToPrivateStorage(uriString: String): String = withContext(Dispatchers.IO) {
        try {
            importPhoto(uriString)
        } catch (failure: IOException) {
            // The UI only shows a generic "couldn't add photo"; keep the real cause in logcat.
            Log.w(LOG_TAG, "Inspiration photo import failed for $uriString", failure)
            throw failure
        }
    }

    private fun importPhoto(uriString: String): String {
        val bitmap = try {
            val uri = uriString.toUri()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                decodeWithImageDecoder(uri)
            } else {
                decodeWithBitmapFactory(uri)
            }
        } catch (revokedGrant: SecurityException) {
            throw IOException("Permission denied while reading photo.", revokedGrant)
        }
        val targetDir = File(context.filesDir, INSPIRATION_DIR).apply { mkdirs() }
        val destination = File(targetDir, "photo_${UUID.randomUUID()}.jpg")
        try {
            FileOutputStream(destination).use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) { "JPEG compress failed" }
            }
        } catch (compressFailed: IllegalStateException) {
            throw IOException("Unable to save photo locally.", compressFailed)
        } finally {
            bitmap.recycle()
        }
        return destination.absolutePath
    }

    /** Android 9+: applies the camera's EXIF rotation (portrait shots stay upright) and reads HEIC. */
    @RequiresApi(Build.VERSION_CODES.P)
    private fun decodeWithImageDecoder(uri: Uri): Bitmap {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.setTargetSampleSize(computeInSampleSize(info.size.width, info.size.height))
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE // compress() needs a software bitmap
        }
    }

    // ponytail: ignores EXIF rotation on Android 8.x (API 26-27); add androidx.exifinterface if those users complain.
    private fun decodeWithBitmapFactory(uri: Uri): Bitmap {
        // With inJustDecodeBounds, decodeStream always returns null by design: only a null *stream* means failure.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openPhotoStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        val options = BitmapFactory.Options().apply {
            inSampleSize = computeInSampleSize(bounds.outWidth, bounds.outHeight)
        }
        return openPhotoStream(uri).use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw IOException("Unable to decode selected image.")
    }

    private fun openPhotoStream(uri: Uri): InputStream =
        context.contentResolver.openInputStream(uri) ?: throw IOException("Unable to open selected photo.")

    override suspend fun deletePrivatePhotoFile(filePath: String?): Unit = withContext(Dispatchers.IO) {
        if (filePath.isNullOrBlank()) return@withContext
        try {
            val rootDir = File(context.filesDir, INSPIRATION_DIR).canonicalFile
            val candidate = File(filePath).canonicalFile
            if (candidate.path.startsWith(rootDir.path) && candidate.exists()) {
                candidate.delete()
            }
        } catch (_: IOException) {
            // Best-effort local image cleanup.
        } catch (_: SecurityException) {
            // Best-effort local image cleanup.
        }
    }

    private fun computeInSampleSize(width: Int, height: Int): Int {
        if (width <= 0 || height <= 0) return 1
        var sample = 1
        while (width / sample > MAX_DIMENSION_PX || height / sample > MAX_DIMENSION_PX) {
            sample *= SAMPLE_FACTOR
        }
        return sample
    }

    private companion object {
        const val INSPIRATION_DIR = "inspiration"
        const val LOG_TAG = "RotGuardPhotos"
        const val MAX_DIMENSION_PX = 1280
        const val JPEG_QUALITY = 88
        const val SAMPLE_FACTOR = 2
    }
}
