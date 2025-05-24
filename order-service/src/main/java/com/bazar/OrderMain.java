package com.bazar;

import static spark.Spark.*;
import com.google.gson.Gson;
import java.net.http.*;  // HttpClient, HttpRequest, HttpResponse
import java.net.URI;
import com.google.gson.Gson;
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
    pushInvalidate(id);   // ① notify the proxy
    res.type("application/json");
    return "{ \"ok\": true, \"item\": \""+id+"\" }";
});
    }
}
