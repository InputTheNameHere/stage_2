package org.ulpgc.bd.search.service;

import com.google.gson.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import java.util.stream.Collectors;

public class SearchService {

    private static final String INDEX_STATUS_URL = "http://localhost:7002/index/status";
    private static final Gson gson = new Gson();

    public static List<Map<String, Object>> search(String q, String author, String language, String year) {
        try {

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(INDEX_STATUS_URL))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.err.println("SearchService: Index service unavailable (" + response.statusCode() + ")");
                return List.of();
            }

            Map<String, Object> status = gson.fromJson(response.body(), Map.class);

            if (!status.containsKey("books")) {
                System.err.println("SearchService: No 'books' field found in index/status response");
                return List.of();
            }

            List<Map<String, Object>> indexedBooks = (List<Map<String, Object>>) status.get("books");

            return indexedBooks.stream()
                    .filter(b -> q == null || b.get("title").toString().toLowerCase().contains(q.toLowerCase()))
                    .filter(b -> author == null || author.equalsIgnoreCase((String) b.get("author")))
                    .filter(b -> language == null || language.equalsIgnoreCase((String) b.get("language")))
                    .filter(b -> year == null || year.equals(String.valueOf(b.get("year"))))
                    .collect(Collectors.toList());

        } catch (IOException | InterruptedException e) {
            System.err.println("SearchService: Error communicating with Index Service - " + e.getMessage());
            return List.of();
        } catch (Exception e) {
            System.err.println("SearchService: Unexpected error - " + e);
            return List.of();
        }
    }
}
