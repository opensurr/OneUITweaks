package com.oneuitweak.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Color(0xFF3468CB), onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE6FB), onPrimaryContainer = Color(0xFF0B1E3F),
    secondary = Color(0xFF4A6FA5), onSecondary = Color.White,
    background = Color(0xFFF4F7FD), onBackground = Color(0xFF0B1E3F),
    surface = Color.White, onSurface = Color(0xFF0B1E3F),
    surfaceVariant = Color(0xFFE8EEF9), onSurfaceVariant = Color(0xFF3B4D6E),
    error = Color(0xFFB3261E)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8DB0FF), onPrimary = Color(0xFF0B1E3F),
    primaryContainer = Color(0xFF1B3A73), onPrimaryContainer = Color(0xFFDCE6FB),
    secondary = Color(0xFFA9C4F5), onSecondary = Color(0xFF0B1E3F),
    background = Color(0xFF0B1E3F), onBackground = Color.White,
    surface = Color(0xFF122A55), onSurface = Color.White,
    surfaceVariant = Color(0xFF1B3A73), onSurfaceVariant = Color(0xFFC9D8F5),
    error = Color(0xFFFFB4AB)
)

@Composable
fun OneUiTweakTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}

class Actions(
    val run: (String, () -> ShizukuShell.Result) -> Unit,
    val connect: () -> Unit,
    val openShizuku: () -> Unit,
    val refresh: () -> Unit,
    val setLauncher: (String) -> Unit,
    val saveProfile: (Profile) -> Unit,
    val deleteProfile: (String) -> Unit
)

enum class Tab(val label: String, val glyph: String) {
    HOME("Home", "⌂"),
    DISPLAY("Display", "◐"),
    MOTION("Motion", "◎"),
    APPS("Apps", "▦"),
    PROFILES("Profiles", "★")
}

@Composable
fun AppRoot(
    connected: Boolean,
    granted: Boolean,
    oneUi: OneUiCheck.Info,
    current: Current,
    profiles: List<Profile>,
    busy: Boolean,
    message: String,
    actions: Actions
) {
    var tabIndex by rememberSaveable { mutableStateOf(0) }
    val canUse = granted && oneUi.supported && !busy

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tabIndex == i,
                        onClick = { tabIndex = i },
                        icon = { Text(t.glyph) },
                        label = { Text(t.label) }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Image(
                painter = painterResource(R.drawable.banner_onetweak),
                contentDescription = "OneUI Tweak banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
            if (busy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(16.dp)
            )

            when (Tab.entries[tabIndex]) {
                Tab.HOME -> HomeTab(connected, granted, oneUi, canUse, actions)
                Tab.DISPLAY -> DisplayTab(current, canUse, actions)
                Tab.MOTION -> MotionTab(current, canUse, actions)
                Tab.APPS -> AppsTab(actions)
                Tab.PROFILES -> ProfilesTab(profiles, current, canUse, actions)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            content()
        }
    }
}

@Composable
private fun HomeTab(
    connected: Boolean,
    granted: Boolean,
    oneUi: OneUiCheck.Info,
    canUse: Boolean,
    actions: Actions
) {
    SectionCard("Shizuku connection", "Gives OneUI Tweak system-level control without root.") {
        Text("Device: ${oneUi.label}")
        Text(
            when {
                !connected -> "Shizuku: not running"
                !granted -> "Shizuku: running, not connected yet"
                else -> "Shizuku: connected"
            }
        )
        if (!oneUi.supported) {
            Text("This app supports One UI 8 and One UI 9 only.", color = MaterialTheme.colorScheme.error)
        }
        if (!granted) {
            Button(onClick = actions.connect, modifier = Modifier.fillMaxWidth()) {
                Text("Connect to Shizuku")
            }
        }
        if (!connected) {
            OutlinedButton(onClick = actions.openShizuku, modifier = Modifier.fillMaxWidth()) {
                Text("Open Shizuku app")
            }
        }
        TextButton(onClick = actions.refresh) { Text("Refresh status") }
    }

    SectionCard("Reset", "Restores animation, text size, density, dark mode, rotation and timeout.") {
        Button(
            enabled = canUse,
            onClick = { actions.run("Reset") { Tweaks.resetAll() } },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Reset everything") }
    }

    SectionCard("About") {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(
                painter = painterResource(R.drawable.logo_mark),
                contentDescription = null,
                modifier = Modifier.size(56.dp)
            )
            Column {
                Text("OneUI Tweak 2.0", style = MaterialTheme.typography.titleSmall)
                Text("Built for One UI 8 and One UI 9. No root needed.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun DisplayTab(current: Current, canUse: Boolean, actions: Actions) {
    var font by remember(current.font) { mutableFloatStateOf(current.font) }
    var dpi by remember { mutableStateOf("") }

    SectionCard("Text size", "Changes the system font scale.") {
        Text("Scale: ${"%.2f".format(font)}x")
        Slider(
            value = font,
            onValueChange = { font = it },
            valueRange = 0.85f..1.3f,
            steps = 8,
            enabled = canUse
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = canUse, onClick = { actions.run("Text size") { Tweaks.setFontScale(font) } }) {
                Text("Apply")
            }
            OutlinedButton(enabled = canUse, onClick = {
                font = 1f
                actions.run("Text size") { Tweaks.setFontScale(1f) }
            }) { Text("Normal") }
        }
    }

    SectionCard("Screen density", "Changes how big things look on screen. Use 120 to 800.") {
        OutlinedTextField(
            value = dpi,
            onValueChange = { dpi = it.filter(Char::isDigit) },
            label = { Text("DPI") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = canUse, onClick = {
                actions.run("Density") { Tweaks.setDensity(dpi.toIntOrNull()) }
            }) { Text("Apply") }
            OutlinedButton(enabled = canUse, onClick = {
                actions.run("Density") { Tweaks.resetDensity() }
            }) { Text("Reset") }
        }
    }

    SectionCard("Dark mode") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = canUse, onClick = { actions.run("Dark mode") { Tweaks.setNightMode("yes") } }) { Text("On") }
            OutlinedButton(enabled = canUse, onClick = { actions.run("Dark mode") { Tweaks.setNightMode("no") } }) { Text("Off") }
            OutlinedButton(enabled = canUse, onClick = { actions.run("Dark mode") { Tweaks.setNightMode("auto") } }) { Text("Auto") }
        }
    }

    SectionCard("Auto-rotate") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Rotate screen automatically", modifier = Modifier.weight(1f))
            Switch(
                checked = current.autoRotate,
                enabled = canUse,
                onCheckedChange = { on -> actions.run("Auto-rotate") { Tweaks.setAutoRotate(on) } }
            )
        }
    }

    SectionCard("Screen timeout", "Current: ${current.timeoutSec}s") {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(30 to "30s", 60 to "1m", 300 to "5m", 600 to "10m").forEach { (sec, label) ->
                OutlinedButton(enabled = canUse, onClick = {
                    actions.run("Screen timeout") { Tweaks.setScreenTimeout(sec) }
                }) { Text(label) }
            }
        }
    }
}

@Composable
private fun MotionTab(current: Current, canUse: Boolean, actions: Actions) {
    var anim by remember(current.anim) { mutableFloatStateOf(current.anim) }

    SectionCard("Animation speed", "Scales window, transition and in-app animations.") {
        Text("Scale: ${"%.2f".format(anim)}x")
        Slider(
            value = anim,
            onValueChange = { anim = it },
            valueRange = 0f..2f,
            steps = 19,
            enabled = canUse
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = canUse, onClick = {
                actions.run("Animation speed") { Tweaks.setAnimationScale(anim) }
            }) { Text("Apply") }
            OutlinedButton(enabled = canUse, onClick = {
                anim = 1f
                actions.run("Animation speed") { Tweaks.setAnimationScale(1f) }
            }) { Text("Normal") }
        }
    }

    SectionCard("Presets") {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Off" to 0f, "Fast" to 0.5f, "Normal" to 1f, "Slow" to 1.5f).forEach { (label, v) ->
                FilledTonalButton(enabled = canUse, onClick = {
                    anim = v
                    actions.run("Preset $label") { Tweaks.setAnimationScale(v) }
                }) { Text(label) }
            }
        }
    }
}

@Composable
private fun AppsTab(actions: Actions) {
    var pkg by remember { mutableStateOf("") }

    SectionCard(
        "Enable or disable an app",
        "Uses Shizuku for user 0. Disabling system apps can break things. Connect first."
    ) {
        OutlinedTextField(
            value = pkg,
            onValueChange = { pkg = it.trim() },
            label = { Text("Package name, e.g. com.example.app") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { actions.run("Disable $pkg") { Tweaks.setAppEnabled(pkg, false) } }) {
                Text("Disable")
            }
            OutlinedButton(onClick = { actions.run("Enable $pkg") { Tweaks.setAppEnabled(pkg, true) } }) {
                Text("Enable")
            }
        }
    }

    SectionCard(
        "OneUI Tweak icon",
        "Changes only this app's icon. Other apps' icons need an icon pack launcher. No Shizuku needed."
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { actions.setLauncher("LauncherDefault") }) { Text("Blue") }
            OutlinedButton(onClick = { actions.setLauncher("LauncherDark") }) { Text("Navy") }
        }
    }
}

@Composable
private fun ProfilesTab(
    profiles: List<Profile>,
    current: Current,
    canUse: Boolean,
    actions: Actions
) {
    var name by remember { mutableStateOf("") }
    var night by remember { mutableStateOf("auto") }

    SectionCard(
        "Save current setup",
        "Stores motion, text size, dark mode, rotation and screen timeout. Connect first."
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Profile name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("yes", "no", "auto").forEach { m ->
                FilterChip(
                    selected = night == m,
                    onClick = { night = m },
                    label = { Text(m) }
                )
            }
        }
        Button(
            enabled = canUse && name.isNotBlank(),
            onClick = {
                actions.saveProfile(
                    Profile(name.trim(), current.anim, current.font, night, current.autoRotate, current.timeoutSec)
                )
                name = ""
            }
        ) { Text("Save profile") }
    }

    if (profiles.isEmpty()) {
        SectionCard("No profiles yet") {
            Text("Save your favorite setup once, then apply it in one tap.")
        }
    }

    profiles.forEach { p ->
        SectionCard(
            p.name,
            "Motion ${"%.2f".format(p.anim)}x · Text ${"%.2f".format(p.font)}x · Dark ${p.nightMode} · " +
                "Rotate ${if (p.autoRotate) "on" else "off"} · Screen ${p.timeoutSec}s"
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = canUse, onClick = {
                    actions.run("Profile ${p.name}") { Tweaks.applyProfile(p) }
                }) { Text("Apply") }
                TextButton(onClick = { actions.deleteProfile(p.name) }) { Text("Delete") }
            }
        }
    }
}
