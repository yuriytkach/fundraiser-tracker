package com.yuriytkach.tracker.fundraiser.secret.awssm;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretRequest;

@QuarkusTest
class AwsSecretsManagerReaderTest {

  private static final String SECRET_ID = "id";
  private static final String SECRET_VALUE = "value";

  @Inject
  SecretsManagerClient secretsManagerClient;

  @Inject
  AwsSecretsManagerReader tested;

  @Test
  void canReadSecret() {
    secretsManagerClient.createSecret(
      CreateSecretRequest.builder().name(SECRET_ID).secretString(SECRET_VALUE).build()
    );

    assertThat(tested.readSecret(SECRET_ID)).hasValue(SECRET_VALUE);

    assertThat(tested.readSecret("UnknownID")).isEmpty();
  }

}
