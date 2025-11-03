package org.ulpgc.bd.benchmark;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Random;

/**
 * Minimal local stub for end-to-end tests when real services are not running.
 * Exposes:
 *  - POST /ingest/{id}        on :7001
 *  - POST /index/update/{id}  on :7002
 *  - GET  /search?q=...       on :7003
 *
 * Each handler sleeps a small random delay to simulate realistic latency and replies 200.
 */
public class SmokeServer {
    public static void main(String[] args) throws Exception {
        startServer(7001, "/ingest", 10, 30);       // 10–30 ms
        startServer(7002, "/index/update", 20, 60);
        startServer(7003, "/search", 5, 25);
        System.out.println("Smoke servers on 7001/7002/7003. Ctrl+C to stop.");
        Thread.currentThread().join();
    }

    /**
     * Creates one simple HttpServer with a single context:
     *  - if basePath == "/search": GET /search?q=...
     *  - otherwise: POST basePath/{id}
     */
    static void startServer(int port, String basePath, int minMs, int maxMs) throws IOException {
        HttpServer srv = HttpServer.create(new InetSocketAddress(port), 0);
        if (!"/search".equals(basePath)) {
            srv.createContext(basePath, ex -> {
                if (!"POST".equals(ex.getRequestMethod())) { respond(ex, 405, ""); return; }
                sleepRandom(minMs, maxMs);
                respond(ex, 200, "{\"status\":\"ok\"}");
            });
        }
        if ("/search".equals(basePath)) {
            srv.createContext("/search", ex -> {
                if (!"GET".equals(ex.getRequestMethod())) { respond(ex, 405, ""); return; }
                String q = getQueryParam(ex, "q");
                sleepRandom(minMs, maxMs);
                respond(ex, 200, "[{\"id\":1,\"q\":\""+q+"\"}]");
            });
        }
        srv.setExecutor(null);
        srv.start();
        System.out.println("Started stub on :" + port + " " + basePath);
    }

    /** Sleep a random delay in [minMs, maxMs] to simulate work. */
    static void sleepRandom(int minMs, int maxMs) {
        int d = minMs + new Random().nextInt(Math.max(1, maxMs - minMs + 1));
        try { Thread.sleep(d); } catch (InterruptedException ignored) {}
    }

    /** Write a minimal HTTP response and close the exchange. */
    static void respond(HttpExchange ex, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
        ex.close();
    }

    /** Extract a single query parameter (URL-decoded); returns empty string if missing. */
    static String getQueryParam(HttpExchange ex, String key) {
        String q = ex.getRequestURI().getRawQuery();
        if (q == null) return "";
        for (String kv : q.split("&")) {
            String[] p = kv.split("=", 2);
            if (p.length == 2 && p[0].equals(key)) {
                return URLDecoder.decode(p[1], StandardCharsets.UTF_8);
            }
        }
        return "";
    }
}
