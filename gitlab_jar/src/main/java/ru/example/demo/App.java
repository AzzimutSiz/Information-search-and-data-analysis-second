package ru.example.demo;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public final class App {
    private static final String HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 8080;
    private static final byte[] RESPONSE_BODY = "Hello from Java!".getBytes(StandardCharsets.UTF_8);

    private App() {
    }

    public static HttpServer createServer(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(HOST, port), 0);
        server.createContext("/", App::handleRequest);
        return server;
    }

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", String.valueOf(DEFAULT_PORT)));
        HttpServer server = createServer(port);
        server.start();
        System.out.println("Java HTTP server started on http://" + HOST + ":" + port + "/");
    }

    private static void handleRequest(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Allow", "GET");
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(200, RESPONSE_BODY.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(RESPONSE_BODY);
        }
    }
}
