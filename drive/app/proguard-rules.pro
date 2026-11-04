# Règles R8 : rien de spécifique pour l'instant.
# Itinéraires hors ligne : GraphHopper charge ses encodeurs et traductions par réflexion.
-keep class com.graphhopper.** { *; }
-keep class com.carrotsearch.hppc.** { *; }
-keep class com.fasterxml.jackson.** { *; }
-dontwarn com.graphhopper.**
-dontwarn org.codehaus.janino.**
-dontwarn org.openstreetmap.osmosis.**
-dontwarn org.apache.xmlgraphics.**
-dontwarn javax.**
-dontwarn java.awt.**
-dontwarn java.beans.**
-dontwarn org.slf4j.**
-dontwarn com.fasterxml.jackson.**
-dontwarn org.locationtech.jts.**
-dontwarn de.westnordost.**
-dontwarn org.codehaus.commons.**
