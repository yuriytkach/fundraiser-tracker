package com.yuriytkach.tracker.fundraiser.integration;

import static com.yuriytkach.tracker.fundraiser.integration.DynamoDbTestResource.FUND;
import static com.yuriytkach.tracker.fundraiser.integration.DynamoDbTestResource.FUND_1_TABLE;
import static com.yuriytkach.tracker.fundraiser.service.dynamodb.DynamoDbDonationClientDonation.COL_ID;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.core.MediaType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.yuriytkach.tracker.fundraiser.model.Currency;
import com.yuriytkach.tracker.fundraiser.model.Donation;
import com.yuriytkach.tracker.fundraiser.model.Fund;

import io.quarkus.amazon.lambda.http.model.AwsProxyRequest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@QuarkusTest
@QuarkusTestResource(DynamoDbTestResource.class)
@DisplayName("Happy path integration test for tracking donation from Monobank statement")
class MonobankHookTest extends FundCleanupTestCommon implements AwsLambdaTestCommon {

  private static final String MONO_STATEMENT_ID = "monoStatementId";
  private static final Instant MONO_STATEMENT_TIME = Instant.ofEpochSecond(1667731529);
  private static final int MONO_STATEMENT_AMOUNT = 10;

  @AfterEach
  void cleanUpDataFromMono() {
    deleteItemByIdDirectly(FUND_1_TABLE, COL_ID, MONO_STATEMENT_ID);
  }

  @Test
  @SneakyThrows
  void shouldTrackDonation() {
    final AwsProxyRequest request = createAwsProxyRequest(
      "/mono/hook",
      "POST",
      Map.of(
        "Content-Type", "application/json"
      )
    );

    final var statementBody = Files.readString(
      Path.of(this.getClass().getClassLoader().getResource("monobank_hook_body.json").toURI())
    );

    request.setBody(statementBody);

    given()
      .contentType(MediaType.APPLICATION_JSON)
      .accept(MediaType.APPLICATION_JSON)
      .body(request)
      .when()
      .post(LAMBDA_URL_PATH)
      .then()
      .statusCode(200)
      .body("statusCode", equalTo(200))
      .body("body", nullValue());

    final Optional<Donation> donation = getDonationDirectlyById(MONO_STATEMENT_ID);
    assertThat(donation).hasValue(Donation.builder()
      .id(MONO_STATEMENT_ID)
      .currency(Currency.EUR)
      .amount(MONO_STATEMENT_AMOUNT)
      .dateTime(MONO_STATEMENT_TIME)
      .person("YuriyT")
      .build());

    final Optional<Fund> fund = getFundDirectlyByName(FUND.getName());
    assertThat(fund).hasValue(FUND.toBuilder()
      .raised(FUND.getRaised() + MONO_STATEMENT_AMOUNT)
      .updatedAt(MONO_STATEMENT_TIME)
      .build());
  }

}
