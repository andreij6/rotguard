package com.rotguard.app.feature.shield.celebration

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val CELL_SCALE = 0.42f
private val FILMSTRIP_SECONDS = listOf(0.15f, 0.35f, 0.55f, 0.75f, 1.0f, 1.3f)

/**
 * Debug-only contact sheet: every celebration frozen at `--ef t <seconds>` (default 0.7), on the shield's
 * canopy background, so the 50 can be compared side by side.
 */
class CelebrationGalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val seconds = intent.getFloatExtra("t", 0.7f)
        // `--ei v <n>` (1-based): one variant across its timeline instead of all 50 at one moment.
        val single = intent.getIntExtra("v", 0)
        val cells = if (single in 1..CELEBRATION_VARIANTS.size) {
            FILMSTRIP_SECONDS.map { CELEBRATION_VARIANTS[single - 1] to it }
        } else {
            CELEBRATION_VARIANTS.map { it to seconds }
        }
        setContent {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize().background(Color(0xFF1C352B)).systemBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                itemsIndexed(cells) { index, (variant, at) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(118.dp), contentAlignment = Alignment.Center) {
                            Canvas(
                                modifier = Modifier
                                    .requiredSize(280.dp)
                                    .graphicsLayer(scaleX = CELL_SCALE, scaleY = CELL_SCALE),
                            ) { drawCelebration(variant, seededSparks(variant), at) }
                        }
                        val label = if (single > 0) "t = ${at}s" else "${index + 1}. ${variant.name}"
                        Text(label, color = Color(0xFFEEF3EB), fontSize = 10.sp)
                        Text(
                            "${variant.effect.name.lowercase()} · ${variant.emblem.name.lowercase()}",
                            color = Color(0xFF94AFA2),
                            fontSize = 8.sp,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                }
            }
        }
    }
}
