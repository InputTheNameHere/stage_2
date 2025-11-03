package org.ulpgc.bd.benchmark;

import org.openjdk.jmh.annotations.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * JMH throughput benchmark for the simple tokenizer in TextUtils.
 *
 * It generates a deterministic synthetic text of a given size and measures how many
 * tokenization operations per millisecond we can complete. This isolates the cost
 * of basic normalization and whitespace splitting.
 */
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class TokenizationBench {

    @State(Scope.Benchmark)
    public static class Data {
        @Param({"1000", "10000", "100000"}) // znaki
        public int textSize;

        /** Synthetic text built once per trial to avoid counting setup in measurements. */
        public String text;

        @Setup(Level.Trial)
        public void setup() {
            // Deterministic "text" with a simple word distribution so results are reproducible.
            Random r = new Random(42);
            String[] vocab = {"adventure","sea","captain","love","castle","robot","magic","science"};
            StringBuilder sb = new StringBuilder(textSize + 100);
            while (sb.length() < textSize) {
                sb.append(vocab[r.nextInt(vocab.length)]).append(' ');
                // Occasionally inject punctuation to exercise the sanitizer branch.
                if (r.nextDouble() < 0.1) sb.append(",.;:!?".charAt(r.nextInt(6))).append(' ');
            }
            this.text = sb.toString();
        }
    }

    /**
     * Benchmark method — returns the token list so the JVM can't dead-code eliminate work.
     */
    @Benchmark
    public List<String> tokenize_simple(Data d) {
        return TextUtils.tokenizeSimple(d.text);
    }
}
