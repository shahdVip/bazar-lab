package com.bazar;

import static spark.Spark.*;
import com.google.gson.Gson;
import java.net.http.*;  // HttpClient, HttpRequest, HttpResponse
import java.net.URI;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Map;

public class OrderMain {

    // Used to notify the frontend proxy
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private static void pushInvalidate(String itemId) {
        String front = System.getenv("FRONTEND");
        if (front == null) return;
        String body = new Gson().toJson(Map.of("itemId", itemId));
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(front + "/invalidate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try { HTTP.send(req, HttpResponse.BodyHandlers.discarding()); }
        catch (Exception e) { System.err.println("invalidate failed: " + e); }
    }

    record Status(String status, String serverId) {}

    public static void main(String[] args) {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7100"));
        port(port);

        get("/health", (req, res) -> {
            res.type("application/json");
            return new Gson().toJson(new Status("UP",
                    System.getenv().getOrDefault("SERVER_ID", "order-local")));
        });

        post("/purchase/:id", (req, res) -> {
            String id = req.params(":id");

            // 1. Get item info from catalog
            String catalogUrl = System.getenv().getOrDefault("CATALOG_URL", "http://localhost:7000");
            String itemUrl = catalogUrl + "/item/" + id;

            try {
                HttpRequest getReq = HttpRequest.newBuilder()
                        .uri(URI.create(itemUrl))
                        .GET()
                        .build();
                HttpResponse<String> response = HTTP.send(getReq, HttpResponse.BodyHandlers.ofString());

                JsonObject book = JsonParser.parseString(response.body()).getAsJsonObject();
                int stock = book.get("stock").getAsInt();

                if (stock > 0) {
                    // ↓↓↓ NEW: تقليل المخزون
                    String decUrl = catalogUrl + "/decrement/" + id;
                    HttpRequest decReq = HttpRequest.newBuilder()
                            .uri(URI.create(decUrl))
                            .POST(HttpRequest.BodyPublishers.noBody())
                            .build();
                    HTTP.send(decReq, HttpResponse.BodyHandlers.ofString());

                    pushInvalidate(id);

                    res.type("application/json");
                    return new Gson().toJson(Map.of("ok", true, "item", id));
                } else {
                    res.status(400);
                    return new Gson().toJson(Map.of("ok", false, "error", "Out of stock"));
                }

            } catch (Exception e) {
                res.status(500);
                return new Gson().toJson(Map.of("ok", false, "error", e.getMessage()));
            }
        });
    }
}
