package com.andremunay.hobbyhub.shared.util;

import java.util.Locale;

public class NameNormalizer {
  public static String normalize(String raw) {
    return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
  }
}
