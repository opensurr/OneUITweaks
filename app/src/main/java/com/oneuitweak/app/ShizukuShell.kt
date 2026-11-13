package com.oneuitweak.app

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

/** Runs shell commands through Shizuku (shell uid, no root needed). */
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

    fun exec(vararg commands: String): Result = try {
        val script = commands.joinToString(" && ")
        val process = newProcessMethod.invoke(
            null, arrayOf("sh", "-c", script), null, null
        ) as Process
        val out = process.inputStream.bufferedReader().use { it.readText() }
        val err = process.errorStream.bufferedReader().use { it.readText() }
        val code = process.waitFor()
        Result(code == 0, (out + err).trim())
    } catch (t: Throwable) {
        Result(false, t.message ?: t.javaClass.simpleName)
    }

    /** Reads a value with `settings get`. Returns null when unset or unavailable. */
    fun query(namespace: String, key: String): String? {
        val r = exec("settings get $namespace $key")
        return if (r.ok && r.output.isNotBlank() && r.output != "null") r.output.trim() else null
    }
}
