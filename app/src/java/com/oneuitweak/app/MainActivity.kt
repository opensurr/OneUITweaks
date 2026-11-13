package com.oneuitweak.app

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {

    private val shizukuRunning = mutableStateOf(false)
    private val shizukuGranted = mutableStateOf(false)
    private val oneUi = mutableStateOf(OneUiCheck.Info(false, "Checking..."))

    private val requestCode = 1001

    private val onBinderReceived = Shizuku.OnBinderReceivedListener {
        runOnUiThread { refreshShizuku() }
    }
    private val onBinderDead = Shizuku.OnBinderDeadListener {
        runOnUiThread { refreshShizuku() }
    }
    private val onPermission = Shizuku.OnRequestPermissionResultListener { _, _ ->
        runOnUiThread { refreshShizuku() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Shizuku.addBinderReceivedListenerSticky(onBinderReceived)
        Shizuku.addBinderDeadListener(onBinderDead)
        Shizuku.addRequestPermissionResultListener(onPermission)

        oneUi.value = OneUiCheck.check()
        refreshShizuku()

        setContent {
            OneUiTweakTheme {
                TweakScreen(
                    shizukuRunning = shizukuRunning.value,
                    shizukuGranted = shizukuGranted.value,
                    oneUi = oneUi.value,
                    onRequestPermission = {
                        runCatching { Shizuku.requestPermission(requestCode) }
                    }
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

    private fun refreshShizuku() {
        shizukuRunning.value = ShizukuShell.isReady
        shizukuGranted.value = ShizukuShell.hasPermission
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

@Composable
fun OneUiTweakTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= 31 && dark -> dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= 31 -> dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
fun TweakScreen(
    shizukuRunning: Boolean,
    shizukuGranted: Boolean,
    oneUi: OneUiCheck.Info,
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var status by remember { mutableStateOf("Ready") }
    var animScale by remember { mutableFloatStateOf(1f) }
    var fontScale by remember { mutableFloatStateOf(1f) }
    var dpi by remember { mutableStateOf("") }
    var pkg by remember { mutableStateOf("") }

    val canUse = oneUi.supported && shizukuGranted

    fun exec(block: () -> ShizukuShell.Result) {
        scope.launch {
            val r = withContext(Dispatchers.IO) { block() }
            status = if (r.ok) "Done" else "Failed: ${r.output.ifBlank { "unknown error" }}"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("OneUI Tweak", style = MaterialTheme.typography.headlineMedium)

        Section("Status") {
            Text("Device: ${oneUi.label}")
            Text(
                "Shizuku: " + when {
                    !shizukuRunning -> "not running"
                    !shizukuGranted -> "running, permission needed"
                    else -> "ready"
                }
            )
            if (!oneUi.supported) {
                Text(
                    "This app only supports One UI 8 and One UI 9.",
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (shizukuRunning && !shizukuGranted) {
                Button(onClick = onRequestPermission) { Text("Grant Shizuku permission") }
            }
            Text("Last action: $status", style = MaterialTheme.typography.bodySmall)
        }

        Section("Animation speed") {
            Text("Scale: ${"%.2f".format(animScale)}x")
            Slider(
                value = animScale,
                onValueChange = { animScale = it },
                valueRange = 0f..2f,
                steps = 19
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = canUse, onClick = {
                    exec { Tweaks.setAnimationScale(animScale) }
                }) { Text("Apply") }
                OutlinedButton(enabled = canUse, onClick = {
                    animScale = 1f
                    exec { Tweaks.setAnimationScale(1f) }
                }) { Text("Reset") }
            }
        }

        Section("Font scale") {
            Text("Scale: ${"%.2f".format(fontScale)}x")
            Slider(
                value = fontScale,
                onValueChange = { fontScale = it },
                valueRange = 0.85f..1.3f,
                steps = 8
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = canUse, onClick = {
                    exec { Tweaks.setFontScale(fontScale) }
                }) { Text("Apply") }
                OutlinedButton(enabled = canUse, onClick = {
                    fontScale = 1f
                    exec { Tweaks.setFontScale(1f) }
                }) { Text("Reset") }
            }
        }

        Section("Screen density (DPI)") {
            OutlinedTextField(
                value = dpi,
                onValueChange = { dpi = it.filter(Char::isDigit) },
                label = { Text("DPI, 120 to 800") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = canUse, onClick = {
                    exec { Tweaks.setDensity(dpi.toIntOrNull()) }
                }) { Text("Apply") }
                OutlinedButton(enabled = canUse, onClick = {
                    exec { Tweaks.resetDensity() }
                }) { Text("Reset") }
            }
        }

        Section("Dark mode") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = canUse, onClick = { exec { Tweaks.setNightMode("yes") } }) {
                    Text("On")
                }
                Button(enabled = canUse, onClick = { exec { Tweaks.setNightMode("no") } }) {
                    Text("Off")
                }
                Button(enabled = canUse, onClick = { exec { Tweaks.setNightMode("auto") } }) {
                    Text("Auto")
                }
            }
        }

        Section("This app's icon") {
            Text(
                "Changes only the icon of OneUI Tweak. Other apps' icons need root or an icon pack launcher.",
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    setLauncherIcon(context, "LauncherDefault")
                    status = "Icon changed. The launcher may take a moment to refresh."
                }) { Text("Blue") }
                OutlinedButton(onClick = {
                    setLauncherIcon(context, "LauncherDark")
                    status = "Icon changed. The launcher may take a moment to refresh."
                }) { Text("Dark") }
            }
        }

        Section("Disable or enable an app (user 0)") {
            Text(
                "Disabling system apps can break things. Use with care.",
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedTextField(
                value = pkg,
                onValueChange = { pkg = it.trim() },
                label = { Text("Package name, e.g. com.example.app") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = canUse, onClick = {
                    exec { Tweaks.setAppEnabled(pkg, false) }
                }) { Text("Disable") }
                OutlinedButton(enabled = canUse, onClick = {
                    exec { Tweaks.setAppEnabled(pkg, true) }
                }) { Text("Enable") }
            }
        }

        Section("Reset everything") {
            OutlinedButton(enabled = canUse, onClick = {
                animScale = 1f
                fontScale = 1f
                exec { Tweaks.resetAll() }
            }) { Text("Reset all tweaks") }
        }
    }
}
