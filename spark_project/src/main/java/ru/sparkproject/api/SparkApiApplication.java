package ru.sparkproject.api;

import com.sun.net.httpserver.HttpServer;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import ru.sparkproject.api.http.AnalyzeHandler;
import ru.sparkproject.api.service.SquareSumService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public final class SparkApiApplication {
    private static final Logger LOGGER = Logger.getLogger(SparkApiApplication.class.getName());
    private static final int DEFAULT_API_PORT = 8090;

    private SparkApiApplication() {
    }

    public static void main(String[] args) throws IOException {
        int apiPort = readPort(System.getenv("API_PORT"));
        SparkConf sparkConf = createSparkConf();
        JavaSparkContext sparkContext = new JavaSparkContext(sparkConf);

        HttpServer server;
        ExecutorService httpExecutor = Executors.newFixedThreadPool(
                Math.max(2, Runtime.getRuntime().availableProcessors())
        );

        try {
            server = HttpServer.create(new InetSocketAddress("0.0.0.0", apiPort), 0);
        } catch (IOException exception) {
            httpExecutor.shutdownNow();
            sparkContext.close();
            throw exception;
        }

        server.createContext("/analyze", new AnalyzeHandler(new SquareSumService(sparkContext)));
        server.setExecutor(httpExecutor);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Stopping Spark API");
            server.stop(1);
            httpExecutor.shutdown();
            sparkContext.close();
        }, "spark-api-shutdown"));

        server.start();
        LOGGER.info(() -> "Spark API is listening on port " + apiPort
                + " and connected to " + sparkContext.master());
    }

    private static SparkConf createSparkConf() {
        String masterUrl = environmentOrDefault("SPARK_MASTER_URL", "local[*]");
        SparkConf conf = new SparkConf()
                .setAppName("square-sum-api")
                .setMaster(masterUrl);

        String driverHost = System.getenv("SPARK_DRIVER_HOST");
        if (driverHost != null && !driverHost.isBlank()) {
            conf.set("spark.driver.host", driverHost);
            conf.set("spark.driver.bindAddress", "0.0.0.0");
        }

        return conf;
    }

    private static int readPort(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_API_PORT;
        }

        try {
            int port = Integer.parseInt(value);
            if (port < 1 || port > 65_535) {
                throw new IllegalArgumentException("API_PORT must be between 1 and 65535");
            }
            return port;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("API_PORT must be a number", exception);
        }
    }

    private static String environmentOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
