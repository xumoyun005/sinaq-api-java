package io.sinaq.api.graphql;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** GraphQL request body helpers (V3). */
public final class GraphQl {

    private GraphQl() {}

    public static Map<String, Object> query(String query) {
        Objects.requireNonNull(query, "query");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query", query);
        return body;
    }

    public static Map<String, Object> query(String query, Map<String, ?> variables) {
        Map<String, Object> body = new LinkedHashMap<>(query(query));
        if (variables != null && !variables.isEmpty()) {
            body.put("variables", variables);
        }
        return body;
    }

    public static Map<String, Object> mutation(String mutation, Map<String, ?> variables) {
        return query(mutation, variables);
    }
}
