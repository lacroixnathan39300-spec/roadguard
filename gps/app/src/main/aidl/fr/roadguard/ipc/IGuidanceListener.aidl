package fr.roadguard.ipc;
/** Full bounded snapshot; no bitmaps or raw map packs. */
oneway interface IGuidanceListener { void onSnapshot(String json); }
