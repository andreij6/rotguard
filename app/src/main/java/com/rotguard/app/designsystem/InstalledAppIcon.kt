package com.rotguard.app.designsystem

import android.content.pm.PackageManager
import android.util.LruCache
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.rotguard.app.R
import com.rotguard.app.storage.AppPackageName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MAX_CACHED_ICONS = 64
private const val ICON_CORNER_RADIUS_DP = 8

private object AppIconMemoryCache {
    private val cache = LruCache<String, ImageBitmap>(MAX_CACHED_ICONS)

    fun get(packageName: String): ImageBitmap? = synchronized(cache) {
        cache.get(packageName)
    }

    fun put(packageName: String, bitmap: ImageBitmap) {
        synchronized(cache) {
            cache.put(packageName, bitmap)
        }
    }
}

@Composable
fun InstalledAppIcon(
    packageName: AppPackageName,
    displayName: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val context = LocalContext.current
    val palette = VerdantTheme.colors
    val iconPx = with(LocalDensity.current) { size.roundToPx().coerceAtLeast(1) }
    val cached = AppIconMemoryCache.get(packageName.value)

    val bitmap by produceState(initialValue = cached, key1 = packageName.value, key2 = iconPx) {
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                try {
                    val drawable = context.packageManager.getApplicationIcon(packageName.value)
                    val rendered = drawable.toBitmap(width = iconPx, height = iconPx).asImageBitmap()
                    AppIconMemoryCache.put(packageName.value, rendered)
                    rendered
                } catch (_: PackageManager.NameNotFoundException) {
                    null
                } catch (_: IllegalArgumentException) {
                    null
                }
            }
        }
    }

    val shape = RoundedCornerShape(size = ICON_CORNER_RADIUS_DP.dp)
    val loadedBitmap = bitmap
    if (loadedBitmap != null) {
        Image(
            bitmap = loadedBitmap,
            contentDescription = stringResource(R.string.apps_icon_description, displayName),
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size = size)
                .clip(shape),
        )
    } else {
        Surface(
            modifier = modifier.size(size = size),
            shape = shape,
            color = palette.sageTint,
            border = BorderStroke(width = 1.dp, color = palette.cardBorder),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = displayName.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = palette.ink,
                )
            }
        }
    }
}
