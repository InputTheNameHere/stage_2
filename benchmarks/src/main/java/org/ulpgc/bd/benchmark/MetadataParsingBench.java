package org.ulpgc.bd.benchmark;

import org.openjdk.jmh.annotations.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JMH throughput benchmark for regex-based metadata header parsing.
 * Extracts title/author/language/year from a fixed Project Gutenberg-style header string.
 */
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(1)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class MetadataParsingBench {

    /** Precompiled patterns so we don't measure regex compilation cost inside the benchmark. */
    private static final Pattern TITLE = Pattern.compile("^Title:\\s*(.*)$", Pattern.MULTILINE);
    private static final Pattern AUTHOR = Pattern.compile("^Author:\\s*(.*)$", Pattern.MULTILINE);
    private static final Pattern LANG = Pattern.compile("^Language:\\s*(.*)$", Pattern.MULTILINE);
    private static final Pattern YEAR = Pattern.compile("^Release Date:\\s*.*?(\\d{4}).*$", Pattern.MULTILINE);

    @State(Scope.Benchmark)
    public static class Headers {
        /** Static header reused across iterations; makes results stable and comparable. */
        public String header;

        @Setup(Level.Trial)
        public void setup() {
            header =
                    "Project Gutenberg EBook\n" +
                            "Title: Pride and Prejudice\n" +
                            "Author: Jane Austen\n" +
                            "Language: English\n" +
                            "Release Date: August 26, 2008 [EBook #1342]\n";
        }
    }

    /**
     * Parses the header and returns a small map (title/author/language/year).
     * Returning a value prevents the JIT from optimizing away the work.
     */
    @Benchmark
    public Map<String, String> parse_header(Headers h) {
        Map<String, String> meta = new HashMap<>();
        meta.put("title", group(TITLE, h.header));
        meta.put("author", group(AUTHOR, h.header));
        meta.put("language", group(LANG, h.header));
        meta.put("year", group(YEAR, h.header));
        return meta;
    }

    /** Utility: return the first capturing group if present, else empty string. */
    private static String group(Pattern p, String s) {
        Matcher m = p.matcher(s);
        return m.find() ? m.group(1).trim() : "";
    }
}
