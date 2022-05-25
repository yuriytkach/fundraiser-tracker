package com.yuriytkach.tracker.fundraiser.integration;

import static com.yuriytkach.tracker.fundraiser.util.JsonMatcher.jsonEqualTo;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.yuriytkach.tracker.fundraiser.config.FundTrackerConfig;
import com.yuriytkach.tracker.fundraiser.model.CommandType;
import com.yuriytkach.tracker.fundraiser.model.ErrorResponse;
import com.yuriytkach.tracker.fundraiser.model.SlackResponse;

import io.quarkus.amazon.lambda.http.model.AwsProxyRequest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import one.util.streamex.StreamEx;

@QuarkusTest
@QuarkusTestResource(DynamoDbTestResource.class)
public class CmdFailuresAwsLambdaTest implements AwsLambdaTestCommon {

  @Inject
  FundTrackerConfig appConfig;

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
    request.setBody("token=" + appConfig.slackToken() + "&text=abc xyz");

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
    "track fund xyz 123 person",
    "create fund xyz 123"
  })
  void shouldReturnFailureIfUnknownCurrency(final String cmd) {
    final AwsProxyRequest request = createAwsProxyRequest();
    request.setBody("token=" + appConfig.slackToken() + "&text=" + cmd);

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

}
