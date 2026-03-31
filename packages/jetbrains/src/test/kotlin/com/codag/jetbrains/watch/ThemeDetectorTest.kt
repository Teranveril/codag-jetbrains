package com.codag.jetbrains.watch

import org.junit.Assert.*
import org.junit.Test

/**
 * TDD tests for ThemeDetector — maps IDE theme to webview CSS class.
 * Pure utility, no IDE dependencies.
 */
class ThemeDetectorTest {

    @Test
    fun testDarkThemeNames() {
        assertEquals("vscode-dark", ThemeDetector.resolveThemeClass("Darcula"))
        assertEquals("vscode-dark", ThemeDetector.resolveThemeClass("One Dark"))
        assertEquals("vscode-dark", ThemeDetector.resolveThemeClass("Monokai Pro"))
        assertEquals("vscode-dark", ThemeDetector.resolveThemeClass("Dark"))
    }

    @Test
    fun testLightThemeNames() {
        assertEquals("vscode-light", ThemeDetector.resolveThemeClass("IntelliJ Light"))
        assertEquals("vscode-light", ThemeDetector.resolveThemeClass("Default"))
        assertEquals("vscode-light", ThemeDetector.resolveThemeClass("Solarized Light"))
    }

    @Test
    fun testHighContrastTheme() {
        assertEquals("vscode-high-contrast", ThemeDetector.resolveThemeClass("High contrast"))
        assertEquals("vscode-high-contrast", ThemeDetector.resolveThemeClass("High Contrast"))
    }

    @Test
    fun testUnknownThemeDefaultsToDark() {
        assertEquals("vscode-dark", ThemeDetector.resolveThemeClass("SomeCustomTheme"))
    }

    @Test
    fun testGeneratesThemeInjectionScript() {
        val script = ThemeDetector.generateThemeScript("vscode-dark")

        assertTrue(script.contains("vscode-dark"))
        assertTrue(script.contains("document.body"))
        assertTrue(script.contains("classList"))
    }

    @Test
    fun testThemeScriptRemovesPreviousThemes() {
        val script = ThemeDetector.generateThemeScript("vscode-light")

        assertTrue(script.contains("remove"))
        assertTrue(script.contains("vscode-dark"))
        assertTrue(script.contains("vscode-light"))
        assertTrue(script.contains("vscode-high-contrast"))
        assertTrue(script.contains("add"))
    }
}
