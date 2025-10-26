package org.ulpgc.bd;

import io.javalin.Javalin;
import io.javalin.http.Context;
import com.google.gson.Gson;
import java.util.*;

public class IngestionServiceApp {
    private static final Gson gson = new Gson();

    public static void main(String[] args) {
        Javalin app = Javalin.create(c -> c.http.defaultContentType = "application/json").start(7001);

        app.get("/status", ctx -> {
            Map<String, Object> status = Map.of("service", "ingestion-service", "status", "running");
            ctx.result(gson.toJson(status));
        });

        app.post("/ingest/{bookId}", IngestionServiceApp::ingest);
        app.get("/ingest/status/{bookId}", IngestionServiceApp::status);
        app.get("/ingest/list", IngestionServiceApp::list);
    }

    private static void ingest(Context ctx) {
        String bookId = ctx.pathParam("bookId");
        ctx.result(gson.toJson(Map.of(
                "book_id", bookId,
                "status", "downloaded",
                "path", "datalake/20251008/14/" + bookId
        )));
    }

    private static void status(Context ctx) {
        String bookId = ctx.pathParam("bookId");
        ctx.result(gson.toJson(Map.of("book_id", bookId, "status", "available")));
    }

    private static void list(Context ctx) {
        List<Integer> books = List.of(1342, 5, 17, 42);
        ctx.result(gson.toJson(Map.of("count", books.size(), "books", books)));
    }
}

