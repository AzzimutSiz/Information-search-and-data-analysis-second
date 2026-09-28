package ru.sparkproject.api.service;

import org.apache.spark.api.java.JavaSparkContext;

import java.util.List;
import java.util.Objects;

public final class SquareSumService {
    private final JavaSparkContext sparkContext;

    public SquareSumService(JavaSparkContext sparkContext) {
        this.sparkContext = Objects.requireNonNull(sparkContext, "sparkContext");
    }

    public double calculate(List<Double> numbers) {
        Objects.requireNonNull(numbers, "numbers");

        return sparkContext
                .parallelize(numbers)
                .mapToDouble(number -> number * number)
                .sum();
    }
}
