from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
JAVA=(ROOT/'app/src/main/java/fr/roadguard/app/MainActivity.java').read_text()
# The map cockpit contains an actual editable field and both keyboard/button submit actions.
assert 'destinationSearchInput=new EditText(this)' in JAVA
assert 'EditorInfo.IME_ACTION_SEARCH' in JAVA
assert 'searchDestinationFromBar();return true;' in JAVA
assert 'button("➜",()->searchDestinationFromBar())' in JAVA
# Suggestions are available both in the map drive sheet and the search dialog during guidance.
assert 'driveSearchResults=column()' in JAVA
assert 'showDriveSearchAddresses(found,sourceIgn)' in JAVA
assert 'TextView result=button("⌖  "+resultLabel,()->selectDriveSearchResult(a,resultLabel))' in JAVA
select=JAVA[ JAVA.index('void selectDriveSearchResult('):JAVA.index('// Three positions: closed') ]
assert 'selected="Carte"' in select and 'render()' in select
assert 'destinationSearchDialog.dismiss()' in select
run=JAVA[JAVA.index('void runDriveSearch('):JAVA.index('void showDriveSearchAddresses(')]
assert 'render()' not in run  # typing/results never rebuild the map; selection does
assert 'putString("destination_label",label)' in select
assert 'calculateRoadRoute()' in select
# Coordinates can be entered directly in the same search field.
assert 'query.split("[,;\\\\s]+",-1)' in JAVA
assert 'https://data.geopf.fr/geocodage/completion/?text=' in JAVA
assert 'maximumResponses=5' in JAVA
assert 'fetchFrenchAutocomplete(query)' in JAVA
assert 'optString("fulltext"' in JAVA
assert 'getFromLocationName(query,5)' in JAVA
assert 'driveSearchHandler.postDelayed(driveSearchRunnable,750)' in JAVA
assert 'Recherche automatique…' in JAVA
assert 'rankDriveSearchAddresses(response,query)' in JAVA
assert 'normalizeDriveSearchText' in JAVA
assert 'token!=driveSearchRequestToken' in JAVA
assert 'driveSearchSuppressWatcher=true' in JAVA
assert 'Aucun résultat pertinent' in JAVA
# Previous button behavior that switched to the Trajet page is gone.
assert 'TextView destination=button("⌕    Où va-t-on ?",()->{selected="Trajet";render();});' not in JAVA
print('Recherche destination intégrée à la carte : saisie, clavier, résultats, coordonnées et itinéraire vérifiés')
