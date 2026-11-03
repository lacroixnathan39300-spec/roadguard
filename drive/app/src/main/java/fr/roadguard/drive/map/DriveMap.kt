package fr.roadguard.drive.map

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import fr.roadguard.drive.radar.Radar
import fr.roadguard.drive.radar.RadarRepository
import fr.roadguard.drive.radar.RadarKind
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
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

/** Style vectoriel libre (OpenFreeMap, données © OpenStreetMap), sans clé. */
private const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
private const val RADAR_SOURCE = "roadguard-radars"
private const val RADAR_LAYER = "roadguard-radars-layer"

/**
 * Carte native MapLibre : position du poste (suivi en mode conduite) et radars embarqués.
 * [hasLocation] : la permission de localisation est accordée.
 */
@Composable
fun DriveMap(hasLocation: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply { onCreate(Bundle()) }
    }
    val holder = remember { MapHolder() }

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

    LaunchedEffect(mapView) {
        val radars = RadarRepository.radars(context)
        val collection = withContext(Dispatchers.Default) { radars.toFeatureCollection() }
        mapView.getMapAsync { map ->
            holder.map = map
            map.uiSettings.isRotateGesturesEnabled = false
            map.uiSettings.isTiltGesturesEnabled = false
            map.cameraPosition = CameraPosition.Builder().target(LatLng(46.67, 5.55)).zoom(12.0).build()
            map.setStyle(Style.Builder().fromUri(STYLE_URL)) { style ->
                holder.style = style
                style.addSource(GeoJsonSource(RADAR_SOURCE, collection))
                style.addLayer(radarLayer())
                if (hasLocation) enableLocation(context, map, style)
            }
        }
    }

    LaunchedEffect(hasLocation) {
        val map = holder.map
        val style = holder.style
        if (hasLocation && map != null && style != null) enableLocation(context, map, style)
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

private class MapHolder {
    var map: MapLibreMap? = null
    var style: Style? = null
}

@SuppressLint("MissingPermission")
private fun enableLocation(context: Context, map: MapLibreMap, style: Style) {
    val component = map.locationComponent
    if (component.isLocationComponentActivated) return
    component.activateLocationComponent(
        LocationComponentActivationOptions.builder(context, style).useDefaultLocationEngine(true).build()
    )
    component.isLocationComponentEnabled = true
    component.cameraMode = CameraMode.TRACKING_GPS
    component.renderMode = RenderMode.GPS
    component.zoomWhileTracking(15.0)
}

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
