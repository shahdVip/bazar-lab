package com.bazar;

import static spark.Spark.*;
import com.google.gson.Gson;

public class CatalogMain {
    record Status(String status, String serverId) {}

    public static void main(String[] args) {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "7000"));
        port(port);

        get("/health", (req, res) -> {
            res.type("application/json");
            return new Gson().toJson(new Status("UP",
                         System.getenv().getOrDefault("SERVER_ID", "catalog-local")));
        });

        // placeholder query
        get("/item/:id", (req, res) -> {
            res.type("application/json");
            String id = req.params(":id");
            return "{ \"id\": \""+id+"\", \"name\": \"Demo Item\", \"stock\": 42 }";
        });
    }
}
