package fr.roadguard.app;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/** Bounded geometry for the paired car. No Android dependency. */
final class PhoneRouteGeometry {
  static final int MAX_INPUT_POINTS = 15000;
  static final int MAX_POINTS = 1500;

  static List<double[]> bounded(List<double[]> input) {
    if (input == null || input.size() < 2 || input.size() > MAX_INPUT_POINTS)
      throw new IllegalArgumentException("Invalid route size");
    // Validate all points, including those that thinning will skip.
    for (double[] p : input)
      if (p == null || p.length != 2 || !Double.isFinite(p[0]) || !Double.isFinite(p[1]) ||
          Math.abs(p[0]) > 90 || Math.abs(p[1]) > 180)
        throw new IllegalArgumentException("Invalid route coordinate");
    int count = Math.min(MAX_POINTS, input.size());
    List<double[]> result = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      int index = (int) ((long) i * (input.size() - 1) / (count - 1));
      result.add(input.get(index).clone());
    }
    return result;
  }

  /** Identifies exactly the cached route, not an address or a screen coordinate. */
  static String identity(String snapshot, String destination, String label) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      for (String value : new String[]{snapshot, destination, label}) {
        digest.update(value.getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0);
      }
      StringBuilder hex = new StringBuilder("phone-");
      for (byte b : digest.digest()) hex.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
      return hex.toString();
    } catch (java.security.NoSuchAlgorithmException impossible) {
      throw new IllegalStateException(impossible);
    }
  }
  private PhoneRouteGeometry() {}
}
