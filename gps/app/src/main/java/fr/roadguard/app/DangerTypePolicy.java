package fr.roadguard.app;

import java.util.Locale;

/** Shared label validation and radar classification for foreground and background GPS. */
final class DangerTypePolicy {
  private DangerTypePolicy() {}

  static boolean isValidLabel(String type) {
    if (type == null) return false;
    String value = type.trim();
    return !value.isEmpty() && value.length() <= 80;
  }

  static boolean isRadar(String type) {
    if (type == null) return false;
    String value = type.trim().toLowerCase(Locale.ROOT);
    return value.matches("et(f|d|t|u|vm|fr|pn)")
        || value.contains("radar")
        || value.contains("contrôle de vitesse")
        || value.contains("controle de vitesse")
        || value.contains("contrôle mobile")
        || value.contains("controle mobile")
        || value.contains("speed camera")
        || value.contains("zone dangereuse")
        || value.contains("zone de contrôle possible")
        || value.contains("passage à niveau")
        || value.contains("level crossing");
  }

  static boolean matchesFilter(String type, String filter) {
    if ("all".equals(filter)) return true;
    String value = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
    // A tunnel describes where the control is installed, while the remaining
    // words retain whether it is fixed, mobile, or an average-speed section.
    if ("tunnel".equals(filter)) return value.contains("tunnel") || value.contains("tunel");
    if ("redlight".equals(filter)) return value.contains("feu rouge") || value.contains("red light");
    if ("average".equals(filter)) return value.contains("vitesse moyenne") || value.contains("average speed") || value.contains("tronçon");
    if ("variable".equals(filter)) return value.contains("variable") || value.contains("adaptatif");
    if ("works".equals(filter)) return value.contains("chantier") || value.contains("travaux") || value.contains("construction");
    if ("crossing".equals(filter)) return value.contains("passage à niveau") || value.contains("level crossing");
    if ("danger".equals(filter)) return value.contains("zone dangereuse") || value.contains("zone radar") || value.contains("danger zone") || value.contains("zone de contrôle possible");
    if ("fixed".equals(filter)) {
      return value.contains("fixe") || value.contains("feu rouge")
          || value.contains("discriminant") || value.contains("tourelle")
          || value.contains("urbain");
    }
    if ("mobile".equals(filter)) return value.contains("mobile");
    return true;
  }
}
