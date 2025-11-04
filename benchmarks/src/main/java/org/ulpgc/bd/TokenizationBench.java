package org.ulpgc.bd;

import org.openjdk.jmh.annotations.*;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@Fork(1)
@State(Scope.Benchmark)
public class TokenizationBench {

    private String text;

    @Setup
    public void setup() {
        text = "This is a benchmark test for tokenization performance in Java microbenchmarks.";
    }

    @Benchmark
    public void tokenize() {
        text.split("\\s+");
    }
}
