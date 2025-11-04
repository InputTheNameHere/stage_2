package org.ulpgc.bd;

import org.openjdk.jmh.annotations.*;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@Fork(1)
@State(Scope.Benchmark)
public class MetadataParsingBench {

    private String header;

    @Setup
    public void setup() {
        header = "Title: Pride and Prejudice\nAuthor: Jane Austen\nLanguage: English\nRelease Date: 1813";
    }

    @Benchmark
    public HashMap<String, String> parseHeader() {
        HashMap<String, String> metadata = new HashMap<>();
        for (String line : header.split("\n")) {
            String[] parts = line.split(": ");
            if (parts.length == 2) {
                metadata.put(parts[0].trim(), parts[1].trim());
            }
        }
        return metadata;
    }
}
