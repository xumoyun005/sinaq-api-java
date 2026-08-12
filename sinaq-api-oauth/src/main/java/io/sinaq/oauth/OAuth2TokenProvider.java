package io.sinaq.oauth;

import io.sinaq.api.auth.RefreshingTokenProvider;
import io.sinaq.api.auth.TokenProvider;
import io.sinaq.api.context.RequestContext;
import io.sinaq.api.exception.SinaqConfigurationException;
import io.sinaq.api.http.HttpBody;
import io.sinaq.api.http.HttpHeaders;
import io.sinaq.api.http.HttpMethod;
import io.sinaq.api.http.HttpTimeout;
import io.sinaq.api.request.ApiRequest;
import io.sinaq.api.serialization.SerializationProvider;
import io.sinaq.jdk.JdkHttpEngine;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * OAuth2 token provider — client_credentials, password, refresh_token (V2/V4).
 */
public final class OAuth2TokenProvider implements TokenProvider {

  private final RefreshingTokenProvider delegate;

  public OAuth2TokenProvider(OAuth2Config config) {
    this(new JdkHttpEngine(), config);
  }

  OAuth2TokenProvider(io.sinaq.api.http.HttpEngine engine, OAuth2Config config) {
    Objects.requireNonNull(config, "config");
    this.delegate = new RefreshingTokenProvider(() -> fetch(engine, config));
  }

  @Override
  public String accessToken() {
    return delegate.accessToken();
  }

  public void invalidate() {
    delegate.invalidate();
  }

  private static RefreshingTokenProvider.TokenResponse fetch(
      io.sinaq.api.http.HttpEngine engine, OAuth2Config config) {
    String body = buildTokenBody(config);
    ApiRequest request = new ApiRequest(
        HttpMethod.POST,
        URI.create(config.tokenUrl()),
        HttpHeaders.builder().set("Content-Type", "application/x-www-form-urlencoded").build(),
        java.util.List.of(),
        HttpBody.ofBytes(body.getBytes(java.nio.charset.StandardCharsets.UTF_8),
            HttpBody.FORM),
        HttpTimeout.of(Duration.ofSeconds(10), Duration.ofSeconds(30)),
        RequestContext.create());
    var response = engine.execute(request);
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new SinaqConfigurationException(
          "OAuth2 token request failed: HTTP " + response.statusCode());
    }
    String json = new String(response.rawBody(), java.nio.charset.StandardCharsets.UTF_8);
    Map<?, ?> map = SerializationProvider.builtIn().deserialize(json, Map.class);
    Object token = map.get("access_token");
    if (token == null) {
      throw new SinaqConfigurationException("OAuth2 response missing access_token");
    }
    Object expiresIn = map.get("expires_in");
    Instant expiresAt = expiresIn instanceof Number n
        ? Instant.now().plusSeconds(n.longValue() - 30)
        : null;
    return new RefreshingTokenProvider.TokenResponse(token.toString(), expiresAt);
  }

  private static String buildTokenBody(OAuth2Config config) {
    String grant = config.grantType();
    StringBuilder body = new StringBuilder("grant_type=").append(url(grant));
    switch (grant) {
      case "password" -> {
        body.append("&client_id=").append(url(config.clientId()));
        body.append("&client_secret=").append(url(config.clientSecret()));
        body.append("&username=").append(url(config.username()
            .orElseThrow(() -> new SinaqConfigurationException("password grant requires username"))));
        body.append("&password=").append(url(config.password()
            .orElseThrow(() -> new SinaqConfigurationException("password grant requires password"))));
        config.scope().ifPresent(s -> body.append("&scope=").append(url(s)));
      }
      case "refresh_token" -> {
        body.append("&client_id=").append(url(config.clientId()));
        body.append("&client_secret=").append(url(config.clientSecret()));
        body.append("&refresh_token=").append(url(config.refreshToken()
            .orElseThrow(() -> new SinaqConfigurationException("refresh_token grant requires refreshToken"))));
      }
      default -> {
        body.append("&client_id=").append(url(config.clientId()));
        body.append("&client_secret=").append(url(config.clientSecret()));
        config.scope().ifPresent(s -> body.append("&scope=").append(url(s)));
      }
    }
    return body.toString();
  }

  private static String url(String s) {
    return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
  }

  /** OAuth2 client configuration. */
  public record OAuth2Config(
      String tokenUrl,
      String clientId,
      String clientSecret,
      String grantType,
      Optional<String> scope,
      Optional<String> username,
      Optional<String> password,
      Optional<String> refreshToken) {

    public OAuth2Config {
      Objects.requireNonNull(tokenUrl);
      Objects.requireNonNull(clientId);
      Objects.requireNonNull(clientSecret);
      if (grantType == null || grantType.isBlank()) {
        grantType = "client_credentials";
      }
      scope = scope == null ? Optional.empty() : scope;
      username = username == null ? Optional.empty() : username;
      password = password == null ? Optional.empty() : password;
      refreshToken = refreshToken == null ? Optional.empty() : refreshToken;
    }

    public static Builder builder(String tokenUrl) {
      return new Builder(tokenUrl);
    }

    public static final class Builder {
      private final String tokenUrl;
      private String clientId;
      private String clientSecret;
      private String grantType = "client_credentials";
      private String scope;
      private String username;
      private String password;
      private String refreshToken;

      Builder(String tokenUrl) { this.tokenUrl = tokenUrl; }

      public Builder clientId(String v)       { clientId = v; return this; }
      public Builder clientSecret(String v)   { clientSecret = v; return this; }
      public Builder grantType(String v)      { grantType = v; return this; }
      public Builder scope(String v)          { scope = v; return this; }
      public Builder username(String v)       { username = v; return this; }
      public Builder password(String v)       { password = v; return this; }
      public Builder refreshToken(String v)   { refreshToken = v; return this; }

      public OAuth2Config build() {
        return new OAuth2Config(tokenUrl, clientId, clientSecret, grantType,
            opt(scope), opt(username), opt(password), opt(refreshToken));
      }

      private static Optional<String> opt(String v) {
        return v == null ? Optional.empty() : Optional.of(v);
      }
    }
  }
}
