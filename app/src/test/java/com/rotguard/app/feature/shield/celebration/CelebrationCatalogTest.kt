package com.rotguard.app.feature.shield.celebration

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CelebrationCatalogTest {

    @Test
    fun `catalog has 50 uniquely named variants`() {
        assertEquals(50, CELEBRATION_VARIANTS.size)
        assertEquals(50, CELEBRATION_VARIANTS.map { it.name }.toSet().size)
    }

    @Test
    fun `no two variants share both effect and emblem`() {
        val pairs = CELEBRATION_VARIANTS.map { it.effect to it.emblem }
        assertEquals(pairs.size, pairs.toSet().size)
    }

    @Test
    fun `variants sharing an effect never share a palette`() {
        CELEBRATION_VARIANTS.groupBy { it.effect }.forEach { (effect, group) ->
            assertEquals("palette repeated within $effect", group.size, group.map { it.palette }.toSet().size)
        }
    }

    @Test
    fun `every effect, emblem, frame, accent, entrance and palette is used`() {
        assertEquals(CelebrationEffect.entries.toSet(), CELEBRATION_VARIANTS.map { it.effect }.toSet())
        assertEquals(CelebrationEmblem.entries.toSet(), CELEBRATION_VARIANTS.map { it.emblem }.toSet())
        assertEquals(CelebrationFrame.entries.toSet(), CELEBRATION_VARIANTS.map { it.frame }.toSet())
        assertEquals(CelebrationAccent.entries.toSet(), CELEBRATION_VARIANTS.map { it.accent }.toSet())
        assertEquals(CelebrationEntrance.entries.toSet(), CELEBRATION_VARIANTS.map { it.entrance }.toSet())
        assertEquals(CelebrationPalette.entries.toSet(), CELEBRATION_VARIANTS.map { it.palette }.toSet())
    }

    @Test
    fun `deck plays all 50 before any repeat`() {
        val deck = CelebrationDeck(random = Random(7))
        repeat(3) {
            val round = List(50) { deck.draw() }
            assertEquals((0 until 50).toSet(), round.toSet())
        }
    }

    @Test
    fun `deck never shows the same variant twice in a row, even across refills`() {
        for (seed in 0 until 200) {
            val deck = CelebrationDeck(size = 5, random = Random(seed))
            val draws = List(40) { deck.draw() }
            draws.zipWithNext().forEach { (a, b) -> assertNotEquals("seed $seed", a, b) }
        }
    }

    @Test
    fun `sparks are deterministic per variant`() {
        CELEBRATION_VARIANTS.forEach { variant ->
            val sparks = seededSparks(variant)
            assertEquals(variant.effect.particleCount, sparks.size)
            assertEquals(sparks, seededSparks(variant))
            assertTrue(sparks.all { it.a in 0f..1f && it.d in 0f..1f })
        }
    }
}
