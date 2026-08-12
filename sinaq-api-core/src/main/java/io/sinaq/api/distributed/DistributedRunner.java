package io.sinaq.api.distributed;

import io.sinaq.api.client.ApiClient;
import io.sinaq.api.exception.SinaqException;
import io.sinaq.api.request.RequestSpec;
import io.sinaq.api.response.ApiResponse;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Distributed-style fan-out: same request template against multiple worker clients (V4).
 */
public final class DistributedRunner {

    private DistributedRunner() {}

    public static DistributedResult execute(Function<ApiClient, RequestSpec> requestFactory,
                                            List<ApiClient> workers,
                                            int threads,
                                            Duration timeout) {
        Objects.requireNonNull(requestFactory, "requestFactory");
        Objects.requireNonNull(workers, "workers");
        if (workers.isEmpty()) {
            return new DistributedResult(List.of());
        }
        int poolSize = Math.max(1, Math.min(threads, workers.size()));
        ExecutorService pool = Executors.newFixedThreadPool(poolSize);
        try {
            List<Callable<WorkerResult>> tasks = new ArrayList<>();
            for (int i = 0; i < workers.size(); i++) {
                final int idx = i;
                tasks.add(() -> {
                    ApiResponse response = requestFactory.apply(workers.get(idx)).execute();
                    return new WorkerResult(idx, response.status(), response.responseTime().toMillis());
                });
            }
            List<Future<WorkerResult>> futures = pool.invokeAll(tasks);
            List<WorkerResult> results = new ArrayList<>();
            long ms = timeout != null ? timeout.toMillis() : 60_000L;
            for (Future<WorkerResult> f : futures) {
                results.add(f.get(ms, TimeUnit.MILLISECONDS));
            }
            return new DistributedResult(List.copyOf(results));
        } catch (Exception e) {
            throw new SinaqException("Distributed execution failed", e);
        } finally {
            pool.shutdown();
        }
    }

    public record WorkerResult(int workerIndex, int status, long responseTimeMs) {}

    public record DistributedResult(List<WorkerResult> workers) {
        public DistributedResult {
            workers = workers == null ? List.of() : List.copyOf(workers);
        }

        public boolean allStatus(int expected) {
            return workers.stream().allMatch(w -> w.status() == expected);
        }
    }
}
