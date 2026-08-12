package io.sinaq.api.context;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class RequestContextTest {

    @Test
    void createGeneratesUniqueRequestIds() {
        RequestContext a = RequestContext.create();
        RequestContext b = RequestContext.create();
        assertThat(a.requestId()).isNotBlank();
        assertThat(a.requestId()).isNotEqualTo(b.requestId());
        assertThat(a.correlationId()).isEmpty();
        assertThat(a.metadata()).isEmpty();
    }

    @Test
    void correlationIdIsCarried() {
        RequestContext c = RequestContext.withCorrelationId("trace-1");
        assertThat(c.correlationId()).contains("trace-1");
    }

    @Test
    void metadataIsUnmodifiable() {
        RequestContext c = RequestContext.create();
        assertThatThrownBy(() -> c.metadata().put("k", "v"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
