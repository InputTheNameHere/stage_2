package org.ulpgc.bd.search;

import io.javalin.Javalin;
import org.ulpgc.bd.search.api.SearchHttpApi;

public class SearchServiceApp {
    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> config.http.defaultContentType = "application/json").start(7003);
        SearchHttpApi.register(app);
        System.out.println("Search Service running on http://localhost:7003");
    }
}