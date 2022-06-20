package com.yuriytkach.tracker.fundraiser.integration;

import static com.yuriytkach.tracker.fundraiser.service.dynamodb.DynamoDbDonationClientDonation.COL_AMOUNT;
import static com.yuriytkach.tracker.fundraiser.service.dynamodb.DynamoDbDonationClientDonation.COL_CURR;
import static com.yuriytkach.tracker.fundraiser.service.dynamodb.DynamoDbDonationClientDonation.COL_ID;
import static com.yuriytkach.tracker.fundraiser.service.dynamodb.DynamoDbDonationClientDonation.COL_PERSON;
import static com.yuriytkach.tracker.fundraiser.service.dynamodb.DynamoDbDonationClientDonation.COL_TIME;
import static com.yuriytkach.tracker.fundraiser.util.JsonMatcher.jsonEqualTo;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.yuriytkach.tracker.fundraiser.model.Currency;
import com.yuriytkach.tracker.fundraiser.model.Fund;
import com.yuriytkach.tracker.fundraiser.model.FundStatus;
import com.yuriytkach.tracker.fundraiser.model.Funder;
import com.yuriytkach.tracker.fundraiser.model.SortOrder;
import com.yuriytkach.tracker.fundraiser.service.FundService;

import io.quarkus.amazon.lambda.http.model.AwsProxyRequest;
import io.quarkus.amazon.lambda.http.model.MultiValuedTreeMap;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import lombok.SneakyThrows;
import one.util.streamex.StreamEx;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

@QuarkusTest
@QuarkusTestResource(DynamoDbTestResource.class)
public class FundStatusAwsLambdaTest implements AwsLambdaTestCommon {

  private static final UUID ITEM_ID_1 = new UUID(1, 1);
  private static final UUID ITEM_ID_2 = new UUID(2, 2);
  private static final String FUND_NAME = "fund";
  private static final Currency FUND_CURR = Currency.EUR;
  private static final String FUND_DESC = "fund desc";
  private static final String FUND_OWNER = "fund owner";
  private static final String FUND_COLOR = "fund color";

  @Inject
  DynamoDbClient dynamoDB;

  @InjectMock
  FundService fundService;

  @BeforeEach
  void cleanUp() {
    deleteItemByIdDirectly(ITEM_ID_1, ITEM_ID_2);
  }

  @Test
  void shouldReturnNotFoundForUnknownFund() {
    when(fundService.findByName(any())).thenReturn(Optional.empty());

    final AwsProxyRequest request = createAwsProxyRequest();
    request.setBody(null);
    request.setPath("/funds/unknown/status");
    request.setHttpMethod("GET");
    given()
      .contentType(MediaType.APPLICATION_JSON)
      .accept(MediaType.APPLICATION_JSON)
      .when()
      .body(request)
      .when()
      .post(LAMBDA_URL_PATH)
      .then()
      .statusCode(200)
      .body("statusCode", equalTo(404));

    verify(fundService).findByName("unknown");
  }

  @Test
  void shouldReturnFundStatus() {
    final Fund fund = dummyFund();
    when(fundService.findByName(any())).thenReturn(Optional.of(fund));

    final AwsProxyRequest request = createAwsProxyRequest();
    request.setBody(null);
    request.setPath("/funds/" + FUND_NAME + "/status");
    request.setHttpMethod("GET");
    given()
      .contentType(MediaType.APPLICATION_JSON)
      .accept(MediaType.APPLICATION_JSON)
      .when()
      .body(request)
      .when()
      .post(LAMBDA_URL_PATH)
      .then()
      .statusCode(200)
      .body("statusCode", equalTo(200))
      .body("body", jsonEqualTo(FundStatus.builder()
        .raised(fund.getRaised())
        .goal(fund.getGoal())
        .currency(fund.getCurrency())
        .name(fund.getName())
        .description(fund.getDescription())
        .color(fund.getColor())
        .owner(fund.getOwner())
        .build()));

    verify(fundService).findByName(FUND_NAME);
  }

  @ParameterizedTest
  @ValueSource(strings = {"owner"})
  @NullSource
  void shouldReturnAllFunds(final String owner) {
    final Fund fund = dummyFund();
    when(fundService.findAllFunds(any())).thenReturn(List.of(fund));

    final AwsProxyRequest request = createAwsProxyRequest();
    request.setBody(null);
    request.setPath("/funds");
    request.setHttpMethod("GET");

    if (owner != null) {
      final MultiValuedTreeMap<String, String> queryParams = new MultiValuedTreeMap<>();
      queryParams.add("userId", owner);
      request.setMultiValueQueryStringParameters(queryParams);
    }

    final FundStatus expectedFundStatus = FundStatus.builder()
      .raised(fund.getRaised())
      .goal(fund.getGoal())
      .currency(fund.getCurrency())
      .name(fund.getName())
      .description(fund.getDescription())
      .color(fund.getColor())
      .owner(fund.getOwner())
      .build();

    given()
      .contentType(MediaType.APPLICATION_JSON)
      .accept(MediaType.APPLICATION_JSON)
      .when()
      .body(request)
      .when()
      .post(LAMBDA_URL_PATH)
      .then()
      .statusCode(200)
      .body("statusCode", equalTo(200))
      .body("body", jsonEqualTo(List.of(expectedFundStatus)));

    verify(fundService).findAllFunds(owner);
  }

  @Test
  void shouldReturnEmptyListOfFundersForUnknownFund() {
    when(fundService.findByName(any())).thenReturn(Optional.empty());

    final AwsProxyRequest request = createAwsProxyRequest();
    request.setBody(null);
    request.setPath("/funds/unknown/funders");
    request.setHttpMethod("GET");

    given()
      .contentType(MediaType.APPLICATION_JSON)
      .accept(MediaType.APPLICATION_JSON)
      .when()
      .body(request)
      .when()
      .post(LAMBDA_URL_PATH)
      .then()
      .statusCode(200)
      .body("statusCode", equalTo(200))
      .body("body", jsonEqualTo(List.of()));

    verify(fundService).findByName("unknown");
  }

  @ParameterizedTest
  @NullSource
  @EnumSource(SortOrder.class)
  @SneakyThrows
  void shouldReturnAllFunders(final SortOrder sortOrder) {
    final Fund fund = dummyFund();
    when(fundService.findByName(any())).thenReturn(Optional.of(fund));

    addDonationDirectly(ITEM_ID_1, "EUR", 123, Instant.parse("2022-02-01T12:13:00Z"), "person1");
    addDonationDirectly(ITEM_ID_2, "USD", 987, Instant.parse("2022-03-01T12:13:00Z"), "person2");

    final AwsProxyRequest request = createAwsProxyRequest();
    request.setBody(null);
    request.setPath("/funds/" + FUND_NAME + "/funders");
    request.setHttpMethod("GET");
    if (sortOrder != null) {
      final MultiValuedTreeMap<String, String> queryParams = new MultiValuedTreeMap<>();
      queryParams.add("sortOrder", sortOrder.name());
      request.setMultiValueQueryStringParameters(queryParams);
    }

    final Funder funder1 = Funder.builder()
      .name("person1")
      .amount(123)
      .currency(Currency.EUR)
      .fundedAt("2022-02-01T12:13:00Z")
      .build();

    final Funder funder2 = Funder.builder()
      .name("person2")
      .amount(987)
      .currency(Currency.USD)
      .fundedAt("2022-03-01T12:13:00Z")
      .build();

    final var expectedFunders = SortOrder.ASC == sortOrder ? List.of(funder1, funder2) : List.of(funder2, funder1);

    final var body = given()
      .contentType(MediaType.APPLICATION_JSON)
      .accept(MediaType.APPLICATION_JSON)
      .when()
      .body(request)
      .when()
      .post(LAMBDA_URL_PATH)
      .then()
      .statusCode(200)
      .body("statusCode", equalTo(200))
      .body("body", jsonEqualTo(expectedFunders))
      .body("multiValueHeaders.x-page", hasItems("0"))
      .body("multiValueHeaders.x-size", hasItems("2"))
      .body("multiValueHeaders.x-total-count", hasItems("2"))
      .extract().body().asString();
    System.out.println(body);

    verify(fundService).findByName(FUND_NAME);
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
      .tableName(DynamoDbTestResource.FUND_1_TABLE)
      .item(item)
      .build();
    dynamoDB.putItem(putRequest);
  }

  private void deleteItemByIdDirectly(final UUID... ids) {
    final var requests = StreamEx.of(ids)
      .map(id -> AttributeValue.builder().s(id.toString()).build())
      .map(attrValue -> Map.of(COL_ID, attrValue))
      .map(key -> DeleteRequest.builder().key(key).build())
      .map(delReq -> WriteRequest.builder().deleteRequest(delReq).build())
      .toList();

    dynamoDB.batchWriteItem(BatchWriteItemRequest.builder()
      .requestItems(Map.of(DynamoDbTestResource.FUND_1_TABLE, requests))
      .build());
  }

  private Fund dummyFund() {
    return Fund.builder()
      .id(DynamoDbTestResource.FUND_1_TABLE)
      .goal(1000)
      .currency(FUND_CURR)
      .raised(0)
      .name(FUND_NAME)
      .description(FUND_DESC)
      .owner(FUND_OWNER)
      .color(FUND_COLOR)
      .updatedAt(Instant.now().minusSeconds(20))
      .createdAt(Instant.now().minusSeconds(30))
      .build();
  }

}
