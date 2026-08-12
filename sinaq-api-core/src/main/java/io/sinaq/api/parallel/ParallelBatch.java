package io.sinaq.api.parallel;

import io.sinaq.api.exception.SinaqException;
import io.sinaq.api.request.RequestSpec;
import io.sinaq.api.response.ApiResponse;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Named parallel batch execution with timeout (V3).
 */
public final class ParallelBatch {

    private ParallelBatch() {}

    public static ParallelResult execute(Map<String, Supplier<RequestSpec>> namedSuppliers,
                                         int threads, Duration timeout) {
        Objects.requireNonNull(namedSuppliers, "namedSuppliers");
        if (namedSuppliers.isEmpty()) {
            return new ParallelResult(Map.of());
        }
        int poolSize = Math.max(1, Math.min(threads, namedSuppliers.size()));
        ExecutorService pool = Executors.newFixedThreadPool(poolSize);
        try {
            Map<String, Future<ApiResponse>> futures = new LinkedHashMap<>();
            for (Map.Entry<String, Supplier<RequestSpec>> e : namedSuppliers.entrySet()) {
                Callable<ApiResponse> task = () -> e.getValue().get().execute();
                futures.put(e.getKey(), pool.submit(task));
            }
            Map<String, ApiResponse> results = new LinkedHashMap<>();
            long ms = timeout != null ? timeout.toMillis() : 60_000L;
            for (Map.Entry<String, Future<ApiResponse>> e : futures.entrySet()) {
                results.put(e.getKey(), e.getValue().get(ms, TimeUnit.MILLISECONDS));
            }
            return new ParallelResult(results);
        } catch (Exception e) {
            throw new SinaqException("Parallel batch failed", e);
        } finally {
            pool.shutdown();
        }
    }

    public record ParallelResult(Map<String, ApiResponse> responses) {
        public ParallelResult {
            responses = responses == null ? Map.of() : Map.copyOf(responses);
        }

        public ApiResponse get(String name) {
            return responses.get(name);
        }

        public List<ApiResponse> all() {
            return new ArrayList<>(responses.values());
        }
    }
}
