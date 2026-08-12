package io.sinaq.api.parallel;

import io.sinaq.api.request.RequestSpec;
import io.sinaq.api.response.ApiResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Runs multiple independent requests in parallel (V2). Each supplier must
 * return a fresh, unexecuted {@link RequestSpec}.
 */
public final class ParallelRequests {

    private ParallelRequests() {}

    public static List<ApiResponse> executeAll(List<Supplier<RequestSpec>> suppliers, int threads) {
        Objects.requireNonNull(suppliers, "suppliers");
        if (suppliers.isEmpty()) {
            return List.of();
        }
        int poolSize = Math.max(1, Math.min(threads, suppliers.size()));
        ExecutorService pool = Executors.newFixedThreadPool(poolSize);
        try {
            List<Callable<ApiResponse>> tasks = new ArrayList<>();
            for (Supplier<RequestSpec> s : suppliers) {
                tasks.add(() -> s.get().execute());
            }
            List<Future<ApiResponse>> futures = pool.invokeAll(tasks);
            List<ApiResponse> results = new ArrayList<>(futures.size());
            for (Future<ApiResponse> f : futures) {
                results.add(f.get(60, TimeUnit.SECONDS));
            }
            return List.copyOf(results);
        } catch (Exception e) {
            throw new io.sinaq.api.exception.SinaqException("Parallel execution failed", e);
        } finally {
            pool.shutdown();
        }
    }
}
