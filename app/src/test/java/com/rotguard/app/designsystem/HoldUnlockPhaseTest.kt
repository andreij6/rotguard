package com.rotguard.app.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HoldUnlockPhaseTest {

    @Test
    fun `no lock means unlocked from the start`() {
        assertEquals(HoldUnlockPhase.UNLOCKED, HoldUnlockPhase.at(elapsedMillis = 0L, lockMillis = 0L))
    }

    @Test
    fun `locked with a rounded-up countdown until the delay ends`() {
        val start = HoldUnlockPhase.at(elapsedMillis = 0L, lockMillis = 1_000L)
        assertTrue(start.isLocked)
        assertEquals(10, start.countdownTenths)
        assertEquals(0f, start.unlockFraction)

        val nearlyDone = HoldUnlockPhase.at(elapsedMillis = 950L, lockMillis = 1_000L)
        assertTrue(nearlyDone.isLocked)
        assertEquals(1, nearlyDone.countdownTenths)
    }

    @Test
    fun `unlocks exactly at the delay then animates to fully open`() {
        val unlocked = HoldUnlockPhase.at(elapsedMillis = 1_000L, lockMillis = 1_000L)
        assertFalse(unlocked.isLocked)
        assertEquals(0, unlocked.countdownTenths)
        assertEquals(0f, unlocked.unlockFraction)

        val halfway = HoldUnlockPhase.at(1_000L + HoldUnlockPhase.UNLOCK_ANIMATION_MILLIS / 2, lockMillis = 1_000L)
        assertEquals(0.5f, halfway.unlockFraction, 0.01f)

        val done = HoldUnlockPhase.at(elapsedMillis = 5_000L, lockMillis = 1_000L)
        assertEquals(1f, done.unlockFraction)
    }
}
