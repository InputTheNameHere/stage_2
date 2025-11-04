package org.ulpgc.bd;

import org.openjdk.jmh.annotations.*;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@Fork(1)
@State(Scope.Benchmark)
public class InvertedIndexBench {

    private HashMap<String, List<Integer>> index;

    @Setup
    public void setup() {
        index = new HashMap<>();
    }

    @Benchmark
    public void insertTerm() {
        String term = "data";
        index.computeIfAbsent(term, k -> new ArrayList<>()).add(1);
    }
}
