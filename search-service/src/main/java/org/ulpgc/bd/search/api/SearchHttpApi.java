package org.ulpgc.bd.search.api;

import io.javalin.Javalin;
import io.javalin.http.Context;
import com.google.gson.Gson;
import org.ulpgc.bd.search.service.SearchService;
import java.util.*;

public class SearchHttpApi {
    private static final Gson gson = new Gson();

    public static void register(Javalin app) {
        app.get("/status", ctx -> ctx.result(gson.toJson(Map.of("service", "search-service", "status", "running"))));
        app.get("/search", SearchHttpApi::handleSearch);
    }

    private static void handleSearch(Context ctx) {
        String q = ctx.queryParam("q");
        String author = ctx.queryParam("author");
        String language = ctx.queryParam("language");
        String year = ctx.queryParam("year");

        List<Map<String, Object>> results = SearchService.search(q, author, language, year);

        Map<String, Object> response = Map.of(
                "query", q == null ? "" : q,
                "filters", buildFilters(author, language, year),
                "count", results.size(),
                "results", results
        );

        ctx.result(gson.toJson(response));
    }

    private static Map<String, Object> buildFilters(String author, String language, String year) {
        Map<String, Object> filters = new LinkedHashMap<>();
        if (author != null) filters.put("author", author);
        if (language != null) filters.put("language", language);
        if (year != null) filters.put("year", year);
        return filters;
    }
}
