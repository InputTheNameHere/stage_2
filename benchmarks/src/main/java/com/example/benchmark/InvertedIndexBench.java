package com.example.benchmark;

import org.openjdk.jmh.annotations.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * JMH average-time benchmark for building a tiny inverted index
 * and for computing a simple top-frequency lookup over a synthetic corpus.
 */
@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@Fork(1)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class InvertedIndexBench {

    @State(Scope.Thread)
    public static class Docs {
        @Param({"500","2000"}) public int docs;
        @Param({"80","400"})   public int avgWords;

        /** Synthetic corpus: list of documents, each as a list of tokens. */
        public List<List<String>> corpus;

        @Setup(Level.Trial)
        public void setup() {
            Random r = new Random(7);
            String[] vocab = {"adventure","sea","captain","love","castle","robot","magic","science",
                    "data","index","book","library","search","engine","java","javalin"};
            corpus = new ArrayList<>(docs);
            for (int i = 0; i < docs; i++) {
                List<String> tokens = new ArrayList<>(avgWords);
                for (int j = 0; j < avgWords; j++) {
                    tokens.add(vocab[r.nextInt(vocab.length)]);
                }
                corpus.add(tokens);
            }
        }
    }

    /**
     * Builds an inverted index: term -> list of docIds that contain the term.
     * The map is sized roughly to reduce rehashing during the benchmark.
     */
    @Benchmark
    public Map<String, List<Integer>> build_index(Docs d) {
        Map<String, List<Integer>> index = new HashMap<>(d.docs * 2);
        for (int docId = 0; docId < d.corpus.size(); docId++) {
            for (String term : d.corpus.get(docId)) {
                index.computeIfAbsent(term, k -> new ArrayList<>(8)).add(docId);
            }
        }
        return index;
    }

    /**
     * Computes the most frequent token in the corpus by counting occurrences.
     * Returns the top frequency (an int), not the token; that’s enough to force the work.
     */
    @Benchmark
    public int lookup_topfreq(Docs d) {
        Map<String, Integer> freq = new HashMap<>();
        for (List<String> tokens : d.corpus) {
            for (String t : tokens) freq.merge(t, 1, Integer::sum);
        }
        return freq.entrySet().stream().max(Map.Entry.comparingByValue()).get().getValue();
    }
}
