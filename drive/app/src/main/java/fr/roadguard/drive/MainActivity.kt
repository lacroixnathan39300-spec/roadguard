package fr.roadguard.drive

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import fr.roadguard.drive.map.DriveMap
import fr.roadguard.drive.ui.DriveColors
import fr.roadguard.drive.ui.Speedometer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DriveHome() }
    }
}

@Composable
private fun DriveHome() {
    val context = LocalContext.current
    var hasLocation by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val askLocation = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasLocation = it }
    LaunchedEffect(Unit) { if (!hasLocation) askLocation.launch(Manifest.permission.ACCESS_FINE_LOCATION) }

    val speedFlow = remember(hasLocation) { if (hasLocation) SpeedSource(context).fixes() else emptyFlow() }
    val fix by speedFlow.collectAsState(initial = null)
    var showApps by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(DriveColors.Background)) {
        Row(Modifier.fillMaxSize().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Panel(Modifier.weight(0.62f).fillMaxHeight()) {
                DriveMap(hasLocation = hasLocation, modifier = Modifier.fillMaxSize())
            }
            Column(Modifier.weight(0.38f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Clock()
                Panel(Modifier.fillMaxWidth().weight(1f)) {
                    Speedometer(
                        kmh = fix?.kmh,
                        unit = stringResource(R.string.speed_unit),
                        waiting = if (hasLocation) stringResource(R.string.gps_waiting) else stringResource(R.string.location_needed),
                        modifier = Modifier.padding(16.dp),
                    )
                }
                Panel(Modifier.fillMaxWidth().height(72.dp)) {
                    Text(stringResource(R.string.music_placeholder), color = DriveColors.TextSecondary)
                }
                Panel(Modifier.fillMaxWidth().height(64.dp).clickable { showApps = true }) {
                    Text(stringResource(R.string.apps), color = Color.White, fontSize = 20.sp)
                }
            }
        }
        if (showApps) AppDrawer(onClose = { showApps = false })
    }
}

@Composable
private fun Panel(modifier: Modifier, content: @Composable () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(20.dp)).background(DriveColors.Panel),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun Clock() {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) { while (true) { now = Date(); delay(1_000) } }
    val format = remember { SimpleDateFormat("HH:mm", Locale.FRANCE) }
    Text(format.format(now), color = Color.White, fontSize = 40.sp, modifier = Modifier.padding(start = 8.dp))
}

@Composable
private fun AppDrawer(onClose: () -> Unit) {
    val context = LocalContext.current
    val catalog = remember { AppCatalog(context) }
    var apps by remember { mutableStateOf<List<LaunchableApp>>(emptyList()) }
    LaunchedEffect(Unit) { apps = withContext(Dispatchers.IO) { catalog.load() } }
    BackHandler(onBack = onClose)

    Box(Modifier.fillMaxSize().background(DriveColors.Background.copy(alpha = 0.97f)).clickable(onClick = onClose)) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(120.dp),
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(apps, key = { it.packageName + "/" + it.activity }) { app ->
                Column(
                    Modifier.clip(RoundedCornerShape(16.dp)).clickable { catalog.launch(app); onClose() }.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    app.icon?.let { Image(it.asImageBitmap(), contentDescription = null, modifier = Modifier.size(64.dp)) }
                        ?: Spacer(Modifier.size(64.dp))
                    Text(app.label, color = Color.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
