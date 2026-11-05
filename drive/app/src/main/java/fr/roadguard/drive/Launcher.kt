package fr.roadguard.drive

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import fr.roadguard.core.ui.BigButton
import fr.roadguard.core.ui.DriveColors
import fr.roadguard.core.ui.DriveStyle
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/** Modules de l'accueil (comme Car Launcher Pro). */
enum class ModuleType(val label: String, val emoji: String, val minW: Int, val minH: Int) {
    MAP("Carte et alertes", "🗺", 2, 2),
    SPEED("Compteur de vitesse", "⏱", 1, 1),
    MUSIC("Musique", "🎵", 2, 1),
    CLOCK("Horloge, météo et Link", "🕒", 2, 1),
    DOCK("Applis favorites", "▦", 2, 1),
    TRIP("Ordinateur de bord", "📊", 1, 1),
    SHORTCUTS("Raccourcis (signaler, recherche, réglages)", "⚡", 1, 1),
    WIDGET("Widget Android", "🧩", 1, 1),
}

/** Bloc posé sur la grille : colonne, ligne, largeur, hauteur (en cases). */
data class Tile(val id: Long, val type: ModuleType, val x: Int, val y: Int, val w: Int, val h: Int, val widgetId: Int = -1) {
    fun overlaps(o: Tile) = x < o.x + o.w && o.x < x + w && y < o.y + o.h && o.y < y + h
    fun toJson(): JSONObject = JSONObject().put("id", id).put("t", type.name).put("x", x).put("y", y).put("w", w).put("h", h).put("wid", widgetId)
    companion object {
        fun from(o: JSONObject) = runCatching {
            Tile(o.getLong("id"), ModuleType.valueOf(o.getString("t")), o.getInt("x"), o.getInt("y"), o.getInt("w"), o.getInt("h"), o.optInt("wid", -1))
        }.getOrNull()
    }
}

/** Pages de l'accueil, enregistrées sur le poste. Grille de [COLS] × [ROWS] cases. */
class LauncherLayout(context: Context) {
    private val prefs = context.getSharedPreferences("launcher", Context.MODE_PRIVATE)
    var pages by mutableStateOf(load()); private set

    private fun load(): List<List<Tile>> {
        val raw = prefs.getString("pages", null) ?: return defaults()
        return runCatching {
            val a = JSONArray(raw)
            (0 until a.length()).map { p -> val t = a.getJSONArray(p); (0 until t.length()).mapNotNull { Tile.from(t.getJSONObject(it)) } }
        }.getOrNull()?.takeIf { it.isNotEmpty() } ?: defaults()
    }

    private fun save(p: List<List<Tile>>) {
        pages = p
        prefs.edit().putString("pages", JSONArray(p.map { page -> JSONArray(page.map { it.toJson() }) }).toString()).apply()
    }

    fun reset() = save(defaults())

    /** Place libre la plus haute puis la plus à gauche pour un bloc [w]×[h], ou null. */
    fun freeSpot(page: Int, w: Int, h: Int, ignore: Long = -1): Pair<Int, Int>? {
        val tiles = pages.getOrNull(page).orEmpty().filter { it.id != ignore }
        for (y in 0..ROWS - h) for (x in 0..COLS - w) {
            val t = Tile(0, ModuleType.SPEED, x, y, w, h)
            if (tiles.none { it.overlaps(t) }) return x to y
        }
        return null
    }

    /** Ajoute un module à la page ; faux s'il n'y a plus de place. */
    fun add(page: Int, type: ModuleType, widgetId: Int = -1): Boolean {
        // Taille de départ confortable, sinon la taille minimale si la page est chargée.
        val sizes = listOf(maxOf(type.minW, 2) to maxOf(type.minH, if (type == ModuleType.MAP) 3 else 1), type.minW to type.minH)
        for ((w, h) in sizes) {
            val spot = freeSpot(page, w, h) ?: continue
            save(pages.mapIndexed { i, p -> if (i == page) p + Tile(System.nanoTime(), type, spot.first, spot.second, w, h, widgetId) else p })
            return true
        }
        return false
    }

    val hasMap: Boolean get() = pages.any { p -> p.any { it.type == ModuleType.MAP } }

    /** Déplace ou redimensionne ; refusé si ça sort de la grille ou chevauche un autre bloc. */
    fun change(page: Int, tile: Tile, dx: Int = 0, dy: Int = 0, dw: Int = 0, dh: Int = 0): Boolean {
        val n = tile.copy(x = tile.x + dx, y = tile.y + dy, w = tile.w + dw, h = tile.h + dh)
        if (n.x < 0 || n.y < 0 || n.x + n.w > COLS || n.y + n.h > ROWS || n.w < tile.type.minW || n.h < tile.type.minH) return false
        if (pages[page].any { it.id != tile.id && it.overlaps(n) }) return false
        save(pages.mapIndexed { i, p -> if (i == page) p.map { if (it.id == tile.id) n else it } else p })
        return true
    }

    fun remove(page: Int, tile: Tile) = save(pages.mapIndexed { i, p -> if (i == page) p.filter { it.id != tile.id } else p })
    fun addPage() = save(pages + listOf(emptyList()))
    fun removePage(page: Int) { if (pages.size > 1) save(pages.filterIndexed { i, _ -> i != page }) }

    companion object {
        const val COLS = 6
        const val ROWS = 4
        /** Accueil de départ : carte à gauche, compteur, musique et favoris à droite (comme avant), puis une page infos. */
        fun defaults(): List<List<Tile>> = listOf(
            listOf(
                Tile(1, ModuleType.MAP, 0, 0, 4, 4),
                Tile(2, ModuleType.CLOCK, 4, 0, 2, 1),
                Tile(3, ModuleType.SPEED, 4, 1, 2, 1),
                Tile(4, ModuleType.MUSIC, 4, 2, 2, 1),
                Tile(5, ModuleType.DOCK, 4, 3, 2, 1),
            ),
            listOf(
                Tile(6, ModuleType.CLOCK, 0, 0, 3, 1),
                Tile(7, ModuleType.SPEED, 3, 0, 3, 2),
                Tile(8, ModuleType.TRIP, 0, 1, 3, 2),
                Tile(9, ModuleType.MUSIC, 3, 2, 3, 2),
                Tile(10, ModuleType.SHORTCUTS, 0, 3, 3, 1),
            ),
        )
    }
}

/** Hôte des widgets Android (Spotify, météo, agenda…) posés sur l'accueil. */
object Widgets {
    private const val HOST_ID = 0x524F4144 // « ROAD »
    @Volatile private var host: AppWidgetHost? = null
    fun host(context: Context): AppWidgetHost = host ?: synchronized(this) {
        host ?: AppWidgetHost(context.applicationContext, HOST_ID).also { host = it }
    }
    fun providers(context: Context): List<AppWidgetProviderInfo> =
        AppWidgetManager.getInstance(context).installedProviders.sortedBy { it.loadLabel(context.packageManager).lowercase() }
}

/**
 * Accueil en pages (glisser à gauche / à droite) et modules sur une grille. Appui long sur un module ou
 * bouton ✎ : mode édition (déplacer, agrandir, retirer, ajouter un module ou un widget, ajouter une page).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LauncherPages(layout: LauncherLayout, modifier: Modifier = Modifier, onPage: (Int) -> Unit = {}, module: @Composable (Tile, Modifier) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Long?>(null) }
    var picker by remember { mutableStateOf(false) }
    var widgetPicker by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val pager = rememberPagerState(pageCount = { layout.pages.size })
    androidx.compose.runtime.LaunchedEffect(pager.currentPage) { onPage(pager.currentPage) }

    val host = remember { Widgets.host(context) }
    DisposableEffect(host) {
        runCatching { host.startListening() }
        onDispose { runCatching { host.stopListening() } }
    }
    // Widget en attente : autorisation Android puis éventuel écran de réglage du widget.
    var pendingWidget by remember { mutableStateOf(-1) }
    fun placeWidget(id: Int) {
        if (!layout.add(pager.currentPage, ModuleType.WIDGET, id)) { host.deleteAppWidgetId(id); message = "Plus de place sur cette page" }
        pendingWidget = -1
    }
    val configure = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (pendingWidget != -1) { if (r.resultCode == Activity.RESULT_OK) placeWidget(pendingWidget) else { host.deleteAppWidgetId(pendingWidget); pendingWidget = -1 } }
    }
    fun configureOrPlace(id: Int) {
        val info = AppWidgetManager.getInstance(context).getAppWidgetInfo(id)
        if (info?.configure != null) {
            runCatching {
                configure.launch(Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).setComponent(info.configure).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
            }.onFailure { placeWidget(id) }
        } else placeWidget(id)
    }
    val bind = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (pendingWidget != -1) { if (r.resultCode == Activity.RESULT_OK) configureOrPlace(pendingWidget) else { host.deleteAppWidgetId(pendingWidget); pendingWidget = -1; message = "Widget refusé" } }
    }
    fun addWidget(info: AppWidgetProviderInfo) {
        val id = host.allocateAppWidgetId()
        pendingWidget = id
        if (AppWidgetManager.getInstance(context).bindAppWidgetIdIfAllowed(id, info.provider)) configureOrPlace(id)
        else runCatching {
            bind.launch(Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider))
        }.onFailure { host.deleteAppWidgetId(id); pendingWidget = -1; message = "Ce poste ne permet pas d'ajouter des widgets" }
    }

    Box(modifier) {
        // Pages gardées en mémoire (la carte ne se recharge pas). Pas de glissement sur une page avec la carte
        // (le doigt déplace la carte) : flèches ‹ › en bas.
        val swipe = !editing && layout.pages.getOrNull(pager.currentPage)?.none { it.type == ModuleType.MAP } != false
        HorizontalPager(pager, Modifier.fillMaxSize(), userScrollEnabled = swipe, beyondViewportPageCount = layout.pages.size, key = { it }) { page ->
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val gap = 10.dp
                val cw = (maxWidth - gap * (LauncherLayout.COLS - 1)) / LauncherLayout.COLS
                val ch = (maxHeight - gap * (LauncherLayout.ROWS - 1)) / LauncherLayout.ROWS
                layout.pages.getOrNull(page).orEmpty().forEach { t ->
                    val m = Modifier.offset(x = (cw + gap) * t.x, y = (ch + gap) * t.y).size(cw * t.w + gap * (t.w - 1), ch * t.h + gap * (t.h - 1))
                    Box(m.combinedClickable(enabled = editing, onClick = { selected = t.id }, onLongClick = { editing = true; selected = t.id })) {
                        // Hors édition, le module reçoit lui-même les appuis (boutons, carte…).
                        module(t, Modifier.fillMaxSize())
                        if (editing) {
                            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(22.dp)).background(Color(0x55000000))
                                .border(if (selected == t.id) 3.dp else 1.dp, if (selected == t.id) DriveColors.Accent else Color(0x88FFFFFF), RoundedCornerShape(22.dp))
                                .clickable { selected = t.id })
                            Text("${t.type.emoji} ${t.type.label}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.TopStart).padding(10.dp))
                            if (selected == t.id) EditPad(
                                onMove = { dx, dy -> if (!layout.change(page, t, dx = dx, dy = dy)) message = "Pas de place par là" },
                                onResize = { dw, dh -> if (!layout.change(page, t, dw = dw, dh = dh)) message = "Impossible : pas de place ou taille minimale" },
                                onRemove = {
                                    if (t.widgetId != -1) runCatching { host.deleteAppWidgetId(t.widgetId) }
                                    layout.remove(page, t); selected = null
                                },
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }
                    }
                }
                if (editing && layout.pages.getOrNull(page).isNullOrEmpty()) {
                    Text("Page vide : ajoute un module ou un widget", color = DriveColors.TextSecondary, fontSize = 18.sp, modifier = Modifier.align(Alignment.Center))
                }
            }
        }
        // Points des pages.
        if (layout.pages.size > 1) Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x66000000)),
            horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("‹", color = Color.White, fontSize = 22.sp, modifier = Modifier.clickable {
                scope.launch { pager.animateScrollToPage((pager.currentPage - 1).coerceAtLeast(0)) } }.padding(horizontal = 12.dp))
            repeat(layout.pages.size) { i ->
                Box(Modifier.size(if (i == pager.currentPage) 9.dp else 7.dp).clip(CircleShape)
                    .background(if (i == pager.currentPage) DriveColors.Accent else Color(0x88FFFFFF))
                    .clickable { scope.launch { pager.animateScrollToPage(i) } })
            }
            Text("›", color = Color.White, fontSize = 22.sp, modifier = Modifier.clickable {
                scope.launch { pager.animateScrollToPage((pager.currentPage + 1).coerceAtMost(layout.pages.size - 1)) } }.padding(horizontal = 12.dp))
        }
        if (editing) {
            Row(
                Modifier.align(Alignment.TopCenter).clip(RoundedCornerShape(18.dp)).background(Color(0xEE101820)).padding(8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically,
            ) {
                BigButton("＋ Module", DriveColors.Accent) { picker = true }
                BigButton("🧩 Widget Android", DriveColors.Accent) { widgetPicker = true }
                BigButton("＋ Page", DriveColors.Track) {
                    layout.addPage(); scope.launch { pager.animateScrollToPage(layout.pages.size - 1) }
                }
                if (layout.pages.size > 1) BigButton("Supprimer la page", Color(0xFF7F1D1D)) {
                    layout.pages.getOrNull(pager.currentPage)?.forEach { if (it.widgetId != -1) runCatching { host.deleteAppWidgetId(it.widgetId) } }
                    layout.removePage(pager.currentPage)
                }
                BigButton("Par défaut", DriveColors.Track) { layout.reset(); selected = null }
                BigButton("✓ Terminé", Color(0xFF2E7D32)) { editing = false; selected = null; message = null }
            }
            message?.let {
                Text(it, color = Color.White, fontSize = 15.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp)
                    .clip(RoundedCornerShape(12.dp)).background(Color(0xDD7F1D1D)).padding(horizontal = 12.dp, vertical = 6.dp))
            }
        } else {
            // Bouton discret pour passer en édition (l'appui long sur un module marche aussi).
            Text("✎", color = Color(0xAAFFFFFF), fontSize = 18.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 22.dp)
                .clip(CircleShape).background(Color(0x66000000)).clickable { editing = true }.padding(8.dp))
        }
        if (picker) ModulePicker(hasMap = layout.hasMap, onPick = { type ->
            picker = false
            if (!layout.add(pager.currentPage, type)) message = "Plus de place sur cette page : ajoute une page"
        }, onClose = { picker = false })
        if (widgetPicker) WidgetPicker(onPick = { widgetPicker = false; addWidget(it) }, onClose = { widgetPicker = false })
    }
}

@Composable
private fun EditPad(onMove: (Int, Int) -> Unit, onResize: (Int, Int) -> Unit, onRemove: () -> Unit, modifier: Modifier) {
    @Composable fun B(label: String, color: Color = Color(0xFF263238), onClick: () -> Unit) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(color).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
            Text(label, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xEE101820)).padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { B("▲") { onMove(0, -1) } }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { B("◀") { onMove(-1, 0) }; B("✕", Color(0xFFB71C1C), onRemove); B("▶") { onMove(1, 0) } }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { B("▼") { onMove(0, 1) } }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            B("⇠", DriveColors.Track) { onResize(-1, 0) }; B("⇢", DriveColors.Track) { onResize(1, 0) }
            B("⇡", DriveColors.Track) { onResize(0, -1) }; B("⇣", DriveColors.Track) { onResize(0, 1) }
        }
    }
}

@Composable
private fun PickerFrame(title: String, onClose: () -> Unit, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xCC000000)).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
        Column(Modifier.fillMaxWidth(0.7f).fillMaxHeight(0.9f).clip(RoundedCornerShape(22.dp)).background(DriveStyle.panelBrush)
            .clickable(enabled = false) {}.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                BigButton("Fermer", DriveColors.Panel, onClose)
            }
            content()
        }
    }
}

@Composable
private fun ModulePicker(hasMap: Boolean, onPick: (ModuleType) -> Unit, onClose: () -> Unit) = PickerFrame("Ajouter un module", onClose) {
    if (hasMap) Text("La carte est déjà sur une page (une seule carte possible).", color = DriveColors.TextSecondary, fontSize = 14.sp)
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(ModuleType.entries.filter { it != ModuleType.WIDGET && !(hasMap && it == ModuleType.MAP) }) { t ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(DriveColors.Panel).clickable { onPick(t) }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(t.emoji, fontSize = 26.sp); Text("  ${t.label}", color = Color.White, fontSize = 19.sp)
            }
        }
    }
}

@Composable
private fun WidgetPicker(onPick: (AppWidgetProviderInfo) -> Unit, onClose: () -> Unit) = PickerFrame("Widgets Android", onClose) {
    val context = LocalContext.current
    val providers = remember { runCatching { Widgets.providers(context) }.getOrDefault(emptyList()) }
    if (providers.isEmpty()) Text("Aucune appli installée ne propose de widget.", color = DriveColors.TextSecondary, fontSize = 17.sp)
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(providers, key = { it.provider.flattenToString() }) { p ->
            val pm = context.packageManager
            val icon = remember(p) { runCatching { pm.getApplicationIcon(p.provider.packageName).toBitmap(64, 64).asImageBitmap() }.getOrNull() }
            val app = remember(p) { runCatching { pm.getApplicationLabel(pm.getApplicationInfo(p.provider.packageName, 0)).toString() }.getOrDefault("") }
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(DriveColors.Panel).clickable { onPick(p) }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) Image(icon, null, Modifier.size(40.dp))
                Column(Modifier.padding(start = 12.dp)) {
                    Text(p.loadLabel(pm), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(app, color = DriveColors.TextSecondary, fontSize = 14.sp)
                }
            }
        }
    }
}

/** Affiche un widget Android posé sur l'accueil. */
@Composable
fun AndroidWidget(widgetId: Int, modifier: Modifier) {
    val context = LocalContext.current
    val info = remember(widgetId) { AppWidgetManager.getInstance(context).getAppWidgetInfo(widgetId) }
    if (info == null) {
        Box(modifier.clip(RoundedCornerShape(22.dp)).background(DriveColors.Panel), contentAlignment = Alignment.Center) {
            Text("Widget indisponible (appli désinstallée ?)", color = DriveColors.TextSecondary, fontSize = 14.sp)
        }
        return
    }
    AndroidView(
        factory = { ctx -> Widgets.host(ctx).createView(ctx.applicationContext, widgetId, info).apply { setPadding(0, 0, 0, 0) } },
        modifier = modifier.clip(RoundedCornerShape(22.dp)),
    )
}

/** Module « ordinateur de bord » : trajet en cours en un coup d'œil. */
@Composable
fun TripModule(trip: fr.roadguard.core.TripComputer, modifier: Modifier) {
    val t = trip.trip
    Column(modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Trajet", color = DriveColors.TextSecondary, fontSize = 14.sp)
        Text(fr.roadguard.core.TripComputer.km(t.distanceM), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold, fontFamily = DriveStyle.Numbers)
        Text("${fr.roadguard.core.TripComputer.duration(t.movingS)} · moy. ${if (t.avgKmh > 0) "${t.avgKmh.toInt()} km/h" else "—"}",
            color = Color.White, fontSize = 16.sp)
        Text("Maxi ${t.maxKmh.toInt()} km/h · ${t.radars} radar(s)", color = DriveColors.TextSecondary, fontSize = 14.sp)
    }
}
