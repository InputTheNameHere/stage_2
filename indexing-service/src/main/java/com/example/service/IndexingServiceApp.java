package com.example.service;

import io.javalin.Javalin;
import io.javalin.http.Context;
import com.google.gson.Gson;
import java.util.Map;

public class IndexingServiceApp {
    private static final Gson gson = new Gson();

    public static void main(String[] args) {
        Javalin app = Javalin.create(c -> c.http.defaultContentType = "application/json").start(7002);

        app.get("/status", ctx -> {
            Map<String, Object> status = Map.of("service", "indexing-service", "status", "running");
            ctx.result(gson.toJson(status));
        });

        app.post("/index/update/{bookId}", IndexingServiceApp::update);
        app.post("/index/rebuild", IndexingServiceApp::rebuild);
        app.get("/index/status", IndexingServiceApp::status);
    }

    private static void update(Context ctx) {
        String id = ctx.pathParam("bookId");
        ctx.result(gson.toJson(Map.of("book_id", id, "index", "updated")));
    }

    private static void rebuild(Context ctx) {
        ctx.result(gson.toJson(Map.of("books_processed", 1000, "elapsed_time", "35.2s")));
    }

    private static void status(Context ctx) {
        ctx.result(gson.toJson(Map.of(
                "books_indexed", 1200,
                "last_update", "2025-10-08T14:05:00Z",
                "index_size_MB", 42.7
        )));
    }
}
