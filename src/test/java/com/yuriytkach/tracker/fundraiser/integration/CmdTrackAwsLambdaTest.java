package com.yuriytkach.tracker.fundraiser.integration;

import static com.yuriytkach.tracker.fundraiser.integration.DynamoDbTestResource.FUND;
import static com.yuriytkach.tracker.fundraiser.integration.DynamoDbTestResource.FUNDS_TABLE;
import static com.yuriytkach.tracker.fundraiser.integration.DynamoDbTestResource.FUND_1_TABLE;
import static com.yuriytkach.tracker.fundraiser.integration.DynamoDbTestResource.FUND_OWNER;
import static com.yuriytkach.tracker.fundraiser.service.dynamodb.DynamoDbDonationClientDonation.COL_AMOUNT;
import static com.yuriytkach.tracker.fundraiser.service.dynamodb.DynamoDbDonationClientDonation.COL_CURR;
import static com.yuriytkach.tracker.fundraiser.service.dynamodb.DynamoDbDonationClientDonation.COL_ID;
import static com.yuriytkach.tracker.fundraiser.service.dynamodb.DynamoDbDonationClientDonation.COL_PERSON;
import static com.yuriytkach.tracker.fundraiser.service.dynamodb.DynamoDbDonationClientDonation.COL_TIME;
import static com.yuriytkach.tracker.fundraiser.util.JsonMatcher.jsonEqualTo;
import static io.restassured.RestAssured.given;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.yuriytkach.tracker.fundraiser.config.FundTrackerConfig;
import com.yuriytkach.tracker.fundraiser.forex.ForexService;
import com.yuriytkach.tracker.fundraiser.forex.MonoCurrencyRate;
import com.yuriytkach.tracker.fundraiser.model.Currency;
import com.yuriytkach.tracker.fundraiser.model.Donation;
import com.yuriytkach.tracker.fundraiser.model.Fund;
import com.yuriytkach.tracker.fundraiser.model.SlackResponse;
import com.yuriytkach.tracker.fundraiser.model.slack.SlackBlock;
import com.yuriytkach.tracker.fundraiser.model.slack.SlackText;
import com.yuriytkach.tracker.fundraiser.secret.SecretsReader;
import com.yuriytkach.tracker.fundraiser.service.FundService;
import com.yuriytkach.tracker.fundraiser.service.IdGenerator;
import com.yuriytkach.tracker.fundraiser.slack.SlackProperties;

import io.quarkus.amazon.lambda.http.model.AwsProxyRequest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

@Slf4j
@QuarkusTest
@QuarkusTestResource(DynamoDbTestResource.class)
class CmdTrackAwsLambdaTest extends AbstractFundOperationsTestCommon implements AwsLambdaIntegrationTestCommon {

  private static final String SLACK_TOKEN = "slack-token";

  @Inject
  FundTrackerConfig appConfig;

  @Inject
  SlackProperties slackProperties;

  @InjectMock
  SecretsReader secretsReaderMock;

  @Inject
  DynamoDbClient dynamoDB;

  @Inject
  ForexService forexService;

  @InjectMock
  IdGenerator idGenerator;

  @BeforeEach
  void setupCurrencies() {
    forexService.setCurrencies(List.of(
      new MonoCurrencyRate(
        Currency.EUR.getIsoCode(),
        Currency.USD.getIsoCode(),
        11111L,
        1.1,
        0.9,
        null
      )
    ));
  }

  @BeforeEach
  void initSecretsReaderMock() {
    when(secretsReaderMock.readSecret(slackProperties.tokenSecretName())).thenReturn(Optional.of(SLACK_TOKEN));
  }

  @ParameterizedTest
  @CsvSource({
    "'', carFund, green",
    "' /desc/', desc, green",
    "' color', carFund, color",
    "' /desc/ color', desc, color"
  })
  void shouldCreateFund(final String cmdTextSuffix, final String expectedDesc, final String expectedColor) {
    final Currency fund2Currency = Currency.USD;

    final AwsProxyRequest request = createAwsProxyRequest();
    request.setBody("token=" + SLACK_TOKEN + "&user_id=userAbc"
      + "&text=create " + FUND_2_NAME + " " + fund2Currency + " 123" + cmdTextSuffix);

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
        .text(":white_check_mark: Created fund `" + FUND_2_NAME + "`")
        .build()));

    final Optional<Fund> fundOpt = getFundDirectlyByName(FUND_2_NAME);
    assertThat(fundOpt).isPresent();
    final Fund fund2 = fundOpt.get();
    final String expectedFund2TableName = FundService.FUND_TABLE_PREFIX + FUND_2_NAME;
    assertThat(fund2.getId()).isEqualTo(expectedFund2TableName);
    assertThat(fund2.isEnabled()).isTrue();
    assertThat(fund2.getName()).isEqualTo(FUND_2_NAME);
    assertThat(fund2.getOwner()).isEqualTo("userAbc");
    assertThat(fund2.getDescription()).isEqualTo(expectedDesc);
    assertThat(fund2.getColor()).isEqualTo(expectedColor);
    assertThat(fund2.getGoal()).isEqualTo(123);
    assertThat(fund2.getRaised()).isEqualTo(0);
    assertThat(fund2.getCurrency()).isEqualTo(fund2Currency);
    assertThat(fund2.getCreatedAt()).isCloseTo(Instant.now(), within(1, SECONDS));
    assertThat(fund2.getUpdatedAt()).isCloseTo(Instant.now(), within(1, SECONDS));

    final ListTablesResponse allTablesResponse = dynamoDB.listTables();
    assertThat(allTablesResponse.tableNames()).containsExactlyInAnyOrder(
      expectedFund2TableName, FUNDS_TABLE, FUND_1_TABLE
    );
  }

  @Test
  void shouldDeleteFund() {
    final AwsProxyRequest request = createAwsProxyRequest();
    request.setBody("token=" + SLACK_TOKEN + "&user_id=user2"
      + "&text=create " + FUND_2_NAME + " usd 123");

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
        .text(":white_check_mark: Created fund `" + FUND_2_NAME + "`")
        .build()));

    assertThat(getFundDirectlyByName(FUND_2_NAME)).isPresent();

    request.setBody("token=" + SLACK_TOKEN + "&user_id=user2"
      + "&text=delete " + FUND_2_NAME);

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
        .text(":white_check_mark: Deleted fund `" + FUND_2_NAME + "`")
        .build()));

    final ListTablesResponse allTablesResponse = dynamoDB.listTables();
    assertThat(allTablesResponse.tableNames()).containsExactlyInAnyOrder(
      FUNDS_TABLE, FUND_1_TABLE
    );
  }

  @ParameterizedTest
  @CsvSource({
    "'', noname",
    "' person', person"
  })
  void shouldReturnOKIfTrackSuccessful(final String person, final String expectedPerson) {
    when(idGenerator.generateId()).thenReturn(ITEM_ID_1);

    final AwsProxyRequest request = createAwsProxyRequest();
    request.setBody("token=" + SLACK_TOKEN + "&user_id=" + FUND_OWNER
      + "&text=track " + FUND.getName() + " " + FUND.getCurrency() + " 123" + person + " 2022-02-01 15:13");

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
        .text(":white_check_mark: Tracked 123 "
          + FUND.getCurrency() + " by " + expectedPerson + " at 2022-02-01 15:13"
          + " - :open_book: `fundy` 22.30% [223 of 1000] EUR - :bank:-1")
        .build()));

    final Optional<Donation> donation = getDonationDirectlyById(ITEM_ID_1.toString());
    assertThat(donation).hasValue(Donation.builder()
      .id(ITEM_ID_1.toString())
      .currency(FUND.getCurrency())
      .amount(123)
      .dateTime(Instant.parse("2022-02-01T12:13:00Z"))
      .person(expectedPerson)
      .build());

    final Optional<Fund> fund = getFundDirectlyByName(FUND.getName());
    assertThat(fund).hasValue(FUND.toBuilder()
      .raised(FUND.getRaised() + 123)
      .updatedAt(Instant.parse("2022-02-01T12:13:00Z"))
      .build());
  }

  @Test
  void shouldUpdateTotalsAfterTrackingDiffCurrency() {
    when(idGenerator.generateId()).thenReturn(ITEM_ID_1);

    final AwsProxyRequest request = createAwsProxyRequest();
    request.setBody("token=" + SLACK_TOKEN + "&user_id=" + FUND_OWNER
      + "&text=track " + FUND.getName() + " usd 100 person 2022-02-01 15:13");

    given()
      .contentType(MediaType.APPLICATION_JSON)
      .accept(MediaType.APPLICATION_JSON)
      .body(request)
      .when()
      .post(LAMBDA_URL_PATH)
      .then()
      .statusCode(200);

    final var monoCurrency = forexService.getCurrencies().stream()
      .filter(monoRate -> monoRate.getCurrencyCodeA() == Currency.EUR.getIsoCode()
        && monoRate.getCurrencyCodeB() == Currency.USD.getIsoCode())
      .findFirst()
      .orElseThrow();

    final Optional<Fund> fund = getFundDirectlyByName(FUND.getName());
    assertThat(fund).hasValue(FUND.toBuilder()
      .raised(FUND.getRaised() + (int) (100 / monoCurrency.getRateSell()))
      .updatedAt(Instant.parse("2022-02-01T12:13:00Z"))
      .build());
  }

  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  void shouldUpdateFund(final boolean enable) {
    when(idGenerator.generateId()).thenReturn(ITEM_ID_1);

    fundStorageClient.save(FUND.toBuilder().enabled(!enable).build());

    final AwsProxyRequest request = createAwsProxyRequest();
    request.setBody(
      "token=" + SLACK_TOKEN + "&user_id=" + FUND_OWNER
        + "&text=update " + FUND.getName()
        + (enable ? " open" : " close")
        + " goal:4242 bank:acc1,acc2"
    );

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
        .text(":white_check_mark: The fund with name: `" + FUND.getName() + "` has been updated successfully!")
        .build()));

    final Optional<Fund> fund = getFundDirectlyByName(FUND.getName());
    assertThat(fund).hasValue(FUND.toBuilder()
      .enabled(enable)
      .goal(4242)
      .bankAccounts(Set.of("acc1", "acc2"))
      .build());
  }

  @Test
  void shouldReturnResultForListAllFundsCommand() {
    final AwsProxyRequest request = createAwsProxyRequest();
    request.setBody("token=" + SLACK_TOKEN + "&user_id=" + FUND_OWNER + "&text=list");

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
        .text("""
          :white_check_mark: All Funds
          :open_book: 10.00% `fundy` [100 of 1000] EUR - description [red] - 0 h - :bank:-1\
          """)
        .build()));
  }

  @Test
  void shouldReturnResultForListFundersCommand() {
    addDonationDirectly(ITEM_ID_1, "EUR", 987, Instant.parse("2022-05-10T00:10:30.107591Z"), "PP");

    final AwsProxyRequest request = createAwsProxyRequest();
    request.setBody("token=" + SLACK_TOKEN + "&text=list " + FUND.getName());

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
        .text("Funders of `fundy`")
        .blocks(List.of(
          SlackBlock.builder()
            .header(SlackText.builder().plainText(":white_check_mark: Funders of `fundy`").build())
            .build(),
          SlackBlock.builder()
            .context(List.of(SlackText.builder().markdownText(
              "2022-05-10 03:10 - EUR   987 - PP (_00000000-0000-0001-0000-000000000001_)"
            ).build()))
            .build()
        ))
        .build()));
  }

  @Test
  void shouldReturnResultForHelpCommand() {
    final AwsProxyRequest request = createAwsProxyRequest();
    request.setBody("token=" + SLACK_TOKEN + "&text=help");

    final String supportedCurrencies = StreamEx.of(Currency.values())
      .map(Currency::name)
      .joining(", ");
    final String expectedText = appConfig.helpText().replace("<supported_currencies>", supportedCurrencies);

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
        .text("Fund command help")
        .blocks(List.of(
          SlackBlock.builder()
            .header(SlackText.builder().plainText(":white_check_mark: Fund command help").build())
            .build(),
          SlackBlock.builder()
            .context(List.of(SlackText.builder().markdownText(expectedText).build()))
            .build()
        ))
        .build()));
  }

  private void addDonationDirectly(
    final UUID itemId,
    final String curr,
    final int amount,
    final Instant dateTime,
    final String person
  ) {
    final Map<String, AttributeValue> item = new HashMap<>();
    item.put(COL_ID, AttributeValue.builder().s(itemId.toString()).build());
    item.put(COL_CURR, AttributeValue.builder().s(curr).build());
    item.put(COL_PERSON, AttributeValue.builder().s(person).build());
    item.put(COL_AMOUNT, AttributeValue.builder().n(String.valueOf(amount)).build());
    item.put(COL_TIME, AttributeValue.builder().s(dateTime.toString()).build());

    final var putRequest = PutItemRequest.builder()
      .tableName(FUND_1_TABLE)
      .item(item)
      .build();
    dynamoDB.putItem(putRequest);
  }

}
