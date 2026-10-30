package io.github.mzacode.truthglasses

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.mzacode.truthglasses.ui.Anton

data class TargetApp(
    val packageName: String,
    val label: String,
    val verified: Boolean,
    val enabled: Boolean
)

data class HomeState(
    val serviceEnabled: Boolean = false,
    val glassesOn: Boolean = true,
    val debugMode: Boolean = false,
    val apps: List<TargetApp> = emptyList()
)

class HomeActions(
    val openAppInfo: () -> Unit,
    val openAccessibilitySettings: () -> Unit,
    // null avant Android 13 : la tuile s'ajoute alors à la main
    val addTile: (() -> Unit)?,
    val setGlassesOn: (Boolean) -> Unit,
    val setAppEnabled: (String, Boolean) -> Unit,
    val openSandbox: () -> Unit,
    val setDebugMode: (Boolean) -> Unit
)

@Composable
fun HomeScreen(state: HomeState, actions: HomeActions) {
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .safeDrawingPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.app_name).uppercase(), fontFamily = Anton, fontSize = 40.sp)
            StatusLine(state.serviceEnabled)
            if (state.serviceEnabled) ActiveContent(state, actions) else Guide(actions)
            HorizontalDivider()
            Text(
                stringResource(R.string.transparency),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusLine(serviceEnabled: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier
                .size(10.dp)
                .background(if (serviceEnabled) Color(0xFF2E7D32) else Color(0xFFC62828), CircleShape)
        )
        Text(
            stringResource(if (serviceEnabled) R.string.status_on else R.string.status_off),
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun Guide(actions: HomeActions) {
    Text(stringResource(R.string.guide_title), style = MaterialTheme.typography.titleLarge)
    var number = 1
    // Les paramètres restreints n'existent qu'à partir d'Android 13
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Step(number++, R.string.step_restricted) {
            Button(actions.openAppInfo) { Text(stringResource(R.string.open_app_info)) }
        }
    }
    Step(number++, R.string.step_accessibility) {
        Button(actions.openAccessibilitySettings) { Text(stringResource(R.string.open_accessibility_settings)) }
    }
    TileStep(number, actions.addTile)
}

@Composable
private fun TileStep(number: Int, addTile: (() -> Unit)?) {
    if (addTile != null) {
        Step(number, R.string.step_tile) {
            Button(addTile) { Text(stringResource(R.string.add_tile)) }
        }
    } else {
        Step(number, R.string.step_tile_manual) {}
    }
}

@Composable
private fun Step(number: Int, @StringRes text: Int, action: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("$number", fontFamily = Anton, fontSize = 28.sp)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(text), style = MaterialTheme.typography.bodyLarge)
            action()
        }
    }
}

@Composable
private fun ActiveContent(state: HomeState, actions: HomeActions) {
    GlassesSwitch(state.glassesOn, actions.setGlassesOn)

    Text(stringResource(R.string.apps_title), style = MaterialTheme.typography.titleLarge)
    Column {
        state.apps.forEach { app ->
            SwitchRow(
                title = app.label,
                subtitle = if (app.verified) null else stringResource(R.string.app_not_verified),
                checked = app.enabled,
                onChange = { actions.setAppEnabled(app.packageName, it) }
            )
        }
    }

    OutlinedButton(actions.openSandbox, Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.open_sandbox))
    }
    actions.addTile?.let { addTile ->
        OutlinedButton(addTile, Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.add_tile))
        }
    }

    SwitchRow(
        title = stringResource(R.string.debug_mode),
        subtitle = stringResource(R.string.debug_mode_summary),
        checked = state.debugMode,
        onChange = actions.setDebugMode
    )
}

@Composable
private fun GlassesSwitch(on: Boolean, onChange: (Boolean) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .toggleable(value = on, role = Role.Switch, onValueChange = onChange)
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.tile_label).uppercase(), fontFamily = Anton, fontSize = 36.sp)
                Text(
                    stringResource(if (on) R.string.glasses_on else R.string.glasses_off),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Switch(checked = on, onCheckedChange = null)
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}
