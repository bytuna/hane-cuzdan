package com.hanecuzdan.app.core.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHasherTest {

    @Test
    fun `ayni parola icin uretilen tuzlu hash dogrulanmali`() {
        val hash = PasswordHasher.hash("Hane1234!")
        assertNotEquals("Hane1234!", hash)
        assertTrue(PasswordHasher.verify("Hane1234!", hash))
        assertFalse(PasswordHasher.verify("YanlisParola", hash))
    }

    @Test
    fun `gizli soru cevabi buyuk kucuk harf ve bosluklara karsi toleransli olmali`() {
        val answerHash = PasswordHasher.hashNormalizedAnswer("  Boncuk ")
        assertTrue(PasswordHasher.verifyNormalizedAnswer("boncuk", answerHash))
        assertTrue(PasswordHasher.verifyNormalizedAnswer("BONCUK", answerHash))
        assertFalse(PasswordHasher.verifyNormalizedAnswer("pamuk", answerHash))
    }
}
