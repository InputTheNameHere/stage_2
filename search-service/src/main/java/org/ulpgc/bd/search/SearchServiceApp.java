package org.ulpgc.bd.search;

import io.javalin.Javalin;
import org.ulpgc.bd.search.api.SearchHttpApi;

public class SearchServiceApp {
    public static void main(String[] args) {
        Javalin app = Javalin.create(c -> c.http.defaultContentType = "application/json").start(7003);
        new SearchHttpApi(app);
        System.out.println("Search service started");
    }
}