package org.ulpgc.bd;

import java.io.*;
import java.lang.management.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

public class UnifiedBenchmark {

    private static final int[] CONCURRENCY = {1, 2, 4, 8};
    private static final int REQUESTS_PER_THREAD = 50;

    // Adjust URLs to your running microservices
    private static final String INDEX_URL  = "http://localhost:7002/index/update/1234";
    private static final String SEARCH_URL = "http://localhost:7003/search?q=data";
    private static final String OUT = "benchmarks/results/metrics.csv";

    public static void main(String[] args) throws Exception {
        new File("benchmarks/results").mkdirs();
        try (PrintWriter pw = new PrintWriter(new FileWriter(OUT))) {
            pw.println("endpoint,threads,throughput,avg_ms,p50_ms,p95_ms,max_ms,cpu_pct,mem_mb");
        }

        for (int t : CONCURRENCY) {
            runBenchmark("POST /index/update/{id}", INDEX_URL, "POST", t);
            runBenchmark("GET /search", SEARCH_URL, "GET", t);
        }

        System.out.println("Benchmark completed. Results in " + OUT);
    }

    private static void runBenchmark(String label, String url, String method, int threads) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());

        long start = System.currentTimeMillis();
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                for (int j = 0; j < REQUESTS_PER_THREAD; j++) {
                    HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .timeout(Duration.ofSeconds(10));
                    if ("POST".equalsIgnoreCase(method))
                        reqBuilder = reqBuilder.POST(HttpRequest.BodyPublishers.noBody());
                    else
                        reqBuilder = reqBuilder.GET();

                    long t0 = System.nanoTime();
                    try {
                        client.send(reqBuilder.build(), HttpResponse.BodyHandlers.discarding());
                    } catch (Exception ignored) {}
                    long t1 = System.nanoTime();
                    latencies.add((t1 - t0) / 1_000_000); // ms
                }
            });
        }

        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.MINUTES);
        long elapsed = System.currentTimeMillis() - start;

        Collections.sort(latencies);
        double avg = latencies.stream().mapToLong(Long::longValue).average().orElse(0);
        long p50 = latencies.get((int)(latencies.size()*0.5));
        long p95 = latencies.get((int)(latencies.size()*0.95));
        long max = latencies.get(latencies.size()-1);
        double throughput = (threads * REQUESTS_PER_THREAD * 1000.0) / elapsed;

        // CPU and memory usage of current process
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        double cpuLoad = 0;
        long memUsedMb = 0;
        if (osBean instanceof com.sun.management.OperatingSystemMXBean mx) {
            cpuLoad = mx.getProcessCpuLoad() * 100;
            memUsedMb = (mx.getTotalPhysicalMemorySize() - mx.getFreePhysicalMemorySize()) / (1024 * 1024);
        }

        try (PrintWriter pw = new PrintWriter(new FileWriter(OUT, true))) {
            pw.printf("%s,%d,%.3f,%.3f,%d,%d,%d,%.2f,%d%n",
                    label, threads, throughput, avg, p50, p95, max, cpuLoad, memUsedMb);
        }
        System.out.printf("→ %s (%d threads) done: %.2f req/s%n", label, threads, throughput);
    }
}
