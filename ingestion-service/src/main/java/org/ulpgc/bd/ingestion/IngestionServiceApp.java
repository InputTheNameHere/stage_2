package org.ulpgc.bd.ingestion;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

public class IngestionServiceApp {

    private static final Path DATALAKE_PATH = Paths.get("datalake");

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(7001), 0);

        server.createContext("/ingest", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String[] parts = path.split("/");
            if (parts.length == 3 && exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                int bookId = Integer.parseInt(parts[2]);
                Map<String, Object> result = downloadBook(bookId);
                sendJson(exchange, result);
            } else if (parts.length == 4 && parts[2].equals("status") && exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                int bookId = Integer.parseInt(parts[3]);
                Map<String, Object> result = checkStatus(bookId);
                sendJson(exchange, result);
            } else if (parts.length == 3 && parts[2].equals("list") && exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                Map<String, Object> result = listBooks();
                sendJson(exchange, result);
            } else {
                sendJson(exchange, Map.of("error", "Invalid endpoint or method"));
            }
        });

        server.start();
    }

    private static Map<String, Object> downloadBook(int bookId) {
        Map<String, Object> response = new LinkedHashMap<>();
        try {
            String today = LocalDate.now().toString().replace("-", "");
            Path dir = DATALAKE_PATH.resolve(today).resolve(String.valueOf(bookId));
            Files.createDirectories(dir);

            String url = "https://www.gutenberg.org/files/" + bookId + "/" + bookId + "-0.txt";
            Path filePath = dir.resolve(bookId + ".txt");

            try (InputStream in = new URL(url).openStream()) {
                Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
            }

            response.put("book_id", bookId);
            response.put("status", "downloaded");
            response.put("path", filePath.toString());
        } catch (Exception e) {
            response.put("book_id", bookId);
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        return response;
    }

    private static Map<String, Object> checkStatus(int bookId) {
        Map<String, Object> response = new LinkedHashMap<>();
        try {
            boolean found = Files.walk(DATALAKE_PATH)
                    .anyMatch(path -> path.getFileName().toString().equals(bookId + ".txt"));
            response.put("book_id", bookId);
            response.put("status", found ? "available" : "not found");
        } catch (IOException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        return response;
    }

    private static Map<String, Object> listBooks() {
        Map<String, Object> response = new LinkedHashMap<>();
        List<Integer> ids = new ArrayList<>();
        try {
            if (Files.exists(DATALAKE_PATH)) {
                Files.walk(DATALAKE_PATH)
                        .filter(path -> path.toString().endsWith(".txt"))
                        .forEach(path -> {
                            try {
                                ids.add(Integer.parseInt(path.getFileName().toString().replace(".txt", "")));
                            } catch (NumberFormatException ignored) {}
                        });
            }
            response.put("count", ids.size());
            response.put("books", ids);
        } catch (IOException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }
        return response;
    }

    private static void sendJson(HttpExchange exchange, Map<String, Object> data) throws IOException {
        StringBuilder json = new StringBuilder("{");
        Iterator<Map.Entry<String, Object>> it = data.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> entry = it.next();
            json.append("\"").append(entry.getKey()).append("\": ");
            Object value = entry.getValue();
            if (value instanceof Number || value instanceof Boolean) {
                json.append(value);
            } else {
                json.append("\"").append(value).append("\"");
            }
            if (it.hasNext()) {
                json.append(", ");
            }
        }
        json.append("}");
        byte[] response = json.toString().getBytes();
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }
}
