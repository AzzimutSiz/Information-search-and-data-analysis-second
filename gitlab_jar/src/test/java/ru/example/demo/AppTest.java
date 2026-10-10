package ru.example.demo;

import com.sun.net.httpserver.HttpServer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppTest {
    private static final String EXPECTED_RESPONSE = "Hello from Java!";

    @Test
    void returnsHelloOnHttpGet() throws Exception {
        HttpServer server = App.createServer(0);
        server.start();
        try {
            int port = server.getAddress().getPort();
            URI uri = URI.create("http://127.0.0.1:" + port + "/");
            HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
            HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode());
            assertEquals(EXPECTED_RESPONSE, response.body());
        } finally {
            server.stop(0);
        }
    }
}
