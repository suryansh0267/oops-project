package com.itsm.intelligence.frustration;

import java.util.regex.Pattern;

/** Mask at entry, before persistence, reply echoes, or any optional external call. */
public final class ProfanitySanitizer {
    private static final Pattern WORDS = Pattern.compile("(?i)\\b(fuck(?:ing|ed|er)?|shit(?:ty)?|bitch|asshole|bastard|damn)\\b");
    private ProfanitySanitizer() { }
    public static boolean contains(String text) { return text != null && WORDS.matcher(text).find(); }
    public static String mask(String text) {
        return text == null ? "" : WORDS.matcher(text).replaceAll("[redacted]");
    }
}
