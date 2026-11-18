package com.hanecuzdan.app.core.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GitHubBackupEngineTest {

    @Test
    fun `github repo url ve kisa format dogru ayristirilmali`() {
        val full = GitHubBackupEngine.parseOwnerAndRepo("https://github.com/kullanici/hane-yedek.git")
        val short = GitHubBackupEngine.parseOwnerAndRepo("kullanici/hane-yedek")
        val invalid = GitHubBackupEngine.parseOwnerAndRepo("gecersiz")

        assertEquals("kullanici" to "hane-yedek", full)
        assertEquals("kullanici" to "hane-yedek", short)
        assertNull(invalid)
    }
}
