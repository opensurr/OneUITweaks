package com.oneuitweak.app

import java.util.Locale

data class Current(
    val anim: Float = 1f,
    val font: Float = 1f,
    val autoRotate: Boolean = false,
    val timeoutSec: Int = 60
)

object Tweaks {

    private val packageRegex = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z0-9_]+)+$")

    private fun f(v: Float): String = String.format(Locale.US, "%.2f", v)

    fun setAnimationScale(scale: Float) = ShizukuShell.exec(
        "settings put global window_animation_scale ${f(scale)}",
        "settings put global transition_animation_scale ${f(scale)}",
        "settings put global animator_duration_scale ${f(scale)}"
    )

    fun setFontScale(scale: Float) =
        ShizukuShell.exec("settings put system font_scale ${f(scale)}")

    fun setDensity(dpi: Int?): ShizukuShell.Result =
        if (dpi == null || dpi !in 120..800) {
            ShizukuShell.Result(false, "Enter a DPI between 120 and 800")
        } else {
            ShizukuShell.exec("wm density $dpi")
        }

    fun resetDensity() = ShizukuShell.exec("wm density reset")

    fun setNightMode(mode: String): ShizukuShell.Result =
        if (mode !in setOf("yes", "no", "auto")) {
            ShizukuShell.Result(false, "Invalid mode")
        } else {
            ShizukuShell.exec("cmd uimode night $mode")
        }

    fun setAutoRotate(on: Boolean) =
        ShizukuShell.exec("settings put system accelerometer_rotation ${if (on) 1 else 0}")

    fun setScreenTimeout(seconds: Int) =
        ShizukuShell.exec("settings put system screen_off_timeout ${seconds * 1000}")

    fun setAppEnabled(pkg: String, enabled: Boolean): ShizukuShell.Result {
        if (!packageRegex.matches(pkg)) return ShizukuShell.Result(false, "Invalid package name")
        val cmd = if (enabled) "pm enable --user 0 $pkg" else "pm disable-user --user 0 $pkg"
        return ShizukuShell.exec(cmd)
    }

    fun readCurrent(): Current = Current(
        anim = ShizukuShell.query("global", "window_animation_scale")?.toFloatOrNull() ?: 1f,
        font = ShizukuShell.query("system", "font_scale")?.toFloatOrNull() ?: 1f,
        autoRotate = ShizukuShell.query("system", "accelerometer_rotation") == "1",
        timeoutSec = ((ShizukuShell.query("system", "screen_off_timeout")?.toLongOrNull()
            ?: 60000L) / 1000).toInt()
    )

    fun applyProfile(p: Profile): ShizukuShell.Result {
        val results = listOf(
            setAnimationScale(p.anim),
            setFontScale(p.font),
            setNightMode(p.nightMode),
            setAutoRotate(p.autoRotate),
            setScreenTimeout(p.timeoutSec)
        )
        return results.firstOrNull { !it.ok } ?: ShizukuShell.Result(true, "Profile ${p.name} applied")
    }

    fun resetAll(): ShizukuShell.Result {
        val results = listOf(
            setAnimationScale(1f),
            setFontScale(1f),
            resetDensity(),
            setNightMode("auto"),
            setAutoRotate(false),
            setScreenTimeout(60)
        )
        return results.firstOrNull { !it.ok } ?: ShizukuShell.Result(true, "Reset done")
    }
}
