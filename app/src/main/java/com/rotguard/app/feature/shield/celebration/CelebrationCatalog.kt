package com.rotguard.app.feature.shield.celebration

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.random.Random

/** Earthy, related colour families: a deep [primary] badge, a warm [accent], two spark tints and a light [ink]. */
enum class CelebrationPalette(
    val primary: Color,
    val accent: Color,
    val sparkA: Color,
    val sparkB: Color,
    val ink: Color,
) {
    MOSS_GOLD(Color(0xFF2F6F4E), Color(0xFFDFA952), Color(0xFF74C493), Color(0xFFF2D58A), Color(0xFFF6F5EE)),
    SAGE_HONEY(Color(0xFF5E8B6E), Color(0xFFE8B861), Color(0xFFA8D5B5), Color(0xFFF6D79A), Color(0xFFF7F4EA)),
    JADE_CORAL(Color(0xFF2E7D6B), Color(0xFFE8896A), Color(0xFF7FD1B9), Color(0xFFF4B9A1), Color(0xFFF7F3EE)),
    TEAL_PEACH(Color(0xFF2B6F77), Color(0xFFF2A97E), Color(0xFF7CC7CF), Color(0xFFF7D2B6), Color(0xFFF5F6F1)),
    FOREST_AMBER(Color(0xFF22553F), Color(0xFFE0952F), Color(0xFF5FAF7E), Color(0xFFF3C26B), Color(0xFFF8F3E6)),
    EMERALD_CHAMPAGNE(Color(0xFF1F7A55), Color(0xFFE9D3A3), Color(0xFF63CF9A), Color(0xFFFFF1C9), Color(0xFFFFFBF0)),
    FERN_COPPER(Color(0xFF4C7A3D), Color(0xFFC9773E), Color(0xFF9CCB78), Color(0xFFE9A76F), Color(0xFFF7F1E7)),
    EUCALYPTUS_BLUSH(Color(0xFF4F7F78), Color(0xFFE7A3A6), Color(0xFF9FCFC6), Color(0xFFF6CFD0), Color(0xFFF8F2F1)),
    PINE_LAVENDER(Color(0xFF2F5D57), Color(0xFFB8A3E0), Color(0xFF6FB0A3), Color(0xFFDCD0F5), Color(0xFFF4F2FA)),
    OLIVE_SAFFRON(Color(0xFF66733A), Color(0xFFF0B23A), Color(0xFFB4C46C), Color(0xFFF7D686), Color(0xFFF9F5E4)),
    LAGOON_SUNSET(Color(0xFF1E6E7A), Color(0xFFF08A5D), Color(0xFF64C2CF), Color(0xFFFFC28A), Color(0xFFF7F5EF)),
    MOONLIT_SILVER(Color(0xFF3D5E66), Color(0xFFCFD8DC), Color(0xFF8FB8C2), Color(0xFFE6EEF0), Color(0xFFF2F6F7)),
    ROSE_GOLD_MOSS(Color(0xFF4E6E58), Color(0xFFE3A08A), Color(0xFF9DC7A2), Color(0xFFF6C9B8), Color(0xFFF8F1EC)),
    MINT_LEMON(Color(0xFF3E8E6A), Color(0xFFEBD45E), Color(0xFFA6E3C4), Color(0xFFF6EDA0), Color(0xFFF7F8EE)),
    MAPLE_EMBER(Color(0xFF7A4A2A), Color(0xFFE07B39), Color(0xFFD9A25B), Color(0xFFF2C07A), Color(0xFFFAF1E6)),
    ;

    /** Picks a spark tint from a 0..1 seed so particles mix the palette's warm and cool notes. */
    fun spark(seed: Float): Color = when {
        seed < SPARK_A_SHARE -> sparkA
        seed < SPARK_B_SHARE -> sparkB
        else -> accent
    }

    private companion object {
        const val SPARK_A_SHARE = 0.4f
        const val SPARK_B_SHARE = 0.75f
    }
}

/** The moving layer: each reads its own meaning into the seeded [Spark] values. */
enum class CelebrationEffect(val particleCount: Int, val draw: DrawScope.(Fx) -> Unit) {
    EMBERS(30, DrawScope::drawEmbers),
    CONFETTI(42, DrawScope::drawConfetti),
    PETALS(24, DrawScope::drawPetals),
    FIREFLIES(18, DrawScope::drawFireflies),
    DANDELION(18, DrawScope::drawDandelion),
    LEAF_VORTEX(22, DrawScope::drawLeafVortex),
    STARBURST(18, DrawScope::drawStarburst),
    RIPPLES(14, DrawScope::drawRipples),
    FIREWORKS(48, DrawScope::drawFireworks),
    BUBBLES(20, DrawScope::drawBubbles),
    CONSTELLATION(12, DrawScope::drawConstellation),
    SPARKLES(26, DrawScope::drawSparkles),
    COMETS(4, DrawScope::drawComets),
    GOLDEN_RAIN(40, DrawScope::drawGoldenRain),
    GALAXY(60, DrawScope::drawGalaxy),
    SHAPE_PULSE(12, DrawScope::drawShapePulse),
    VINES(6, DrawScope::drawVines),
    MEADOW(11, DrawScope::drawMeadow),
    SNOW(30, DrawScope::drawSnow),
    SHOOTING_STARS(8, DrawScope::drawShootingStars),
}

/** The badge shape behind the emblem; [CelebrationFrame.RING] is outline-only, so its emblem uses the accent. */
enum class CelebrationFrame { CIRCLE, HEXAGON, SQUIRCLE, ROSETTE, RING }

/** The symbol that draws itself inside the badge. */
enum class CelebrationEmblem { CHECK, SPROUT, STAR, LEAF, SHIELD, SUN, FLOWER, HEART, PEAK_FLAG, MOON }

/** The soft background layer behind everything. */
enum class CelebrationAccent { AURORA, HALO, RAY_FAN, BLOOM_PETALS, MOON_GLOW }

/** How the badge arrives. */
enum class CelebrationEntrance { POP, SPIN, DROP, BLOOM, FLIP }

/** One named celebration. [name] is for previews/debugging only and is never shown to users. */
data class CelebrationVariant(
    val name: String,
    val effect: CelebrationEffect,
    val emblem: CelebrationEmblem,
    val frame: CelebrationFrame,
    val accent: CelebrationAccent,
    val entrance: CelebrationEntrance,
    val palette: CelebrationPalette,
) {
    /** Stable per-variant seed so a variant always scatters its particles the same way. */
    val seed: Int get() = name.hashCode()
}

private fun v(
    name: String,
    effect: CelebrationEffect,
    emblem: CelebrationEmblem,
    frame: CelebrationFrame,
    accent: CelebrationAccent,
    entrance: CelebrationEntrance,
    palette: CelebrationPalette,
) = CelebrationVariant(name, effect, emblem, frame, accent, entrance, palette)

private typealias E = CelebrationEffect
private typealias M = CelebrationEmblem
private typealias F = CelebrationFrame
private typealias A = CelebrationAccent
private typealias N = CelebrationEntrance
private typealias P = CelebrationPalette

/** The 50 celebrations. Same effect never repeats an emblem or a palette (enforced by CelebrationCatalogTest). */
val CELEBRATION_VARIANTS: List<CelebrationVariant> = listOf(
    v("Golden Embers", E.EMBERS, M.CHECK, F.CIRCLE, A.AURORA, N.POP, P.MOSS_GOLD),
    v("Copper Hearth", E.EMBERS, M.SUN, F.ROSETTE, A.RAY_FAN, N.BLOOM, P.FERN_COPPER),
    v("Saffron Sparks", E.EMBERS, M.PEAK_FLAG, F.HEXAGON, A.HALO, N.DROP, P.OLIVE_SAFFRON),
    v("Jade Confetti", E.CONFETTI, M.STAR, F.CIRCLE, A.HALO, N.SPIN, P.JADE_CORAL),
    v("Champagne Toast", E.CONFETTI, M.HEART, F.SQUIRCLE, A.AURORA, N.POP, P.EMERALD_CHAMPAGNE),
    v("Sunset Parade", E.CONFETTI, M.PEAK_FLAG, F.ROSETTE, A.RAY_FAN, N.DROP, P.LAGOON_SUNSET),
    v("Blush Petals", E.PETALS, M.FLOWER, F.CIRCLE, A.BLOOM_PETALS, N.BLOOM, P.EUCALYPTUS_BLUSH),
    v("Rose Garden", E.PETALS, M.HEART, F.RING, A.AURORA, N.FLIP, P.ROSE_GOLD_MOSS),
    v("Honey Blossom", E.PETALS, M.SPROUT, F.HEXAGON, A.MOON_GLOW, N.POP, P.SAGE_HONEY),
    v("Firefly Grove", E.FIREFLIES, M.LEAF, F.CIRCLE, A.MOON_GLOW, N.BLOOM, P.FOREST_AMBER),
    v("Lavender Dusk", E.FIREFLIES, M.MOON, F.RING, A.HALO, N.SPIN, P.PINE_LAVENDER),
    v("Lemon Glow", E.FIREFLIES, M.SUN, F.SQUIRCLE, A.AURORA, N.DROP, P.MINT_LEMON),
    v("Dandelion Wish", E.DANDELION, M.SPROUT, F.CIRCLE, A.MOON_GLOW, N.BLOOM, P.MOSS_GOLD),
    v("Silver Breeze", E.DANDELION, M.LEAF, F.RING, A.HALO, N.FLIP, P.MOONLIT_SILVER),
    v("Peach Drift", E.DANDELION, M.FLOWER, F.ROSETTE, A.AURORA, N.POP, P.TEAL_PEACH),
    v("Autumn Swirl", E.LEAF_VORTEX, M.LEAF, F.HEXAGON, A.RAY_FAN, N.SPIN, P.MAPLE_EMBER),
    v("Fern Spiral", E.LEAF_VORTEX, M.SPROUT, F.SQUIRCLE, A.HALO, N.BLOOM, P.FERN_COPPER),
    v("Olive Whirl", E.LEAF_VORTEX, M.CHECK, F.ROSETTE, A.AURORA, N.DROP, P.OLIVE_SAFFRON),
    v("Starburst", E.STARBURST, M.STAR, F.ROSETTE, A.RAY_FAN, N.POP, P.FOREST_AMBER),
    v("Coral Flare", E.STARBURST, M.CHECK, F.HEXAGON, A.AURORA, N.SPIN, P.JADE_CORAL),
    v("Daybreak", E.STARBURST, M.SUN, F.CIRCLE, A.BLOOM_PETALS, N.BLOOM, P.LAGOON_SUNSET),
    v("Still Pond", E.RIPPLES, M.LEAF, F.CIRCLE, A.MOON_GLOW, N.BLOOM, P.TEAL_PEACH),
    v("Moon Pool", E.RIPPLES, M.MOON, F.RING, A.HALO, N.FLIP, P.MOONLIT_SILVER),
    v("Mint Ripple", E.RIPPLES, M.CHECK, F.SQUIRCLE, A.AURORA, N.POP, P.MINT_LEMON),
    v("Festival Night", E.FIREWORKS, M.STAR, F.CIRCLE, A.MOON_GLOW, N.DROP, P.PINE_LAVENDER),
    v("Summit Fireworks", E.FIREWORKS, M.PEAK_FLAG, F.SQUIRCLE, A.HALO, N.POP, P.MAPLE_EMBER),
    v("Gilded Fireworks", E.FIREWORKS, M.SHIELD, F.HEXAGON, A.RAY_FAN, N.SPIN, P.EMERALD_CHAMPAGNE),
    v("Sea Glass Bubbles", E.BUBBLES, M.CHECK, F.CIRCLE, A.AURORA, N.BLOOM, P.TEAL_PEACH),
    v("Fizz", E.BUBBLES, M.HEART, F.ROSETTE, A.HALO, N.POP, P.MINT_LEMON),
    v("Constellation", E.CONSTELLATION, M.STAR, F.RING, A.MOON_GLOW, N.FLIP, P.MOONLIT_SILVER),
    v("Night Garden", E.CONSTELLATION, M.MOON, F.CIRCLE, A.HALO, N.BLOOM, P.PINE_LAVENDER),
    v("Guiding Stars", E.CONSTELLATION, M.SHIELD, F.HEXAGON, A.AURORA, N.SPIN, P.MOSS_GOLD),
    v("Twinkle", E.SPARKLES, M.STAR, F.SQUIRCLE, A.AURORA, N.POP, P.SAGE_HONEY),
    v("Rose Sparkle", E.SPARKLES, M.FLOWER, F.CIRCLE, A.BLOOM_PETALS, N.SPIN, P.ROSE_GOLD_MOSS),
    v("Saffron Glint", E.SPARKLES, M.SHIELD, F.ROSETTE, A.RAY_FAN, N.DROP, P.OLIVE_SAFFRON),
    v("Comet Orbit", E.COMETS, M.CHECK, F.RING, A.HALO, N.SPIN, P.EMERALD_CHAMPAGNE),
    v("Planet Dance", E.COMETS, M.SUN, F.CIRCLE, A.MOON_GLOW, N.POP, P.FOREST_AMBER),
    v("Golden Rain", E.GOLDEN_RAIN, M.SHIELD, F.CIRCLE, A.AURORA, N.DROP, P.MOSS_GOLD),
    v("Copper Shower", E.GOLDEN_RAIN, M.PEAK_FLAG, F.RING, A.RAY_FAN, N.FLIP, P.FERN_COPPER),
    v("Galaxy", E.GALAXY, M.STAR, F.HEXAGON, A.MOON_GLOW, N.SPIN, P.MOONLIT_SILVER),
    v("Coral Spiral", E.GALAXY, M.FLOWER, F.ROSETTE, A.AURORA, N.BLOOM, P.JADE_CORAL),
    v("Hex Pulse", E.SHAPE_PULSE, M.SHIELD, F.HEXAGON, A.HALO, N.POP, P.FOREST_AMBER),
    v("Heartbeat", E.SHAPE_PULSE, M.HEART, F.ROSETTE, A.AURORA, N.BLOOM, P.EUCALYPTUS_BLUSH),
    v("Climbing Vines", E.VINES, M.SPROUT, F.CIRCLE, A.BLOOM_PETALS, N.BLOOM, P.MOSS_GOLD),
    v("Wild Ivy", E.VINES, M.LEAF, F.ROSETTE, A.HALO, N.DROP, P.SAGE_HONEY),
    v("Meadow", E.MEADOW, M.SPROUT, F.SQUIRCLE, A.RAY_FAN, N.POP, P.MINT_LEMON),
    v("Sunny Field", E.MEADOW, M.SUN, F.ROSETTE, A.AURORA, N.DROP, P.OLIVE_SAFFRON),
    v("First Frost", E.SNOW, M.MOON, F.CIRCLE, A.MOON_GLOW, N.FLIP, P.MOONLIT_SILVER),
    v("Blush Flurry", E.SNOW, M.HEART, F.HEXAGON, A.HALO, N.BLOOM, P.EUCALYPTUS_BLUSH),
    v("Shooting Stars", E.SHOOTING_STARS, M.STAR, F.CIRCLE, A.MOON_GLOW, N.SPIN, P.PINE_LAVENDER),
)

/**
 * Shuffle bag over the catalog: every variant plays once before any repeats, and a refill never starts
 * with the one just shown. Pure (inject [random]) so it is unit-tested.
 */
class CelebrationDeck(
    private val size: Int = CELEBRATION_VARIANTS.size,
    private val random: Random = Random.Default,
) {
    private val bag = ArrayDeque<Int>()
    private var lastDrawn = -1

    fun draw(): Int {
        if (bag.isEmpty()) refill()
        return bag.removeFirst().also { lastDrawn = it }
    }

    private fun refill() {
        val order = (0 until size).shuffled(random).toMutableList()
        if (size > 1 && order.first() == lastDrawn) {
            order[0] = order[order.lastIndex].also { order[order.lastIndex] = order[0] }
        }
        bag.addAll(order)
    }
}
