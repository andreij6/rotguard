package com.rotguard.app.designsystem

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.rotguard.app.R
import com.rotguard.app.storage.InspirationDefaults
import java.io.File

private const val BREATHE_MIN = 0.985f
private const val BREATHE_MAX = 1.02f
private const val BREATHE_DURATION_MS = 2400
private const val ARCH_TOP_PERCENT = 50
private const val ARCH_BOTTOM_RADIUS_DP = 18
private const val PHOTO_CORNER_RADIUS_DP = 10
private const val GLOW_ALPHA = 0.16f

enum class BotanicalIllustration {
    SPROUT,
    TERRARIUM,
}

@DrawableRes
private fun BotanicalIllustration.resolveDrawable(isDark: Boolean): Int = when (this) {
    BotanicalIllustration.SPROUT -> if (isDark) R.drawable.verdant_sprout_dark else R.drawable.verdant_sprout
    BotanicalIllustration.TERRARIUM -> if (isDark) R.drawable.verdant_terrarium_dark else R.drawable.verdant_terrarium
}

@DrawableRes
fun resolveBundledPhotoDrawable(name: String?): Int? = when (name) {
    InspirationDefaults.DRAWABLE_ALPINE_SUNRISE -> R.drawable.photo_alpine_sunrise
    InspirationDefaults.DRAWABLE_CEDAR_GROVE -> R.drawable.photo_cedar_grove
    InspirationDefaults.DRAWABLE_COASTAL_HORIZON -> R.drawable.photo_coastal_horizon
    InspirationDefaults.DRAWABLE_MISTY_LAKE -> R.drawable.photo_misty_lake
    InspirationDefaults.DRAWABLE_DESERT_DUNES -> R.drawable.photo_desert_dunes
    InspirationDefaults.DRAWABLE_STARRY_MEADOW -> R.drawable.photo_starry_meadow
    InspirationDefaults.DRAWABLE_ROLLING_HILLS -> R.drawable.photo_rolling_hills
    InspirationDefaults.DRAWABLE_SNOW_PEAKS -> R.drawable.photo_snow_peaks
    InspirationDefaults.DRAWABLE_LAVENDER_FIELD -> R.drawable.photo_lavender_field
    else -> null
}

@Composable
fun BotanicalArchedMedallion(
    illustration: BotanicalIllustration,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 188.dp,
    breatheAnimation: Boolean = true,
) {
    val palette = VerdantTheme.colors
    val transition = rememberInfiniteTransition(label = "botanicalBreathe")
    val pulse by transition.animateFloat(
        initialValue = BREATHE_MIN,
        targetValue = BREATHE_MAX,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = BREATHE_DURATION_MS, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "botanicalPulse",
    )
    val scale = if (breatheAnimation) pulse else 1f
    val archShape = RoundedCornerShape(
        topStart = CornerSize(percent = ARCH_TOP_PERCENT),
        topEnd = CornerSize(percent = ARCH_TOP_PERCENT),
        bottomEnd = CornerSize(size = ARCH_BOTTOM_RADIUS_DP.dp),
        bottomStart = CornerSize(size = ARCH_BOTTOM_RADIUS_DP.dp),
    )

    Box(
        modifier = modifier.size(size = size + 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            palette.mossAccent.copy(alpha = GLOW_ALPHA),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Surface(
            modifier = Modifier
                .size(size = size)
                .scale(scale),
            shape = archShape,
            color = palette.card,
            border = BorderStroke(width = 1.dp, color = palette.cardBorder),
        ) {
            Image(
                painter = painterResource(id = illustration.resolveDrawable(palette.isDark)),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun InspirationPhotoThumbnail(
    filePath: String?,
    bundledDrawableName: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    arched: Boolean = false,
    borderColor: Color? = null,
) {
    val palette = VerdantTheme.colors
    val shape = if (arched) {
        RoundedCornerShape(
            topStart = CornerSize(percent = ARCH_TOP_PERCENT),
            topEnd = CornerSize(percent = ARCH_TOP_PERCENT),
            bottomEnd = CornerSize(size = ARCH_BOTTOM_RADIUS_DP.dp),
            bottomStart = CornerSize(size = ARCH_BOTTOM_RADIUS_DP.dp),
        )
    } else {
        RoundedCornerShape(size = PHOTO_CORNER_RADIUS_DP.dp)
    }
    val bundledRes = resolveBundledPhotoDrawable(bundledDrawableName)
    val localFile = filePath?.let(::File)?.takeIf { it.exists() }

    Surface(
        modifier = modifier.clip(shape),
        shape = shape,
        color = palette.canopyCard,
        border = BorderStroke(width = 1.dp, color = borderColor ?: palette.cardBorder),
    ) {
        when {
            localFile != null -> AsyncImage(
                model = localFile,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            bundledRes != null -> Image(
                painter = painterResource(id = bundledRes),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            else -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(palette.haloMid, palette.canopyCore),
                        ),
                    ),
            )
        }
    }
}
