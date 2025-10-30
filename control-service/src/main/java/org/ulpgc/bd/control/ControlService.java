package org.ulpgc.bd.control;

import io.javalin.Javalin;
import com.google.gson.Gson;
import java.net.http.*;
import java.net.URI;
import java.util.Map;

public class ControlService {
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final Gson gson = new Gson();

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7003);

        app.get("/status", ctx -> ctx.json(Map.of("service", "control", "status", "running")));

        app.post("/control/run/{book_id}", ctx -> {
            int bookId = Integer.parseInt(ctx.pathParam("book_id"));
            try {
                // Step 1: Ingest
                post("http://localhost:7001/ingest/" + bookId);

                // Step 2: Wait for ingestion confirmation
                String ingestionStatus;
                do {
                    ingestionStatus = getStatus("http://localhost:7001/ingest/status/" + bookId);
                    Thread.sleep(2000);
                } while (!"available".equalsIgnoreCase(ingestionStatus));

                // Step 3: Index
                post("http://localhost:7002/index/update/" + bookId);

                // Step 4: Optional - notify search
                get("http://localhost:7000/status");

                ctx.json(Map.of(
                        "book_id", bookId,
                        "status", "complete"
                ));
            } catch (Exception e) {
                ctx.status(500).json(Map.of("error", e.getMessage()));
            }
        });
    }

    private static void post(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private static String getStatus(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> body = gson.fromJson(res.body(), Map.class);
        return (String) body.get("status");
    }

    private static void get(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        client.send(req, HttpResponse.BodyHandlers.ofString());
    }
}
