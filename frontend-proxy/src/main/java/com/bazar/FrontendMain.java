package com.bazar;

import static spark.Spark.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.benmanes.caffeine.cache.*;
import com.google.gson.*;
import org.apache.hc.client5.http.fluent.Request;

public class FrontendMain {

    /* ---------- helper: round-robin picker ---------- */
    static class RoundRobinLB {
        private final List<String> urls;
        private final AtomicInteger next = new AtomicInteger(0);
        RoundRobinLB(String csv) {
            this.urls = List.of(csv.split("\\s*,\\s*"));   // "http://host1:7001,http://host2:7002"
        }
        String pick() { return urls.get(Math.abs(next.getAndIncrement() % urls.size())); }
    }

    /* ---------- main ---------- */
    public static void main(String[] args) throws Exception {

        int portNo = Integer.parseInt(System.getenv().getOrDefault("PORT", "8000"));
        port(portNo);

        // read back-end URL lists from env
        RoundRobinLB catLB   = new RoundRobinLB(System.getenv().getOrDefault(
                                 "CATALOG_URLS", "http://localhost:7000"));
        RoundRobinLB orderLB = new RoundRobinLB(System.getenv().getOrDefault(
                                 "ORDER_URLS", "http://localhost:7100"));

        // Caffeine cache (2-minute TTL, 5k entries max)
        Cache<String, String> catalogCache = Caffeine.newBuilder()
                .maximumSize(5_000)
                .expireAfterWrite(2, TimeUnit.MINUTES)
                .build();

        /* ---- Health endpoint ---- */
        get("/health", (req, res) -> {
            res.type("application/json");
            return new Gson().toJson(Map.of("status","UP"));
        });

        /* ---- READ: forward to catalog, with cache ---- */
        get("/item/:id", (req, res) -> {
String key = req.uri();      // ← now "/" + "item/123"
    try {
        String json = catalogCache.getIfPresent(key);
        if (json == null) {
            String targetUrl = catLB.pick() + key;
            System.out.println("[Proxy] Forwarding to " + targetUrl);
            json = Request.get(targetUrl)
                          .execute().returnContent().asString();
            catalogCache.put(key, json);
        } else {
            System.out.println("[Proxy] Cache hit for " + key);
        }
        res.type("application/json");
        return json;
    } catch (Exception e) {
        System.err.println("❌ Error in /item/:id handler:");
        e.printStackTrace();
        res.status(500);
        return "{\"error\":\"" + e.getMessage().replace("\"","\\\"") + "\"}";
    }
});


        /* ---- WRITE: forward to order replica ---- */
       post("/purchase/:id", (req, res) -> {
    String uri = req.uri();  // "/purchase/123"
    try {
        String targetUrl = orderLB.pick() + uri;
        System.out.println("[Proxy] Forwarding WRITE to " + targetUrl);
        String json = Request.post(targetUrl)
                             .bodyString(req.body(), null)
                             .execute()
                             .returnContent()
                             .asString();
        res.type("application/json");
        return json;
    } catch (Exception e) {
        System.err.println("❌ Error in /purchase/:id handler:");
        e.printStackTrace();
        res.status(500);
        return "{\"error\":\"" + e.getMessage().replace("\"","\\\"") + "\"}";
    }
});


        /* ---- invalidation endpoint for replicas ---- */
        post("/invalidate", (req, res) -> {
    JsonObject body = JsonParser.parseString(req.body()).getAsJsonObject();
    String id = body.get("itemId").getAsString();
    // Remove any cached /item/{id} entries
    catalogCache.asMap()
                .keySet()
                .removeIf(key -> key.contains("/item/" + id));
    res.status(204);
    return "";
});


        System.out.println("Frontend proxy listening on port " + portNo);
    }
}
