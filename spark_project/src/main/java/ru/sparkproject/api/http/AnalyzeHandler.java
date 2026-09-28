package ru.sparkproject.api.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.sparkproject.api.model.AnalyzeRequest;
import ru.sparkproject.api.model.AnalyzeResponse;
import ru.sparkproject.api.model.ErrorResponse;
import ru.sparkproject.api.service.SquareSumService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class AnalyzeHandler implements HttpHandler {
    private static final Logger LOGGER = Logger.getLogger(AnalyzeHandler.class.getName());
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int MAX_BODY_BYTES = 1_048_576;
    private static final int MAX_ARRAY_SIZE = 100_000;

    private final SquareSumService squareSumService;

    public AnalyzeHandler(SquareSumService squareSumService) {
        this.squareSumService = Objects.requireNonNull(squareSumService, "squareSumService");
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            if (!"/analyze".equals(exchange.getRequestURI().getPath())) {
                sendJson(exchange, 404, new ErrorResponse("Endpoint not found"));
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Allow", "POST");
                sendJson(exchange, 405, new ErrorResponse("Only POST is supported"));
                return;
            }

            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            if (contentType != null
                    && !contentType.toLowerCase(Locale.ROOT).startsWith("application/json")) {
                sendJson(exchange, 415, new ErrorResponse("Content-Type must be application/json"));
                return;
            }

            byte[] requestBody = readLimited(exchange.getRequestBody());
            AnalyzeRequest request = OBJECT_MAPPER.readValue(requestBody, AnalyzeRequest.class);
            List<Double> numbers = validate(request);

            double result = squareSumService.calculate(numbers);
            sendJson(exchange, 200, new AnalyzeResponse(result));
        } catch (RequestTooLargeException exception) {
            sendJson(exchange, 413, new ErrorResponse(exception.getMessage()));
        } catch (JsonProcessingException exception) {
            sendJson(exchange, 400, new ErrorResponse("Invalid JSON request"));
        } catch (IllegalArgumentException exception) {
            sendJson(exchange, 400, new ErrorResponse(exception.getMessage()));
        } catch (RuntimeException exception) {
            LOGGER.log(Level.SEVERE, "Spark calculation failed", exception);
            sendJson(exchange, 500, new ErrorResponse("Calculation failed"));
        } finally {
            exchange.close();
        }
    }

    private static List<Double> validate(AnalyzeRequest request) {
        if (request == null || request.array() == null) {
            throw new IllegalArgumentException("Field 'array' is required");
        }
        List<Double> numbers = request.array();
        if (numbers.size() > MAX_ARRAY_SIZE) {
            throw new IllegalArgumentException("Array must contain at most " + MAX_ARRAY_SIZE + " numbers");
        }

        for (Double number : numbers) {
            if (number == null || !Double.isFinite(number)) {
                throw new IllegalArgumentException("Array must contain only finite numbers");
            }
        }
        return numbers;
    }

    private static byte[] readLimited(InputStream inputStream) throws IOException {
        byte[] body = inputStream.readNBytes(MAX_BODY_BYTES + 1);
        if (body.length > MAX_BODY_BYTES) {
            throw new RequestTooLargeException("Request body must not exceed 1 MiB");
        }
        return body;
    }

    private static void sendJson(HttpExchange exchange, int statusCode, Object response) throws IOException {
        byte[] body = OBJECT_MAPPER.writeValueAsString(response).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, body.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(body);
        }
    }

    private static final class RequestTooLargeException extends IllegalArgumentException {
        private RequestTooLargeException(String message) {
            super(message);
        }
    }
}
