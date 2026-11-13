package com.oneuitweak.app

import java.util.Locale

object Tweaks {

    private val packageRegex = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z0-9_]+)+$")

    fun setAnimationScale(scale: Float): ShizukuShell.Result {
        val s = String.format(Locale.US, "%.2f", scale)
        return ShizukuShell.exec(
            "settings put global window_animation_scale $s",
            "settings put global transition_animation_scale $s",
            "settings put global animator_duration_scale $s"
        )
    }

    fun setFontScale(scale: Float): ShizukuShell.Result {
        val s = String.format(Locale.US, "%.2f", scale)
        return ShizukuShell.exec("settings put system font_scale $s")
    }

    fun setDensity(dpi: Int?): ShizukuShell.Result {
        if (dpi == null || dpi !in 120..800) {
            return ShizukuShell.Result(false, "Enter a DPI between 120 and 800")
        }
        return ShizukuShell.exec("wm density $dpi")
    }

    fun resetDensity(): ShizukuShell.Result = ShizukuShell.exec("wm density reset")

    fun setNightMode(mode: String): ShizukuShell.Result {
        if (mode !in setOf("yes", "no", "auto")) {
            return ShizukuShell.Result(false, "Invalid mode")
        }
        return ShizukuShell.exec("cmd uimode night $mode")
    }

    fun setAppEnabled(pkg: String, enabled: Boolean): ShizukuShell.Result {
        if (!packageRegex.matches(pkg)) {
            return ShizukuShell.Result(false, "Invalid package name")
        }
        val cmd = if (enabled) "pm enable --user 0 $pkg" else "pm disable-user --user 0 $pkg"
        return ShizukuShell.exec(cmd)
    }

    fun resetAll(): ShizukuShell.Result {
        val results = listOf(
            setAnimationScale(1f),
            setFontScale(1f),
            resetDensity(),
            setNightMode("auto")
        )
        return results.firstOrNull { !it.ok } ?: ShizukuShell.Result(true, "")
    }
}
