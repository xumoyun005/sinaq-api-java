package io.sinaq.api.contract;

import io.sinaq.api.exception.SinaqAssertionException;
import io.sinaq.api.recording.RecordedExchange;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class ContractVerifierTest {

  private static RecordedExchange exchange() {
    return new RecordedExchange(
        "r1", "POST", "http://localhost/loan",
        Map.of("Authorization", "Bearer tok"),
        "{\"amount\":100}",
        200, Map.of(), "{\"success\":true}", 50, Instant.now());
  }

  @Test
  void verifiesMatchingContract() {
    ContractVerifier.verify(exchange(), ContractExpectation.builder()
        .method("POST")
        .urlContains("/loan")
        .status(200)
        .responseBodyContains("success")
        .build());
  }

  @Test
  void failsOnMismatch() {
    assertThatThrownBy(() -> ContractVerifier.verify(exchange(),
        ContractExpectation.builder().status(404).build()))
        .isInstanceOf(SinaqAssertionException.class);
  }

  @Test
  void failsOnRequestBodyMismatch() {
    assertThatThrownBy(() -> ContractVerifier.verify(exchange(),
        ContractExpectation.builder().requestBodyContains("nope").build()))
        .isInstanceOf(SinaqAssertionException.class);
  }

  @Test
  void verifyAnyFailsWhenNoMatch() {
    assertThatThrownBy(() -> ContractVerifier.verifyAny(List.of(exchange()),
        ContractExpectation.builder().method("DELETE").build()))
        .isInstanceOf(SinaqAssertionException.class);
  }
}
