package com.oneuitweak.app

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

/**
 * Runs shell commands through Shizuku (shell uid, no root needed).
 * Uses the same internal newProcess API that many Shizuku apps rely on.
 */
object ShizukuShell {

    data class Result(val ok: Boolean, val output: String)

    private val newProcessMethod by lazy {
        Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java
        ).apply { isAccessible = true }
    }

    val isReady: Boolean
        get() = runCatching { Shizuku.pingBinder() }.getOrDefault(false)

    val hasPermission: Boolean
        get() = runCatching {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)

    fun exec(vararg commands: String): Result {
        return try {
            val script = commands.joinToString(" && ")
            val process = newProcessMethod.invoke(
                null,
                arrayOf("sh", "-c", script),
                null,
                null
            ) as Process

            val out = process.inputStream.bufferedReader().use { it.readText() }
            val err = process.errorStream.bufferedReader().use { it.readText() }
            val code = process.waitFor()
            Result(code == 0, (out + err).trim())
        } catch (t: Throwable) {
            Result(false, t.message ?: t.javaClass.simpleName)
        }
    }
}
