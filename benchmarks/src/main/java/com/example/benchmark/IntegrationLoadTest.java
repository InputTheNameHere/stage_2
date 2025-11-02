package com.example.benchmark;

import com.google.gson.Gson;
import java.util.function.Supplier;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.DoubleStream;

/**
 * Simple end-to-end load test runner for three endpoints:
 *   - GET  /search?q=<term>
 *   - POST /ingest/{id}
 *   - POST /index/update/{id}
 *
 * Usage:
 *   java -Xmx512m -cp target/benchmarks-1.0.0.jar com.example.benchmark.IntegrationLoadTest \
 *        --ingest http://localhost:7001 \
 *        --index  http://localhost:7002 \
 *        --search http://localhost:7003 \
 *        --concurrency 1,2,4,8,16 \
 *        --durationSec 20
 *
 * Outputs CSV to: benchmarks/results/integration_metrics.csv
 *
 * Notes:
 *  - Uses a shared HttpClient with reasonable timeouts.
 *  - At each concurrency level, collects latency samples and computes avg/p50/p95/max.
 *  - Returns non-2xx HTTP status codes as errors in the summary.
 */

public class IntegrationLoadTest {

    // Compact immutable row used to write the CSV summary.
    record Summary(String endpoint, int concurrency, long requests, double avgMs,
                   double p50, double p95, double maxMs, long errors) {}

    public static void main(String[] args) throws Exception {
        Map<String,String> arg = parseArgs(args);
        String ingest = arg.getOrDefault("--ingest", "http://localhost:7001");
        String index  = arg.getOrDefault("--index",  "http://localhost:7002");
        String search = arg.getOrDefault("--search", "http://localhost:7003");
        int duration  = Integer.parseInt(arg.getOrDefault("--durationSec","20"));

        int[] conc = Arrays.stream(arg.getOrDefault("--concurrency","1,2,4,8")
                .split(",")).mapToInt(Integer::parseInt).toArray();

        List<String> terms = loadTerms();
        List<Integer> bookIds = Arrays.asList(1342, 11, 12, 158, 201, 6500);

        List<Summary> all = new ArrayList<>();
        // Each runEndpoint(...) executes the endpoint across all concurrency levels and collects a Summary row per level.
        all.addAll(runEndpoint("GET /search", conc, duration,
                () -> randomSearch(search, terms)));
        all.addAll(runEndpoint("POST /ingest/{id}", conc, duration,
                () -> ingestOnce(ingest, pick(bookIds))));
        all.addAll(runEndpoint("POST /index/update/{id}", conc, duration,
                () -> indexOnce(index, pick(bookIds))));

        writeCsv(all);
        System.out.println("Saved: benchmarks/results/integration_metrics.csv");
    }
    /** Minimal positional CLI parser: expects pairs like '--key value'. */
    static Map<String,String> parseArgs(String[] a){
        Map<String,String> m = new HashMap<>();
        for (int i=0;i<a.length;i+=2) if(i+1<a.length) m.put(a[i],a[i+1]);
        return m;
    }
    /** Picks a random element from a non-empty list. */
    static <T> T pick(List<T> list) { return list.get(ThreadLocalRandom.current().nextInt(list.size())); }

    /**
     * Loads a list of search terms from src/main/resources/terms.txt.
     * Falls back to a small built-in list if the file is missing.
     */
    static List<String> loadTerms() throws IOException {
        Path p = Paths.get("src","main","resources","terms.txt");
        if (!Files.exists(p)) return List.of("adventure","love","castle","robot","science");
        return Files.readAllLines(p);
    }

    // ---- HTTP helpers ----
    static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    static Callable<Integer> randomSearch(String base, List<String> terms){
        return () -> {
            String q = pick(terms);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(base + "/search?q=" + q))
                    .timeout(Duration.ofSeconds(5))
                    .GET().build();
            HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            return resp.statusCode();
        };
    }

    static Callable<Integer> ingestOnce(String base, int id){
        return () -> {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(base + "/ingest/" + id))
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.noBody()).build();
            HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            return resp.statusCode();
        };
    }

    static Callable<Integer> indexOnce(String base, int id){
        return () -> {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(base + "/index/update/" + id))
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.noBody()).build();
            HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            return resp.statusCode();
        };
    }

    // ---- runner ----
    static List<Summary> runEndpoint(String name, int[] conc, int durationSec,
                                     Supplier<Callable<Integer>> supplier) throws InterruptedException {
        List<Summary> out = new ArrayList<>();
        for (int c : conc) {
            long endAt = System.nanoTime() + TimeUnit.SECONDS.toNanos(durationSec);
            ExecutorService pool = Executors.newFixedThreadPool(c);
            List<Double> lat = Collections.synchronizedList(new ArrayList<>());
            long[] okErr = new long[2];

            Runnable task = () -> {
                while (System.nanoTime() < endAt) {
                    long t0 = System.nanoTime();
                    try {
                        Integer code = supplier.get().call();
                        if (code >= 200 && code < 300) okErr[0]++; else okErr[1]++;
                    } catch (Exception e) {
                        okErr[1]++;
                    } finally {
                        lat.add((System.nanoTime() - t0) / 1_000_000.0);
                    }
                }
            };
            for (int i=0;i<c;i++) pool.submit(task);
            pool.shutdown();
            pool.awaitTermination(durationSec + 10L, TimeUnit.SECONDS);

            double[] arr = lat.stream().mapToDouble(Double::doubleValue).sorted().toArray();
            double avg = Arrays.stream(arr).average().orElse(0);
            double p50 = percentile(arr, 50);
            double p95 = percentile(arr, 95);
            double max = arr.length>0? arr[arr.length-1] : 0;

            out.add(new Summary(name, c, okErr[0]+okErr[1], avg, p50, p95, max, okErr[1]));
            System.out.printf(Locale.ROOT, "%s c=%d: avg=%.2fms p95=%.2fms err=%d%n",
                    name, c, avg, p95, okErr[1]);
        }
        return out;
    }

    static double percentile(double[] sorted, int p){
        if (sorted.length==0) return 0;
        double rank = (p/100.0)*(sorted.length-1);
        int lo = (int)Math.floor(rank), hi = (int)Math.ceil(rank);
        return (lo==hi) ? sorted[lo] : sorted[lo] + (sorted[hi]-sorted[lo])*(rank-lo);
    }

    static void writeCsv(List<Summary> s) throws IOException {
        Path dir = Paths.get("results");
        Files.createDirectories(dir);
        Path csv = dir.resolve("integration_metrics.csv");
        try (BufferedWriter w = Files.newBufferedWriter(csv)) {
            w.write("endpoint,concurrency,requests,avg_ms,p50_ms,p95_ms,max_ms,errors\n");
            for (Summary x : s) {
                w.write(String.format(Locale.ROOT, "%s,%d,%d,%.3f,%.3f,%.3f,%.3f,%d%n",
                        x.endpoint, x.concurrency, x.requests, x.avgMs, x.p50, x.p95, x.maxMs, x.errors));
            }
        }
    }
}
