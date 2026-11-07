package fr.roadguard.app.car

import android.app.Presentation
import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import fr.roadguard.core.DrivingSession
import fr.roadguard.core.map.NIGHT_STYLE_URL
import fr.roadguard.core.map.STYLE_URL
import fr.roadguard.core.radar.AlertText
import fr.roadguard.core.radar.RadarKind
import fr.roadguard.core.radar.RadarRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

/**
 * Carte de l'écran voiture : une vraie carte MapLibre dessinée sur la surface fournie par Android Auto
 * (écran virtuel), avec trajet, radars, position et bandeau d'alerte radar.
 */
class CarMapRenderer(private val context: Context, private val scope: CoroutineScope) : SurfaceCallback {
    private var display: VirtualDisplay? = null
    private var presentation: Presentation? = null
    private var mapView: MapView? = null
    private var map: MapLibreMap? = null
    private var style: Style? = null
    private var styleNight: Boolean? = null
    private var banner: TextView? = null
    private var lastRoute: Any? = null
    /** Couche radars affichée (null : à régler après chaque changement de style). */
    private var lastRadarsVisible: Boolean? = null
    private var zoom = 16.0
    /** Haut de la zone visible (sous les cartes du système) : la position est placée plus bas. */
    private var visibleTop = 0

    override fun onSurfaceAvailable(container: SurfaceContainer) {
        val surface = container.surface ?: return
        release()
        MapLibre.getInstance(context)
        val dm = context.getSystemService(DisplayManager::class.java)
        val vd = dm.createVirtualDisplay("roadguard-auto", container.width, container.height, container.dpi, surface,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY)
        display = vd
        val p = Presentation(context, vd.display)
        val root = FrameLayout(p.context)
        val mv = MapView(p.context)
        root.addView(mv, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        val b = TextView(p.context).apply {
            textSize = 26f; setTextColor(android.graphics.Color.WHITE); setPadding(32, 18, 32, 18)
            setBackgroundColor(android.graphics.Color.parseColor("#E6B71C1C")); visibility = android.view.View.GONE
        }
        root.addView(b, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.START).apply { setMargins(24, 24, 24, 24) })
        banner = b
        p.setContentView(root)
        runCatching { p.show() }
        presentation = p
        mapView = mv
        mv.onCreate(null); mv.onStart(); mv.onResume()
        mv.getMapAsync { m ->
            map = m
            m.uiSettings.setAllGesturesEnabled(false)
            m.uiSettings.isAttributionEnabled = false
            m.uiSettings.isLogoEnabled = false
            m.cameraPosition = CameraPosition.Builder().target(LatLng(46.67, 5.55)).zoom(zoom).build()
        }
    }

    override fun onVisibleAreaChanged(visibleArea: android.graphics.Rect) { visibleTop = visibleArea.top }

    override fun onScale(focusX: Float, focusY: Float, scaleFactor: Float) {
        zoom = (zoom + Math.log(scaleFactor.toDouble()) / Math.log(2.0)).coerceIn(10.0, 18.0)
    }

    override fun onSurfaceDestroyed(container: SurfaceContainer) = release()

    fun zoomBy(delta: Double) { zoom = (zoom + delta).coerceIn(10.0, 18.0) }

    /** Mise à jour depuis l'état de conduite (appelée chaque seconde). */
    fun update(s: DrivingSession?) {
        val m = map ?: return
        val night = s?.night ?: false
        if (styleNight != night) {
            styleNight = night; style = null; lastRoute = null; lastRadarsVisible = null
            m.setStyle(Style.Builder().fromUri(if (night) NIGHT_STYLE_URL else STYLE_URL)) { st ->
                st.addSource(GeoJsonSource(ROUTE))
                st.addLayer(LineLayer(ROUTE, ROUTE).withProperties(
                    PropertyFactory.lineColor(android.graphics.Color.parseColor("#1E9BFF")), PropertyFactory.lineWidth(9f),
                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND), PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)))
                st.addSource(GeoJsonSource(RADARS))
                st.addLayer(CircleLayer(RADARS, RADARS).withProperties(
                    PropertyFactory.circleRadius(8f), PropertyFactory.circleColor(android.graphics.Color.parseColor("#FF3B30")),
                    PropertyFactory.circleStrokeColor(android.graphics.Color.WHITE), PropertyFactory.circleStrokeWidth(2f)))
                st.addSource(GeoJsonSource(ME))
                st.addLayer(CircleLayer(ME, ME).withProperties(
                    PropertyFactory.circleRadius(11f), PropertyFactory.circleColor(android.graphics.Color.parseColor("#00B8FF")),
                    PropertyFactory.circleStrokeColor(android.graphics.Color.WHITE), PropertyFactory.circleStrokeWidth(3f)))
                style = st
                scope.launch { loadRadars() }
            }
        }
        val st = style ?: return
        val here = s?.here
        // Points radars sur la carte : seulement si l'utilisateur a activé lui-même les alertes radars.
        val radarsVisible = s?.settings?.radarsOn == true
        if (radarsVisible != lastRadarsVisible) runCatching {
            st.getLayer(RADARS)?.setProperties(PropertyFactory.visibility(if (radarsVisible) Property.VISIBLE else Property.NONE))
            lastRadarsVisible = radarsVisible
        }
        runCatching {
            if (s?.route !== lastRoute) {
                lastRoute = s?.route
                val line = s?.route?.points?.map { Point.fromLngLat(it.lon, it.lat) }
                st.getSourceAs<GeoJsonSource>(ROUTE)?.setGeoJson(
                    if (line == null || line.size < 2) FeatureCollection.fromFeatures(emptyList())
                    else FeatureCollection.fromFeature(Feature.fromGeometry(LineString.fromLngLats(line))))
            }
            if (here != null) {
                st.getSourceAs<GeoJsonSource>(ME)?.setGeoJson(Point.fromLngLat(here.lon, here.lat))
                val navigating = s.route != null
                val bearing = if (navigating && !here.bearing.isNaN()) here.bearing else 0.0
                m.cameraPosition = CameraPosition.Builder().target(LatLng(here.lat, here.lon)).zoom(zoom)
                    .bearing(bearing).tilt(if (navigating) 45.0 else 0.0)
                    .padding(0.0, visibleTop.toDouble() + (mapView?.height ?: 0) * 0.35, 0.0, 0.0).build()
            }
        }
        val alert = s?.alert
        banner?.let { b ->
            if (alert == null) b.visibility = android.view.View.GONE
            else {
                b.text = "${AlertText.kindLabel(alert.radar.kind)}${alert.radar.speedKmh?.let { " $it" } ?: ""} · ${alert.distanceM} m"
                b.visibility = android.view.View.VISIBLE
            }
        }
    }

    private suspend fun loadRadars() {
        val radars = RadarRepository.radars(context).filter { it.kind != RadarKind.OTHER }
        val fc = FeatureCollection.fromFeatures(radars.map { Feature.fromGeometry(Point.fromLngLat(it.lon, it.lat)) })
        runCatching { style?.getSourceAs<GeoJsonSource>(RADARS)?.setGeoJson(fc) }
    }

    fun release() {
        mapView?.let { runCatching { it.onPause(); it.onStop(); it.onDestroy() } }
        runCatching { presentation?.dismiss() }
        display?.release()
        mapView = null; map = null; style = null; styleNight = null; presentation = null; display = null; banner = null
    }

    private companion object {
        const val ROUTE = "car-route"
        const val RADARS = "car-radars"
        const val ME = "car-me"
    }
}
