package com.yuriytkach.tracker.fundraiser.integration;

import static com.yuriytkach.tracker.fundraiser.integration.DynamoDbTestResource.FUND;
import static com.yuriytkach.tracker.fundraiser.integration.DynamoDbTestResource.FUND_1_NAME;
import static com.yuriytkach.tracker.fundraiser.integration.DynamoDbTestResource.FUND_OWNER;
import static com.yuriytkach.tracker.fundraiser.util.JsonMatcher.jsonEqualTo;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.yuriytkach.tracker.fundraiser.model.CommandType;
import com.yuriytkach.tracker.fundraiser.model.ErrorResponse;
import com.yuriytkach.tracker.fundraiser.model.SlackResponse;
import com.yuriytkach.tracker.fundraiser.secret.SecretsReader;
import com.yuriytkach.tracker.fundraiser.service.dynamodb.DynamoDbFundStorageClient;

import io.quarkus.amazon.lambda.http.model.AwsProxyRequest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import one.util.streamex.StreamEx;

@QuarkusTest
@QuarkusTestResource(DynamoDbTestResource.class)
class CmdFailuresAwsLambdaTest implements AwsLambdaIntegrationTestCommon {

  private static final String SLACK_TOKEN = "slackToken";

  @InjectMock
  SecretsReader secretsReaderMock;

  @Inject
  DynamoDbFundStorageClient fundStorageClient;

  @BeforeEach
  void initSecretsReaderMock() {
    when(secretsReaderMock.readSecret(any())).thenReturn(Optional.of(SLACK_TOKEN));
  }

  @Test
  void shouldReturnForbiddenResponseForInvalidToken() {
    final AwsProxyRequest request = createAwsProxyRequest();
    request.setBody("token=adsf");

    given()
      .contentType(MediaType.APPLICATION_JSON)
      .accept(MediaType.APPLICATION_JSON)
      .body(request)
      .when()
      .post(LAMBDA_URL_PATH)
      .then()
      .statusCode(200)
      .body("statusCode", equalTo(403))
      .body("body", jsonEqualTo(new ErrorResponse("Forbidden")));
  }

  @Test
  void shouldReturnFailureIfCantMatchCommand() {
    final AwsProxyRequest request = createAwsProxyRequest();
    request.setBody("token=" + SLACK_TOKEN + "&text=abc xyz");

    final String expectedAllowedStatuses = StreamEx.of(CommandType.values()).map(CommandType::name).joining(",");
    given()
      .contentType(MediaType.APPLICATION_JSON)
      .accept(MediaType.APPLICATION_JSON)
      .body(request)
      .when()
      .post(LAMBDA_URL_PATH)
      .then()
      .statusCode(200)
      .body("statusCode", equalTo(200))
      .body("body", jsonEqualTo(SlackResponse.builder()
        .responseType(SlackResponse.RESPONSE_PRIVATE)
        .text(":x: Unknown command. Supported commands: " + expectedAllowedStatuses)
        .build()));
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "track " + FUND_1_NAME + " xyz 123 person",
    "create hohoho xyz 123",
    "update " + FUND_1_NAME + " curr:xyz",
  })
  void shouldReturnFailureIfUnknownCurrency(final String cmd) {
    final AwsProxyRequest request = createAwsProxyRequest();
    request.setBody("token=" + SLACK_TOKEN + "&user_id=" + FUND_OWNER + "&text=" + cmd);

    given()
      .contentType(MediaType.APPLICATION_JSON)
      .accept(MediaType.APPLICATION_JSON)
      .body(request)
      .when()
      .post(LAMBDA_URL_PATH)
      .then()
      .statusCode(200)
      .body("statusCode", equalTo(200))
      .body("body", jsonEqualTo(SlackResponse.builder()
        .responseType(SlackResponse.RESPONSE_PRIVATE)
        .text(":x: Unknown currency in text: " + cmd)
        .build()));
  }

  @ParameterizedTest
  @ValueSource(strings = {
    "list unknown",
    "track unknown eur 123 PP",
    "update unknown curr:eur",
  })
  void shouldReturnErrorForUnknownFund(final String cmd) {
    final AwsProxyRequest request = createAwsProxyRequest();
    request.setBody("token=" + SLACK_TOKEN + "&text=" + cmd);

    given()
      .contentType(MediaType.APPLICATION_JSON)
      .accept(MediaType.APPLICATION_JSON)
      .body(request)
      .when()
      .post(LAMBDA_URL_PATH)
      .then()
      .statusCode(200)
      .body("statusCode", equalTo(200))
      .body("body", jsonEqualTo(SlackResponse.builder()
        .responseType(SlackResponse.RESPONSE_PRIVATE)
        .text(":x: Fund not found by name: unknown")
        .build()));
  }

  @Test
  void shouldReturnFailureIfTrackForNotOwnedFund() {
    final AwsProxyRequest request = createAwsProxyRequest();
    request.setBody("token=" + SLACK_TOKEN + "&user_id=unknown_user"
      + "&text=track " + FUND.getName() + " " + FUND.getCurrency() + " 123 person 2022-02-01 15:13");

    given()
      .contentType(MediaType.APPLICATION_JSON)
      .accept(MediaType.APPLICATION_JSON)
      .body(request)
      .when()
      .post(LAMBDA_URL_PATH)
      .then()
      .statusCode(200)
      .body("statusCode", equalTo(200))
      .body("body", jsonEqualTo(SlackResponse.builder()
        .responseType(SlackResponse.RESPONSE_PRIVATE)
        .text(":x: Fund `" + FUND.getName() + "` owned by `" + FUND.getOwner() + "`: Can't track donations")
        .build()));
  }

  @Test
  void shouldReturnFailureIfTrackForDisabledFund() {
    try {
      fundStorageClient.save(FUND.toBuilder()
        .enabled(false)
        .updatedAt(Instant.now().minus(50, ChronoUnit.HOURS))
        .build());

      final AwsProxyRequest request = createAwsProxyRequest();
      request.setBody("token=" + SLACK_TOKEN + "&user_id=" + FUND_OWNER
        + "&text=track " + FUND.getName() + " " + FUND.getCurrency() + " 123 person 2022-02-01 15:13");

      given()
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .body(request)
        .when()
        .post(LAMBDA_URL_PATH)
        .then()
        .statusCode(200)
        .body("statusCode", equalTo(200))
        .body("body", jsonEqualTo(SlackResponse.builder()
          .responseType(SlackResponse.RESPONSE_PRIVATE)
          .text(":x: Fund `" + FUND.getName() + "` closed `2 days` ago: Can't track donations")
          .build()));
    } finally {
      fundStorageClient.save(FUND);
    }
  }

  @Test
  void shouldReturnFailureIfDeleteNotOwnedFund() {
    final AwsProxyRequest request = createAwsProxyRequest();
    request.setBody("token=" + SLACK_TOKEN + "&user_id=unknown_user"
      + "&text=delete " + FUND.getName());

    given()
      .contentType(MediaType.APPLICATION_JSON)
      .accept(MediaType.APPLICATION_JSON)
      .body(request)
      .when()
      .post(LAMBDA_URL_PATH)
      .then()
      .statusCode(200)
      .body("statusCode", equalTo(200))
      .body("body", jsonEqualTo(SlackResponse.builder()
        .responseType(SlackResponse.RESPONSE_PRIVATE)
        .text(":x: Fund `" + FUND.getName() + "` owned by `" + FUND.getOwner() + "`: Can't delete fund")
        .build()));
  }

  @Test
  void shouldReturnFailureIfUpdateNotOwnedFund() {
    final AwsProxyRequest request = createAwsProxyRequest();
    request.setBody("token=" + SLACK_TOKEN + "&user_id=unknown_user"
      + "&text=update " + FUND.getName() + " curr:EUR");

    given()
      .contentType(MediaType.APPLICATION_JSON)
      .accept(MediaType.APPLICATION_JSON)
      .body(request)
      .when()
      .post(LAMBDA_URL_PATH)
      .then()
      .statusCode(200)
      .body("statusCode", equalTo(200))
      .body("body", jsonEqualTo(SlackResponse.builder()
        .responseType(SlackResponse.RESPONSE_PRIVATE)
        .text(":x: Fund `" + FUND.getName() + "` owned by `" + FUND.getOwner() + "`: Can't update fund")
        .build()));
  }

}
