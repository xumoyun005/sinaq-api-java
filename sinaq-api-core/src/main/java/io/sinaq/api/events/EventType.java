package io.sinaq.api.events;

/** Framework-neutral lifecycle events (spec §21). */
public enum EventType {
    TEST_STARTED,
    REQUEST_CREATED,
    REQUEST_STARTED,
    REQUEST_SENT,
    RESPONSE_RECEIVED,
    ASSERTION_STARTED,
    ASSERTION_PASSED,
    ASSERTION_FAILED,
    TEST_FINISHED,
    ERROR_OCCURRED,
    /** V2 — polling lifecycle */
    POLL_STARTED,
    POLL_ATTEMPT,
    POLL_SUCCEEDED,
    POLL_FAILED,
    /** V2 — per-request performance snapshot */
    PERFORMANCE_RECORDED,
    /** V2 — plugin registered */
    PLUGIN_LOADED,
    /** V3 — interceptor applied */
    INTERCEPTOR_APPLIED,
    /** V3 — exchange recorded for contract/HAR */
    EXCHANGE_RECORDED,
    /** V3 — contract verified */
    CONTRACT_VERIFIED,
    /** V3 — database validation */
    DB_VALIDATION,
    /** V4 — response cache */
    CACHE_HIT,
    CACHE_MISS,
    /** V4 — replay */
    REPLAY_EXECUTED,
    /** V4 — messaging */
    MESSAGE_PUBLISHED,
    MESSAGE_VALIDATED,
    /** V4 — distributed batch */
    DISTRIBUTED_BATCH_COMPLETED,
    /** V4 — OpenAPI validated */
    OPENAPI_VALIDATED
}
