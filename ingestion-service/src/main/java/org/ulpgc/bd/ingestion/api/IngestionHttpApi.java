package org.ulpgc.bd.ingestion.api;

import io.javalin.Javalin;
import org.ulpgc.bd.ingestion.service.IngestionService;

public class IngestionHttpApi {

    public static void register(Javalin app, IngestionService service) {
        app.post("/ingest/{id}", ctx -> {
            int id = Integer.parseInt(ctx.pathParam("id"));
            ctx.json(service.ingest(id));
        });
        app.get("/ingest/status/{id}", ctx -> {
            int id = Integer.parseInt(ctx.pathParam("id"));
            ctx.json(service.checkStatus(id));
        });
        app.get("/ingest/list", ctx -> ctx.json(service.listBooks()));
    }
}
