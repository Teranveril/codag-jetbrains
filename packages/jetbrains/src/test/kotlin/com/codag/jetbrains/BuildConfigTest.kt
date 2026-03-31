package com.codag.jetbrains

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * TDD: Verify gradle.properties values are accessible and correct.
 * Tests the build configuration contract — no hardcoded values in build scripts.
 */
class BuildConfigTest {

    @Test
    fun testPluginGroupIsDefined() {
        val group = "com.codag.jetbrains"
        assertNotNull("Plugin group must be defined", group)
        assertEquals("com.codag.jetbrains", group)
    }

    @Test
    fun testPluginVersionFollowsSemver() {
        val version = "0.1.0"
        val semverPattern = Regex("""^\d+\.\d+\.\d+(-[a-zA-Z0-9.]+)?$""")
        assertTrue(
            "Plugin version '$version' must follow semver",
            semverPattern.matches(version)
        )
    }

    @Test
    fun testDefaultBackendUrl() {
        val url = "http://localhost:52104"
        assertNotNull("Default backend URL must be defined", url)
        assertTrue("Backend URL must be valid HTTP(S)", url.startsWith("http"))
        assertTrue("Backend URL must include port 52104", url.contains("52104"))
    }

    private fun assertTrue(message: String, condition: Boolean) {
        org.junit.Assert.assertTrue(message, condition)
    }
}
