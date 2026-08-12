package io.sinaq.jsonschema;

import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import io.sinaq.api.assertion.SchemaValidator;
import io.sinaq.api.client.ApiClient;
import io.sinaq.api.exception.SinaqAssertionException;
import io.sinaq.api.plugin.SinaqPlugin;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NetworkNT JSON Schema {@link SchemaValidator} (V2).
 */
public final class NetworkntSchemaValidator implements SchemaValidator {

  private static final JsonSchemaFactory FACTORY =
      JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
  private final ConcurrentHashMap<String, JsonSchema> cache = new ConcurrentHashMap<>();

  @Override
  public void validate(String jsonBody, String schemaJson) {
    JsonSchema schema = cache.computeIfAbsent(schemaJson, FACTORY::getSchema);
    Set<ValidationMessage> errors = schema.validate(jsonBody, com.networknt.schema.InputFormat.JSON);
    if (!errors.isEmpty()) {
      throw new SinaqAssertionException("JSON Schema validation failed: " + errors.iterator().next());
    }
  }

  public static final class Plugin implements SinaqPlugin {
    @Override public String id() { return "jsonschema"; }
    @Override public void onClientBuild(ApiClient.Builder builder) {
      builder.schemaValidator(new NetworkntSchemaValidator());
    }
  }
}
