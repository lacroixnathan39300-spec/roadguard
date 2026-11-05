package fr.roadguard.core.map

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import fr.roadguard.core.radar.Radar
import fr.roadguard.core.radar.RadarRepository
import fr.roadguard.core.radar.RadarKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.geojson.LineString
import fr.roadguard.core.nav.Route
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

/** Style vectoriel libre (OpenFreeMap, données © OpenStreetMap), sans clé. */
internal const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
/** Style de nuit (CARTO Dark Matter, données © OpenStreetMap, © CARTO). */
internal const val NIGHT_STYLE_URL = "https://basemaps.cartocdn.com/gl/dark-matter-gl-style/style.json"
private const val RADAR_SOURCE = "roadguard-radars"
private const val RADAR_LAYER = "roadguard-radars-layer"

/**
 * Carte native MapLibre : position du poste (suivi en mode conduite) et radars embarqués.
 * [hasLocation] : la permission de localisation est accordée.
 */
@Composable
fun DriveMap(
    hasLocation: Boolean,
    route: Route?,
    recenterRequest: Int,
    night: Boolean,
    visibleKinds: Set<RadarKind>,
    modifier: Modifier = Modifier,
    /** Pack hors ligne à utiliser (pas d'Internet), ou null pour la carte en ligne. */
    offlinePack: OfflinePack? = null,
    /** Variantes du trajet proposées au départ, dessinées en gris. */
    alternatives: List<Route> = emptyList(),
    /** Carte toujours orientée nord (sinon : dans le sens de la marche pendant le guidage). */
    northUp: Boolean = false,
    /** Vitesse actuelle, pour le zoom adaptatif (plus large quand on roule vite). */
    speedKmh: Int = 0,
    adaptiveZoom: Boolean = true,
    /**
     * Position à afficher, fournie par ROADGUARD (GPS du poste ou du téléphone par Link, cap complété).
     * La carte n'interroge plus le GPS elle-même : point et carte suivent exactement la même position que les alertes.
     */
    position: fr.roadguard.core.radar.Fix? = null,
    /** Signalements de la communauté et conducteurs visibles autour. */
    reports: List<fr.roadguard.core.community.Report> = emptyList(),
    drivers: List<fr.roadguard.core.community.Driver> = emptyList(),
) {
    val zoom = when {
        !adaptiveZoom -> if (route != null) 16.0 else 15.0
        speedKmh >= 100 -> 14.0
        speedKmh >= 70 -> 15.0
        speedKmh >= 40 -> 15.8
        else -> 16.5
    }
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember {
        MapLibre.getInstance(context)
        // Rendu dans une TextureView : la carte est dessinée comme une vue normale de l'écran.
        // Avec la surface séparée par défaut, certains postes (MediaTek) et téléphones affichaient
        // une zone blanche ou une ancienne image à la place de la carte.
        val options = org.maplibre.android.maps.MapLibreMapOptions.createFromAttributes(context)
            .textureMode(true).translucentTextureSurface(false)
        MapView(context, options).apply { onCreate(Bundle()) }
    }
    val holder = remember { MapHolder() }
    var styleVersion by remember { mutableStateOf(0) }
    /** Diagnostic : dernière erreur de chargement et heure du dernier style chargé. */
    var mapError by remember { mutableStateOf<String?>(null) }
    var styleLoadedAt by remember { mutableStateOf(0L) }
    var retry by remember { mutableStateOf(0) }
    androidx.compose.runtime.DisposableEffect(mapView) {
        val fail = MapView.OnDidFailLoadingMapListener { msg -> mapError = msg ?: "erreur inconnue" }
        mapView.addOnDidFailLoadingMapListener(fail)
        onDispose { mapView.removeOnDidFailLoadingMapListener(fail) }
    }
    var radarCollection by remember { mutableStateOf<FeatureCollection?>(null) }

    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    val radarVersion by RadarRepository.version.collectAsState()
    LaunchedEffect(radarVersion) {
        val radars = RadarRepository.radars(context)
        val collection = withContext(Dispatchers.Default) { radars.toFeatureCollection() }
        radarCollection = collection
        runCatching { holder.style?.getSourceAs<GeoJsonSource>(RADAR_SOURCE)?.setGeoJson(collection) }
    }

    LaunchedEffect(mapView) {
        mapView.getMapAsync { map ->
            holder.map = map
            map.uiSettings.isRotateGesturesEnabled = false
            // Carte épurée : logo et bouton d'information masqués (crédits OpenStreetMap dans Réglages → À propos).
            map.uiSettings.isLogoEnabled = false
            map.uiSettings.isAttributionEnabled = false
            map.uiSettings.isTiltGesturesEnabled = false
            map.cameraPosition = CameraPosition.Builder().target(LatLng(46.67, 5.55)).zoom(12.0).build()
            styleVersion = -1 // carte prête : le style est chargé par l'effet ci-dessous
        }
    }

    // Style jour/nuit ; à chaque changement, les couches ROADGUARD sont remises.
    LaunchedEffect(night, styleVersion < 0 || styleVersion > 0, radarCollection, offlinePack?.file?.path, retry) {
        val map = holder.map ?: return@LaunchedEffect
        val collection = radarCollection ?: return@LaunchedEffect
        // Sans aucun Internet : style de jour, celui des zones enregistrées et du cache (la nuit n'est pas enregistrée).
        val online = fr.roadguard.core.net.Net.hasDirectInternet() || fr.roadguard.core.net.Net.relay?.isConnected() == true
        val useNight = night && online
        val uri = offlinePack?.let { "offline:${it.file.path}:$night" } ?: if (useNight) NIGHT_STYLE_URL else STYLE_URL
        if (holder.styleUri == uri) return@LaunchedEffect
        holder.styleUri = uri
        val builder = offlinePack?.let { pack ->
            val json = withContext(Dispatchers.IO) { runCatching { OfflineMaps.style(context, pack, night) }.getOrNull() }
            json?.let { Style.Builder().fromJson(it) }
        } ?: Style.Builder().fromUri(if (useNight) NIGHT_STYLE_URL else STYLE_URL)
        // L'ancien style devient inutilisable dès ce moment : plus aucun accès jusqu'au nouveau.
        holder.style = null
        map.setStyle(builder) { style ->
            holder.style = style
            mapError = null
            styleLoadedAt = System.currentTimeMillis()
            style.addSource(GeoJsonSource(ROUTE_SOURCE))
            style.addSource(GeoJsonSource(ALT_SOURCE))
            style.addLayerBelow(altLayer(), firstSymbolLayer(style))
            style.addLayerAbove(routeCasingLayer(), ALT_LAYER)
            style.addLayerAbove(routeLayer(), ROUTE_CASING_LAYER)
            style.addSource(GeoJsonSource(RADAR_SOURCE, collection))
            style.addLayer(radarLayer())
            style.addSource(GeoJsonSource(COMMUNITY_SOURCE))
            style.addLayer(communityLayer())
            style.addSource(GeoJsonSource(DRIVERS_SOURCE))
            style.addLayer(driversLayer())
            style.addImage(ME_ICON, meArrow(fr.roadguard.core.ui.DriveColors.Accent.toArgb()))
            style.addSource(GeoJsonSource(ME_SOURCE))
            style.addLayer(SymbolLayer(ME_SOURCE, ME_SOURCE).withProperties(
                PropertyFactory.iconImage(ME_ICON), PropertyFactory.iconRotate(Expression.get("bearing")),
                PropertyFactory.iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP),
                PropertyFactory.iconAllowOverlap(true), PropertyFactory.iconIgnorePlacement(true),
                PropertyFactory.iconSize(Expression.interpolate(Expression.linear(), Expression.zoom(), Expression.stop(8, 0.55f), Expression.stop(16, 1f))),
            ))
            if (hasLocation) enableLocation(context, map, style, holder)
            styleVersion = if (styleVersion < 0) 1 else styleVersion + 1
        }
    }

    LaunchedEffect(hasLocation) {
        val map = holder.map
        val style = holder.style
        if (hasLocation && map != null && style != null) runCatching { enableLocation(context, map, style, holder) }
    }

    // Trajet : tracé sur la carte et carte orientée dans le sens de la marche pendant le guidage.
    LaunchedEffect(route, styleVersion) {
        val style = holder.style ?: return@LaunchedEffect
        val source = runCatching { style.getSourceAs<GeoJsonSource>(ROUTE_SOURCE) }.getOrNull() ?: return@LaunchedEffect
        runCatching {
            if (route == null) {
                source.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
            } else {
                val line = LineString.fromLngLats(route.points.map { Point.fromLngLat(it.lon, it.lat) })
                source.setGeoJson(Feature.fromGeometry(line))
            }
        }
        holder.map?.locationComponent?.takeIf { it.isLocationComponentActivated }?.let {
            it.cameraMode = if (route != null && !northUp) CameraMode.TRACKING_GPS else CameraMode.TRACKING_GPS_NORTH
            it.zoomWhileTracking(zoom)
        }
    }

    // Variantes : toutes sauf celle suivie, en gris sous le trajet.
    LaunchedEffect(alternatives, route, styleVersion) {
        val style = holder.style ?: return@LaunchedEffect
        runCatching {
            val lines = alternatives.filter { it !== route }.map { r ->
                Feature.fromGeometry(LineString.fromLngLats(r.points.map { Point.fromLngLat(it.lon, it.lat) }))
            }
            style.getSourceAs<GeoJsonSource>(ALT_SOURCE)?.setGeoJson(FeatureCollection.fromFeatures(lines))
        }
    }

    // Signalements et conducteurs (mis à jour toutes les minutes en roulant).
    LaunchedEffect(reports, drivers, styleVersion) {
        val style = holder.style ?: return@LaunchedEffect
        runCatching {
            style.getSourceAs<GeoJsonSource>(COMMUNITY_SOURCE)?.setGeoJson(FeatureCollection.fromFeatures(reports.map { r ->
                Feature.fromGeometry(Point.fromLngLat(r.lon, r.lat)).apply {
                    addStringProperty("color", String.format("#%06X", r.type.color and 0xFFFFFF))
                    addBooleanProperty("permanent", r.permanent)
                }
            }))
            style.getSourceAs<GeoJsonSource>(DRIVERS_SOURCE)?.setGeoJson(FeatureCollection.fromFeatures(drivers.map { d ->
                Feature.fromGeometry(Point.fromLngLat(d.lon, d.lat))
            }))
        }
    }

    // Types de radars affichés (réglages).
    LaunchedEffect(visibleKinds, styleVersion) {
        val layer = runCatching { holder.style?.getLayerAs<CircleLayer>(RADAR_LAYER) }.getOrNull() ?: return@LaunchedEffect
        val stops = RadarKind.entries.map { Expression.stop(it.name, Expression.literal(it in visibleKinds)) }.toTypedArray()
        runCatching { layer.setFilter(Expression.match(Expression.get("kind"), Expression.literal(false), *stops)) }
    }

    // Zoom adaptatif et orientation : appliqués seulement si la carte suit encore la position.
    LaunchedEffect(zoom, northUp) {
        holder.map?.locationComponent?.takeIf { it.isLocationComponentActivated && it.cameraMode != CameraMode.NONE }?.let {
            it.cameraMode = if (route != null && !northUp) CameraMode.TRACKING_GPS else CameraMode.TRACKING_GPS_NORTH
            it.zoomWhileTracking(zoom, 1200)
        }
    }

    LaunchedEffect(position, styleVersion) {
        val p = position ?: return@LaunchedEffect
        val comp = holder.map?.locationComponent?.takeIf { it.isLocationComponentActivated } ?: return@LaunchedEffect
        val loc = android.location.Location("roadguard").apply {
            latitude = p.lat; longitude = p.lon
            // Horloge actuelle : sinon le point peut être affiché « périmé » (gris) avec une horloge GPS décalée.
            time = System.currentTimeMillis(); elapsedRealtimeNanos = android.os.SystemClock.elapsedRealtimeNanos()
            accuracy = p.accuracyM.toFloat()
            if (p.speedKmh > 0) speed = (p.speedKmh / 3.6).toFloat()
            if (!p.bearing.isNaN()) bearing = p.bearing.toFloat()
        }
        runCatching { comp.forceLocationUpdate(loc) }
        hidePuck(holder.style)
        // La flèche suit ensuite la position animée de la carte (voir enableLocation) : mouvement fluide.
        // Flèche glissée jusqu'à la nouvelle position (même durée que l'animation de la caméra) : pas de sauts.
        holder.animateMe(p.lat, p.lon, if (p.bearing.isNaN()) Double.NaN else p.bearing)
        // Trajet effacé derrière la voiture : on ne dessine que ce qui reste à parcourir.
        val r = route
        if (r != null && r.points.size >= 2) {
            val pts = r.points
            if (holder.trimRoute !== r) { holder.trimRoute = r; holder.trimIndex = 0 }
            var best = holder.trimIndex; var bestD = Double.MAX_VALUE
            val from = (holder.trimIndex - 3).coerceAtLeast(0); val to = (holder.trimIndex + 300).coerceAtMost(pts.size - 1)
            for (i in from..to) {
                val d = fr.roadguard.core.radar.Geo.distanceMeters(p.lat, p.lon, pts[i].lat, pts[i].lon)
                if (d < bestD) { bestD = d; best = i }
            }
            if (bestD < 80) holder.trimIndex = best
            val remaining = listOf(Point.fromLngLat(p.lon, p.lat)) +
                pts.subList((holder.trimIndex + 1).coerceAtMost(pts.size - 1), pts.size).map { Point.fromLngLat(it.lon, it.lat) }
            if (remaining.size >= 2) runCatching {
                holder.style?.getSourceAs<GeoJsonSource>(ROUTE_SOURCE)?.setGeoJson(Feature.fromGeometry(LineString.fromLngLats(remaining)))
            }
        }
    }

    // Bouton « recentrer » : reprendre le suivi après un déplacement de la carte au doigt.
    LaunchedEffect(recenterRequest) {
        if (recenterRequest == 0) return@LaunchedEffect
        holder.map?.locationComponent?.takeIf { it.isLocationComponentActivated }?.let {
            it.cameraMode = if (route != null && !northUp) CameraMode.TRACKING_GPS else CameraMode.TRACKING_GPS_NORTH
            it.zoomWhileTracking(zoom)
        }
    }

    // Style pas chargé après 12 s (réseau lent ou coupé) : nouvel essai, et explication à l'écran.
    LaunchedEffect(styleVersion, retry, mapError) {
        kotlinx.coroutines.delay(12_000)
        if (holder.style == null || mapError != null) { holder.styleUri = null; retry++ }
    }

    androidx.compose.foundation.layout.Box(modifier) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
        if (holder.style == null && retry > 0 || mapError != null) {
            val direct = fr.roadguard.core.net.Net.hasDirectInternet()
            val s = fr.roadguard.core.net.NetStats
            androidx.compose.material3.Text(
                "Carte non chargée" + (mapError?.let { " : $it" } ?: "") +
                    "\nInternet direct : ${if (direct) "oui" else "non"} · par le téléphone : ${s.relayOk.get()} ok / ${s.relayFail.get()} échecs" +
                    (s.lastError?.let { " ($it)" } ?: "") + " · essai ${retry + 1}",
                color = androidx.compose.ui.graphics.Color.White, fontSize = androidx.compose.ui.unit.TextUnit(13f, androidx.compose.ui.unit.TextUnitType.Sp),
                modifier = Modifier.align(androidx.compose.ui.Alignment.BottomStart).padding(8.dp)
                    .background(androidx.compose.ui.graphics.Color(0xE0101820), androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}

private class MapHolder {
    var map: MapLibreMap? = null
    var style: Style? = null
    var styleUri: String? = null
    var lastBearing = 0.0
    var trimRoute: Route? = null
    var trimIndex = 0
    private var meLat = Double.NaN; private var meLon = 0.0; private var lastDraw = 0L

    /** Dessine la flèche ROADGUARD (au plus 30 fois par seconde, pour ménager le poste). */
    fun drawMe(lat: Double, lon: Double, bearing: Double, force: Boolean = false) {
        meLat = lat; meLon = lon; if (!bearing.isNaN()) lastBearing = bearing
        val now = android.os.SystemClock.uptimeMillis()
        if (!force && now - lastDraw < 33) return
        lastDraw = now
        runCatching {
            style?.getSourceAs<GeoJsonSource>(ME_SOURCE)?.setGeoJson(
                Feature.fromGeometry(Point.fromLngLat(meLon, meLat)).apply { addNumberProperty("bearing", lastBearing) })
        }
    }

    private var animator: android.animation.ValueAnimator? = null
    private var lastFixAt = 0L

    /** Animation linéaire de la flèche vers la nouvelle position et le nouveau cap (chemin le plus court). */
    fun animateMe(lat: Double, lon: Double, bearing: Double) {
        val now = android.os.SystemClock.uptimeMillis()
        val duration = (now - lastFixAt).coerceIn(300L, 1200L)
        lastFixAt = now
        animator?.cancel()
        if (meLat.isNaN()) { drawMe(lat, lon, bearing); return }
        val fromLat = meLat; val fromLon = meLon; val fromB = lastBearing
        val toB = if (bearing.isNaN()) fromB else bearing
        val delta = ((toB - fromB + 540.0) % 360.0) - 180.0
        animator = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            interpolator = android.view.animation.LinearInterpolator()
            addUpdateListener { a ->
                val t = (a.animatedValue as Float).toDouble()
                drawMe(fromLat + (lat - fromLat) * t, fromLon + (lon - fromLon) * t, (fromB + delta * t + 360.0) % 360.0, force = t >= 1.0)
            }
            start()
        }
    }
}

@SuppressLint("MissingPermission")
private fun enableLocation(context: Context, map: MapLibreMap, style: Style, holder: MapHolder? = null) {
    val component = map.locationComponent
    if (component.isLocationComponentActivated) return
    component.activateLocationComponent(
        LocationComponentActivationOptions.builder(context, style).useDefaultLocationEngine(false)
            // Point MapLibre rendu invisible : la flèche ROADGUARD (couche « rg-me ») montre le vrai sens de marche.
            .locationComponentOptions(
                org.maplibre.android.location.LocationComponentOptions.builder(context)
                    .foregroundTintColor(android.graphics.Color.TRANSPARENT).backgroundTintColor(android.graphics.Color.TRANSPARENT)
                    .foregroundStaleTintColor(android.graphics.Color.TRANSPARENT).backgroundStaleTintColor(android.graphics.Color.TRANSPARENT)
                    .bearingTintColor(android.graphics.Color.TRANSPARENT).accuracyAlpha(0f).elevation(0f).build()
            ).build()
    )
    component.isLocationComponentEnabled = true
    component.cameraMode = CameraMode.TRACKING_GPS_NORTH
    component.renderMode = RenderMode.NORMAL
    component.zoomWhileTracking(15.0)
    hidePuck(style)
}

/**
 * Le point MapLibre (sa propre flèche, mal orientée sur certains téléphones) est masqué : il passait
 * au-dessus de la flèche ROADGUARD « rg-me ». MapLibre réaffiche parfois ses couches : appelé à chaque position.
 */
private fun hidePuck(style: Style?) {
    val st = style ?: return
    runCatching {
        for (layer in st.layers) {
            val id = layer.id
            if (id != ME_SOURCE && (id.contains("location", ignoreCase = true) || id.contains("puck", ignoreCase = true)) &&
                layer.visibility.value != Property.NONE) layer.setProperties(PropertyFactory.visibility(Property.NONE))
        }
    }
}

private const val ROUTE_SOURCE = "roadguard-route"
private const val ME_SOURCE = "rg-me"
private const val ME_ICON = "rg-me-arrow"

/** Flèche de position : rond blanc, flèche de la couleur d'accent pointée vers le haut (cap 0 = nord). */
private fun meArrow(color: Int): android.graphics.Bitmap {
    val s = 96
    val b = android.graphics.Bitmap.createBitmap(s, s, android.graphics.Bitmap.Config.ARGB_8888)
    val c = android.graphics.Canvas(b)
    val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    p.color = android.graphics.Color.argb(70, 0, 0, 0); c.drawCircle(s / 2f, s / 2f + 3, s * 0.42f, p)
    p.color = android.graphics.Color.WHITE; c.drawCircle(s / 2f, s / 2f, s * 0.40f, p)
    p.color = color
    val path = android.graphics.Path().apply {
        moveTo(s * 0.5f, s * 0.18f); lineTo(s * 0.74f, s * 0.76f); lineTo(s * 0.5f, s * 0.62f); lineTo(s * 0.26f, s * 0.76f); close()
    }
    c.drawPath(path, p)
    return b
}
private const val COMMUNITY_SOURCE = "roadguard-community"
private const val DRIVERS_SOURCE = "roadguard-drivers"

/** Signalements : pastille de la couleur du type, contour blanc épais. */
private fun communityLayer() = CircleLayer(COMMUNITY_SOURCE, COMMUNITY_SOURCE).withProperties(
    PropertyFactory.circleRadius(Expression.interpolate(Expression.linear(), Expression.zoom(), Expression.stop(9, 4f), Expression.stop(16, 11f))),
    PropertyFactory.circleColor(Expression.toColor(Expression.get("color"))),
    PropertyFactory.circleStrokeColor(android.graphics.Color.WHITE),
    PropertyFactory.circleStrokeWidth(3f),
).apply { minZoom = 9f }

/** Autres conducteurs ROADGUARD visibles : petit rond violet. */
private fun driversLayer() = CircleLayer(DRIVERS_SOURCE, DRIVERS_SOURCE).withProperties(
    PropertyFactory.circleRadius(7f),
    PropertyFactory.circleColor(android.graphics.Color.parseColor("#7C4DFF")),
    PropertyFactory.circleStrokeColor(android.graphics.Color.WHITE),
    PropertyFactory.circleStrokeWidth(2f),
).apply { minZoom = 9f }
private const val ALT_SOURCE = "roadguard-route-alternatives"
private const val ALT_LAYER = "roadguard-route-alternatives-layer"

private fun altLayer() = LineLayer(ALT_LAYER, ALT_SOURCE).withProperties(
    PropertyFactory.lineColor(android.graphics.Color.parseColor("#8A97A6")),
    PropertyFactory.lineOpacity(0.85f),
    PropertyFactory.lineWidth(Expression.interpolate(Expression.linear(), Expression.zoom(), Expression.stop(10, 4f), Expression.stop(17, 10f))),
    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
)
private const val ROUTE_LAYER = "roadguard-route-layer"
private const val ROUTE_CASING_LAYER = "roadguard-route-casing"

/** Le trajet passe sous les noms de rues et les radars. */
private fun firstSymbolLayer(style: Style): String =
    style.layers.firstOrNull { it is SymbolLayer }?.id ?: style.layers.last().id

private fun routeCasingLayer() = LineLayer(ROUTE_CASING_LAYER, ROUTE_SOURCE).withProperties(
    PropertyFactory.lineColor(android.graphics.Color.parseColor("#0B4F8A")),
    PropertyFactory.lineWidth(Expression.interpolate(Expression.linear(), Expression.zoom(), Expression.stop(10, 6f), Expression.stop(17, 16f))),
    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
)

private fun routeLayer() = LineLayer(ROUTE_LAYER, ROUTE_SOURCE).withProperties(
    PropertyFactory.lineColor(android.graphics.Color.parseColor("#1E9BFF")),
    PropertyFactory.lineWidth(Expression.interpolate(Expression.linear(), Expression.zoom(), Expression.stop(10, 4f), Expression.stop(17, 11f))),
    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
)

private fun radarLayer(): CircleLayer = CircleLayer(RADAR_LAYER, RADAR_SOURCE).withProperties(
    PropertyFactory.circleRadius(
        Expression.interpolate(Expression.linear(), Expression.zoom(), Expression.stop(8, 2f), Expression.stop(15, 7f))
    ),
    PropertyFactory.circleColor(
        Expression.match(
            Expression.get("kind"), Expression.color(android.graphics.Color.parseColor("#FF3B30")),
            Expression.stop(RadarKind.REDLIGHT.name, Expression.color(android.graphics.Color.parseColor("#FF9500"))),
            Expression.stop(RadarKind.AVERAGE.name, Expression.color(android.graphics.Color.parseColor("#AF52DE"))),
            Expression.stop(RadarKind.MOBILE.name, Expression.color(android.graphics.Color.parseColor("#FFCC00"))),
        )
    ),
    PropertyFactory.circleStrokeColor(android.graphics.Color.WHITE),
    PropertyFactory.circleStrokeWidth(1.5f),
).apply { minZoom = 8f }

private fun List<Radar>.toFeatureCollection(): FeatureCollection = FeatureCollection.fromFeatures(
    map { r ->
        Feature.fromGeometry(Point.fromLngLat(r.lon, r.lat)).apply {
            addStringProperty("kind", r.kind.name)
            addStringProperty("id", r.id)
        }
    }
)
