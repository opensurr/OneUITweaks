package com.oneuitweak.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
private const val SHIZUKU_DOWNLOAD = "https://shizuku.rikka.app/download/"

class MainActivity : ComponentActivity() {

    private val connected = mutableStateOf(false)
    private val granted = mutableStateOf(false)
    private val oneUi = mutableStateOf(OneUiCheck.Info(false, "Checking..."))
    private val current = mutableStateOf(Current())
    private val profiles = mutableStateOf<List<Profile>>(emptyList())
    private val busy = mutableStateOf(false)
    private val message = mutableStateOf("Welcome to OneUI Tweak")
    private val requestCode = 1001

    private val onBinderReceived = Shizuku.OnBinderReceivedListener {
        runOnUiThread { refresh() }
    }
    private val onBinderDead = Shizuku.OnBinderDeadListener {
        runOnUiThread { refresh() }
    }
    private val onPermission = Shizuku.OnRequestPermissionResultListener { _, result ->
        runOnUiThread {
            message.value = if (result == PackageManager.PERMISSION_GRANTED) {
                "Connected to Shizuku"
            } else {
                "Shizuku permission was denied"
            }
            refresh()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Shizuku.addBinderReceivedListenerSticky(onBinderReceived)
        Shizuku.addBinderDeadListener(onBinderDead)
        Shizuku.addRequestPermissionResultListener(onPermission)

        profiles.value = ProfileStore.load(this)

        lifecycleScope.launch(Dispatchers.IO) {
            val info = OneUiCheck.check()
            withContext(Dispatchers.Main) { oneUi.value = info }
        }
        refresh()

        setContent {
            OneUiTweakTheme {
                AppRoot(
                    connected = connected.value,
                    granted = granted.value,
                    oneUi = oneUi.value,
                    current = current.value,
                    profiles = profiles.value,
                    busy = busy.value,
                    message = message.value,
                    actions = Actions(
                        run = this::runTweak,
                        connect = this::connectShizuku,
                        openShizuku = this::openShizuku,
                        refresh = this::refresh,
                        setLauncher = { variant ->
                            setLauncherIcon(this@MainActivity, variant)
                            message.value = "Icon changed. The launcher may take a moment to refresh."
                        },
                        saveProfile = this::saveProfile,
                        deleteProfile = this::deleteProfile
                    )
                )
            }
        }
    }

    override fun onDestroy() {
        Shizuku.removeBinderReceivedListener(onBinderReceived)
        Shizuku.removeBinderDeadListener(onBinderDead)
        Shizuku.removeRequestPermissionResultListener(onPermission)
        super.onDestroy()
    }

    private fun refresh() {
        connected.value = ShizukuShell.isReady
        granted.value = ShizukuShell.hasPermission
        if (granted.value) {
            lifecycleScope.launch(Dispatchers.IO) {
                val c = Tweaks.readCurrent()
                withContext(Dispatchers.Main) { current.value = c }
            }
        }
    }

    private fun runTweak(label: String, block: () -> ShizukuShell.Result) {
        if (busy.value) return
        busy.value = true
        lifecycleScope.launch {
            try {
                val r = withContext(Dispatchers.IO) { block() }
                message.value = if (r.ok) {
                    "$label: done"
                } else {
                    "$label failed: ${r.output.ifBlank { "unknown error" }}"
                }
            } finally {
                busy.value = false
                refresh()
            }
        }
    }

    private fun connectShizuku() {
        when {
            !ShizukuShell.isReady -> {
                message.value = "Shizuku is not running. Start it, then tap Connect again."
                openShizuku()
            }
            ShizukuShell.hasPermission -> {
                message.value = "Already connected to Shizuku"
                refresh()
            }
            else -> runCatching { Shizuku.requestPermission(requestCode) }
                .onFailure { message.value = "Could not request permission: ${it.message}" }
        }
    }

    private fun openShizuku() {
        val intent = packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
            ?: Intent(Intent.ACTION_VIEW, Uri.parse(SHIZUKU_DOWNLOAD))
        runCatching { startActivity(intent) }
            .onFailure { message.value = "Could not open Shizuku" }
    }

    private fun saveProfile(p: Profile) {
        val updated = profiles.value.filterNot { it.name == p.name } + p
        profiles.value = updated
        ProfileStore.save(this, updated)
        message.value = "Saved profile ${p.name}"
    }

    private fun deleteProfile(name: String) {
        val updated = profiles.value.filterNot { it.name == name }
        profiles.value = updated
        ProfileStore.save(this, updated)
        message.value = "Deleted profile $name"
    }
}

/** Switches only this app's own launcher icon. Needs no Shizuku. */
fun setLauncherIcon(context: Context, variant: String) {
    val pm = context.packageManager
    listOf("LauncherDefault", "LauncherDark").forEach { name ->
        pm.setComponentEnabledSetting(
            ComponentName(context.packageName, "${context.packageName}.$name"),
            if (name == variant) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}
