package com.yuriytkach.tracker.fundraiser.integration;

import static com.yuriytkach.tracker.fundraiser.integration.DynamoDbTestResource.FUND;
import static com.yuriytkach.tracker.fundraiser.integration.DynamoDbTestResource.FUNDS_TABLE;
import static com.yuriytkach.tracker.fundraiser.integration.DynamoDbTestResource.FUND_1_TABLE;
import static com.yuriytkach.tracker.fundraiser.integration.DynamoDbTestResource.FUND_OWNER;
import static com.yuriytkach.tracker.fundraiser.service.dynamodb.DynamoDbDonationClientDonation.ALL_ATTRIBUTES;
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
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.yuriytkach.tracker.fundraiser.config.FundTrackerConfig;
import com.yuriytkach.tracker.fundraiser.forex.ForexService;
import com.yuriytkach.tracker.fundraiser.forex.MonoCurrencyRate;
import com.yuriytkach.tracker.fundraiser.model.Currency;
import com.yuriytkach.tracker.fundraiser.model.Donation;
import com.yuriytkach.tracker.fundraiser.model.Fund;
import com.yuriytkach.tracker.fundraiser.model.SlackResponse;
import com.yuriytkach.tracker.fundraiser.model.slack.SlackBlock;
import com.yuriytkach.tracker.fundraiser.model.slack.SlackText;
import com.yuriytkach.tracker.fundraiser.service.FundService;
import com.yuriytkach.tracker.fundraiser.service.IdGenerator;
import com.yuriytkach.tracker.fundraiser.service.dynamodb.DynamoDbDonationClientDonation;
import com.yuriytkach.tracker.fundraiser.service.dynamodb.DynamoDbFundStorageClient;

import io.quarkus.amazon.lambda.http.model.AwsProxyRequest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ListTablesResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

@Slf4j
@QuarkusTest
@QuarkusTestResource(DynamoDbTestResource.class)
class CmdTrackAwsLambdaTest implements AwsLambdaTestCommon {

  private static final UUID ITEM_ID_1 = new UUID(1, 1);
  private static final UUID ITEM_ID_2 = new UUID(2, 2);

  private static final String FUND_2_NAME = "carFund";

  @Inject
  FundTrackerConfig appConfig;

  @Inject
  DynamoDbClient dynamoDB;

  @Inject
  DynamoDbFundStorageClient fundStorageClient;

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

  @AfterEach
  void cleanUp() {
    log.info("----- CLEANUP ------");
    deleteItemByIdDirectly(FUND_1_TABLE, COL_ID, ITEM_ID_1, ITEM_ID_2);
    deleteItemByIdDirectly(FUNDS_TABLE, DynamoDbFundStorageClient.COL_NAME, FUND_2_NAME);
    deleteTableIfExists(FundService.FUND_TABLE_PREFIX + FUND_2_NAME);
    fundStorageClient.save(FUND);
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
    request.setBody("token=" + appConfig.slackToken() + "&user_id=userAbc"
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
    request.setBody("token=" + appConfig.slackToken() + "&user_id=user2"
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

    request.setBody("token=" + appConfig.slackToken() + "&user_id=user2"
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
    request.setBody("token=" + appConfig.slackToken() + "&user_id=" + FUND_OWNER
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
          + " - `fundy` 22.30% [223 of 1000] EUR")
        .build()));

    final Optional<Donation> donation = getDonationDirectlyById(ITEM_ID_1);
    assertThat(donation).hasValue(Donation.builder()
      .id(ITEM_ID_1)
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
    request.setBody("token=" + appConfig.slackToken() + "&user_id=" + FUND_OWNER
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

  @Test
  void shouldUpdateFund() {
    when(idGenerator.generateId()).thenReturn(ITEM_ID_1);

    final AwsProxyRequest request = createAwsProxyRequest();
    request.setBody("token=" + appConfig.slackToken() + "&user_id=" + FUND_OWNER
      + "&text=update " + FUND.getName() + " goal:4242 mono:account-id");

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
      .goal(4242)
      .monobankAccount("account-id")
      .build());
  }

  @Test
  void shouldReturnResultForListAllFundsCommand() {
    final AwsProxyRequest request = createAwsProxyRequest();
    request.setBody("token=" + appConfig.slackToken() + "&user_id=" + FUND_OWNER + "&text=list");

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
        .text(":white_check_mark: All Funds\n"
          + "10.00% `fundy` [100 of 1000] EUR - description [red] - :open_book: 0 hour(s)")
        .build()));
  }

  @Test
  void shouldReturnResultForListFundersCommand() {
    addDonationDirectly(ITEM_ID_1, "EUR", 987, Instant.parse("2022-05-10T00:10:30.107591Z"), "PP");

    final AwsProxyRequest request = createAwsProxyRequest();
    request.setBody("token=" + appConfig.slackToken() + "&text=list " + FUND.getName());

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
    request.setBody("token=" + appConfig.slackToken() + "&text=help");

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

  private Optional<Donation> getDonationDirectlyById(final UUID id) {
    final GetItemRequest dbGetItemRequest = GetItemRequest.builder()
      .tableName(FUND_1_TABLE)
      .key(Map.of(COL_ID, AttributeValue.builder().s(id.toString()).build()))
      .attributesToGet(ALL_ATTRIBUTES)
      .build();
    final GetItemResponse response = dynamoDB.getItem(dbGetItemRequest);
    assertThat(response.item()).isNotEmpty();
    return DynamoDbDonationClientDonation.parseDonation(response.item());
  }

  private Optional<Fund> getFundDirectlyByName(final String name) {
    final GetItemRequest dbGetItemRequest = GetItemRequest.builder()
      .tableName(FUNDS_TABLE)
      .key(Map.of(DynamoDbFundStorageClient.COL_NAME, AttributeValue.builder().s(name).build()))
      .attributesToGet(DynamoDbFundStorageClient.ALL_ATTRIBUTES)
      .build();
    final GetItemResponse response = dynamoDB.getItem(dbGetItemRequest);
    assertThat(response.item()).isNotEmpty();
    return DynamoDbFundStorageClient.parseFund(response.item());
  }

  private void deleteItemByIdDirectly(final String fundTable, final String keyColumn, final Object... ids) {
    final var requests = StreamEx.of(ids)
      .map(id -> AttributeValue.builder().s(id.toString()).build())
      .map(attrValue -> Map.of(keyColumn, attrValue))
      .map(key -> DeleteRequest.builder().key(key).build())
      .map(delReq -> WriteRequest.builder().deleteRequest(delReq).build())
      .toList();

    dynamoDB.batchWriteItem(BatchWriteItemRequest.builder()
      .requestItems(Map.of(fundTable, requests))
      .build());
  }

  private void deleteTableIfExists(final String tableName) {
    final var rez = dynamoDB.listTables();
    if (rez.tableNames().stream().anyMatch(name -> name.equals(tableName))) {
      dynamoDB.deleteTable(DeleteTableRequest.builder().tableName(tableName).build());
    }
  }

}
