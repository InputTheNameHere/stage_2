package org.ulpgc.bd.search.api;

import io.javalin.Javalin;
import io.javalin.http.Context;
import com.google.gson.Gson;
import org.ulpgc.bd.search.model.SearchResult;
import org.ulpgc.bd.search.service.SearchService;

import java.util.*;

public class SearchHttpApi {
    private static final Gson gson = new Gson();
    private final SearchService searchService = new SearchService();

    public SearchHttpApi(Javalin app) {
        app.get("/status", this::status);
        app.get("/search", this::search);
    }

    private void status(Context ctx) {
        Map<String, Object> status = Map.of(
                "service", "search-service",
                "status", "running"
        );
        ctx.result(gson.toJson(status));
    }

    private void search(Context ctx) {
        String term = Optional.ofNullable(ctx.queryParam("q")).orElse("");
        String author = ctx.queryParam("author");
        String language = ctx.queryParam("language");
        String yearStr = ctx.queryParam("year");
        Integer year = (yearStr != null) ? Integer.parseInt(yearStr) : null;

        List<SearchResult> results = searchService.search(term, author, language, year);

        Map<String, Object> filters = new LinkedHashMap<>();
        if (author != null) filters.put("author", author);
        if (language != null) filters.put("language", language);
        if (year != null) filters.put("year", year);

        Map<String, Object> response = Map.of(
                "query", term,
                "filters", filters,
                "count", results.size(),
                "results", results
        );

        ctx.result(gson.toJson(response));
    }
}
