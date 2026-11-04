package fr.roadguard.app.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.SearchTemplate
import androidx.car.app.model.Template
import androidx.lifecycle.lifecycleScope
import fr.roadguard.app.next.DriveService
import fr.roadguard.core.nav.LatLon
import fr.roadguard.core.nav.NavApi
import fr.roadguard.core.nav.Place
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** « Où aller ? » : Maison, Travail, destinations récentes, et recherche. */
class CarDestinationScreen(carContext: CarContext) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        val s = DriveService.session
        val list = ItemList.Builder()
        val places = buildList {
            s?.home?.let { add("🏠 Maison" to it) }
            s?.work?.let { add("💼 Travail" to it) }
            s?.favorites.orEmpty().forEach { add("★ ${it.name}" to it) }
            s?.recents.orEmpty().filterNot { r -> s?.favorites.orEmpty().any { it.name == r.name } }.forEach { add(it.name to it) }
        }
        // Android Auto limite la longueur des listes (souvent 6) : on garde les premiers.
        places.take(6).forEach { (title, place) -> list.addItem(placeRow(title, place)) }
        if (places.isEmpty()) list.setNoItemsMessage("Aucune destination récente. Utilise la recherche.")
        return ListTemplate.Builder()
            .setTitle("Où aller ?")
            .setHeaderAction(Action.BACK)
            .setActionStrip(ActionStrip.Builder().addAction(
                Action.Builder().setTitle("Rechercher").setOnClickListener { screenManager.push(CarSearchScreen(carContext)) }.build()
            ).build())
            .setSingleList(list.build())
            .build()
    }

    private fun placeRow(title: String, place: Place) = Row.Builder()
        .setTitle(title)
        .apply { if (place.detail.isNotBlank()) addText(place.detail) else if (title != place.name) addText(place.name) }
        .setOnClickListener { go(this, place) }
        .build()

    companion object {
        /** Lance le guidage et revient à la carte. */
        fun go(screen: Screen, place: Place) {
            DriveService.session?.startNavigation(place)
            screen.screenManager.popToRoot()
        }
    }
}

/** Recherche d'adresse (clavier de la voiture, souvent bloqué en roulant : la saisie vocale reste possible). */
class CarSearchScreen(carContext: CarContext) : Screen(carContext) {
    private var results: List<Place> = emptyList()
    private var loading = false
    private var job: Job? = null

    override fun onGetTemplate(): Template {
        val list = ItemList.Builder()
        results.forEach { p ->
            list.addItem(Row.Builder().setTitle(p.name).apply { if (p.detail.isNotBlank()) addText(p.detail) }
                .setOnClickListener { CarDestinationScreen.go(this, p) }.build())
        }
        if (results.isEmpty()) list.setNoItemsMessage("Tape ou dicte une adresse")
        return SearchTemplate.Builder(object : SearchTemplate.SearchCallback {
            override fun onSearchTextChanged(searchText: String) = search(searchText, 600)
            override fun onSearchSubmitted(searchText: String) = search(searchText, 0)
        })
            .setHeaderAction(Action.BACK)
            .setSearchHint("Adresse, ville, lieu…")
            .setShowKeyboardByDefault(true)
            .setLoading(loading)
            .apply { if (!loading) setItemList(list.build()) }
            .build()
    }

    private fun search(text: String, debounceMs: Long) {
        job?.cancel()
        if (text.trim().length < 3) return
        job = lifecycleScope.launch {
            delay(debounceMs)
            loading = true; invalidate()
            val here = DriveService.session?.here?.let { LatLon(it.lat, it.lon) }
            results = runCatching { NavApi.search(text, here) }.getOrDefault(emptyList()).take(6)
            loading = false; invalidate()
        }
    }
}
