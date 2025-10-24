package com.example.service;

import io.javalin.Javalin;
import io.javalin.http.Context;
import com.google.gson.Gson;
import java.util.*;

public class SearchServiceApp {
    private static final Gson gson = new Gson();

    public static void main(String[] args) {
        Javalin app = Javalin.create(c -> c.http.defaultContentType = "application/json").start(7003);

        app.get("/status", ctx -> {
            Map<String, Object> status = Map.of("service", "search-service", "status", "running");
            ctx.result(gson.toJson(status));
        });

        app.get("/search", SearchServiceApp::search);
    }

    private static void search(Context ctx) {
        String term = Objects.requireNonNullElse(ctx.queryParam("filter"), "none");
        ctx.result(gson.toJson(Map.of(
                "query", term,
                "filters", Map.of(),
                "count", 2,
                "results", List.of(
                        Map.of("book_id", 5, "title", "Example A", "author", "Author A", "language", "en", "year", 1900),
                        Map.of("book_id", 6, "title", "Example B", "author", "Author B", "language", "en", "year", 1910)
                )
        )));
    }
}
