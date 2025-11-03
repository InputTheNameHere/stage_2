package org.ulpgc.bd.benchmark;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Text utility helpers used by benchmarks.
 * The goal is to keep these helpers small and deterministic so they don't skew timing.
 */
public final class TextUtils {
    private TextUtils() {}

    /**
     * Normalizes a string by:
     *  1) Decomposing Unicode (NFD) and stripping diacritic marks (\\p{M}),
     *  2) Lowercasing using ROOT locale for stable results across JVMs.
     *
     * @param s input string (should be non-null for best performance)
     * @return ASCII-ish lowercase string without diacritics
     */
    public static String normalize(String s) {
        String noDiacritics = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return noDiacritics.toLowerCase(Locale.ROOT);
    }

    /**
     * Very simple tokenizer:
     *  - normalize(),
     *  - remove any non [a-z0-9\\s] chars,
     *  - split on whitespace,
     *  - keep non-empty tokens.
     *
     * This intentionally avoids regex capture groups, streams, etc. to keep it predictable.
     *
     * @param text free-form input text
     * @return list of tokens (possibly empty, never null)
     */
    public static List<String> tokenizeSimple(String text) {
        String norm = normalize(text).replaceAll("[^a-z0-9\\s]", " ");
        String[] parts = norm.trim().split("\\s+");
        List<String> out = new ArrayList<>(parts.length);
        for (String p : parts) {
            if (!p.isEmpty()) out.add(p);
        }
        return out;
    }
}
