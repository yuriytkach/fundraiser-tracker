package com.yuriytkach.tracker.fundraiser.integration;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.verify;

import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.junit.jupiter.api.Test;

import com.yuriytkach.tracker.fundraiser.privatbank.PrivatbankService;

import io.quarkus.amazon.lambda.http.model.AwsProxyRequest;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@QuarkusTest
class PrivatEventTest implements AwsLambdaIntegrationTestCommon {

  @InjectMock
  PrivatbankService privatbankService;
  @Test
  void shouldGetTransactionsFromPrivat() {
    final AwsProxyRequest request = createAwsProxyRequest("/privat/event", "POST", Map.of());

    given()
      .contentType(MediaType.APPLICATION_JSON)
      .accept(MediaType.APPLICATION_JSON)
      .body(request)
      .when()
      .post(LAMBDA_URL_PATH)
      .then()
      .statusCode(200)
      .body("statusCode", equalTo(200));

    verify(privatbankService).syncData();
  }

}
