package com.bazar;

import static spark.Spark.*;
import com.google.gson.Gson;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CatalogMain {

    static class Book {
        String id;
        String name;
        String topic;
        int stock;
        double price;

        Book(String id, String name, String topic, int stock, double price) {
            this.id = id;
            this.name = name;
            this.topic = topic;
            this.stock = stock;
            this.price = price;
        }
    }

    // 🧠 تخزين الكتب بشكل فعلي
    static Map<String, Book> catalog = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7000"));
        port(port);

        // 📚 تعريف الكتب
        catalog.put("1", new Book("1", "How to get a good grade in DOS", "distributed systems", 5, 49.99));
        catalog.put("2", new Book("2", "RPCs for Noobs", "distributed systems", 2, 39.99));
        catalog.put("3", new Book("3", "Xen and the Art of Surviving Undergrad", "undergraduate school", 0, 29.99));
        catalog.put("4", new Book("4", "Cooking for the Impatient Undergrad", "undergraduate school", 3, 19.99));

        // ✅ /health
        get("/health", (req, res) -> {
            res.type("application/json");
            return new Gson().toJson(Map.of(
                    "status", "UP",
                    "serverId", System.getenv().getOrDefault("SERVER_ID", "catalog-local")
            ));
        });

        // ✅ /item/:id
        get("/item/:id", (req, res) -> {
            res.type("application/json");
            String id = req.params(":id");
            Book book = catalog.get(id);
            if (book == null) {
                res.status(404);
                return new Gson().toJson(Map.of("error", "Book not found"));
            }
            return new Gson().toJson(book);
        });

        // ✅ /decrement/:id
        post("/decrement/:id", (req, res) -> {
            res.type("application/json");
            String id = req.params(":id");
            Book book = catalog.get(id);
            if (book == null) {
                res.status(404);
                return new Gson().toJson(Map.of("error", "Book not found"));
            }
            if (book.stock > 0) {
                book.stock--;
                return new Gson().toJson(Map.of("ok", true, "newStock", book.stock));
            } else {
                res.status(400);
                return new Gson().toJson(Map.of("ok", false, "error", "Stock already zero"));
            }
        });
    }
}
