package com.yuriytkach.tracker.fundraiser.service.dynamodb;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.yuriytkach.tracker.fundraiser.config.FundTrackerConfig;
import com.yuriytkach.tracker.fundraiser.model.Currency;
import com.yuriytkach.tracker.fundraiser.model.Fund;
import com.yuriytkach.tracker.fundraiser.service.FundStorageClient;

import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.EntryStream;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ComparisonOperator;
import software.amazon.awssdk.services.dynamodb.model.Condition;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

@Slf4j
@Singleton
@RequiredArgsConstructor
public class DynamoDbFundStorageClient implements FundStorageClient {

  public static final String COL_ID = "id";
  public static final String COL_ENABLED = "enabled";
  public static final String COL_NAME = "name";
  public static final String COL_OWNER = "owner";
  public static final String COL_DESC = "description";
  public static final String COL_COLOR = "color";
  public static final String COL_CURR = "curr";
  public static final String COL_GOAL = "goal";
  public static final String COL_RAISED = "raised";
  public static final String COL_BANK = "bank";
  public static final String COL_CREATED_AT = "createdAt";
  public static final String COL_UPDATED_AT = "updatedAt";

  public static final String[] ALL_ATTRIBUTES = new String[] {
    COL_ID,
    COL_ENABLED,
    COL_NAME,
    COL_DESC,
    COL_COLOR,
    COL_OWNER,
    COL_CURR,
    COL_GOAL,
    COL_RAISED,
    COL_BANK,
    COL_CREATED_AT,
    COL_UPDATED_AT,
  };

  private final DynamoDbClient dynamoDB;

  private final FundTrackerConfig config;

  public static Optional<Fund> parseFund(final Map<String, AttributeValue> item) {
    if (item == null || item.isEmpty()) {
      return Optional.empty();
    } else {
      return Optional.of(Fund.builder()
        .id(item.get(COL_ID).s())
        .enabled(Fund.ENABLED_N_KEY.equals(item.get(COL_ENABLED).n()))
        .name(item.get(COL_NAME).s())
        .owner(item.get(COL_OWNER).s())
        .description(item.get(COL_DESC).s())
        .color(item.get(COL_COLOR).s())
        .goal(Integer.parseInt(item.get(COL_GOAL).n()))
        .raised(Integer.parseInt(item.get(COL_RAISED).n()))
        .currency(Currency.fromString(item.get(COL_CURR).s()).orElseThrow())
        .bankAccounts(item.get(COL_BANK) == null ? Set.of() : Set.copyOf(item.get(COL_BANK).ss()))
        .createdAt(Instant.parse(item.get(COL_CREATED_AT).s()))
        .updatedAt(Instant.parse(item.get(COL_UPDATED_AT).s()))
        .build());
    }
  }

  @Override
  public void create(final Fund fund) {
    save(fund);
  }

  @Override
  public void save(final Fund item) {
    log.debug("Saving: {}", item);

    final var attributes = EntryStream.of(
      COL_ID, AttributeValue.builder().s(item.getId()).build(),
      COL_ENABLED, AttributeValue.builder().n(item.getEnabledNkey()).build(),
      COL_NAME, AttributeValue.builder().s(item.getName()).build(),
      COL_OWNER, AttributeValue.builder().s(item.getOwner()).build(),
      COL_DESC, AttributeValue.builder().s(item.getDescription()).build(),
      COL_COLOR, AttributeValue.builder().s(item.getColor()).build(),
      COL_CURR, AttributeValue.builder().s(item.getCurrency().name()).build(),
      COL_GOAL, AttributeValue.builder().n(String.valueOf(item.getGoal())).build(),
      COL_RAISED, AttributeValue.builder().n(String.valueOf(item.getRaised())).build()
    )
      .append(
        COL_CREATED_AT, AttributeValue.builder().s(item.getCreatedAt().toString()).build(),
        COL_UPDATED_AT, AttributeValue.builder().s(item.getUpdatedAt().toString()).build()
      )
      .append(
        COL_BANK, item.getBankAccounts().isEmpty() ? null : AttributeValue.builder().ss(item.getBankAccounts()).build()
      )
      .filterValues(Objects::nonNull)
      .toImmutableMap();

    final PutItemRequest putRequest = PutItemRequest.builder()
      .tableName(config.fundsTable())
      .item(attributes)
      .build();

    dynamoDB.putItem(putRequest);

    log.info("Saved: {}", item);
  }

  @Override
  public Stream<Fund> findAll() {
    final var request = ScanRequest.builder()
      .tableName(config.fundsTable())
      .attributesToGet(ALL_ATTRIBUTES)
      .build();

    return dynamoDB.scanPaginator(request).items().stream()
      .map(DynamoDbFundStorageClient::parseFund)
      .flatMap(Optional::stream);
  }

  @Override
  public Optional<Fund> getByName(final String name) {
    final var request = GetItemRequest.builder()
      .tableName(config.fundsTable())
      .attributesToGet(ALL_ATTRIBUTES)
      .key(Map.of(COL_NAME, AttributeValue.builder().s(name).build()))
      .build();

    final GetItemResponse response = dynamoDB.getItem(request);
    if (response.hasItem()) {
      return parseFund(response.item());
    } else {
      return Optional.empty();
    }
  }

  @Override
  public Optional<Fund> getActiveFundByBankAccount(final String accountId) {
    return findAllEnabled()
      .filter(fund -> fund.getBankAccounts().contains(accountId))
      .findFirst();
  }

  @Override
  public Stream<Fund> findAllEnabled() {
    final QueryRequest queryRequest = QueryRequest.builder()
      .tableName(config.fundsTable())
      .indexName(config.fundsEnabledIndex())
      .keyConditions(Map.of(COL_ENABLED, Condition.builder()
        .attributeValueList(AttributeValue.fromN(Fund.ENABLED_N_KEY))
        .comparisonOperator(ComparisonOperator.EQ)
        .build()))
      .attributesToGet(ALL_ATTRIBUTES)
      .build();

    final var foundFundsOpt = Optional.ofNullable(dynamoDB.query(queryRequest))
      .filter(QueryResponse::hasItems)
      .map(QueryResponse::items);

    foundFundsOpt.ifPresent(list -> log.debug("Found enabled funds: {}", list.size()));

    return foundFundsOpt
      .stream()
      .flatMap(Collection::stream)
      .map(DynamoDbFundStorageClient::parseFund)
      .flatMap(Optional::stream);
  }

  @Override
  public void remove(final Fund fund) {
    final DeleteItemRequest itemDeleteRequest = DeleteItemRequest.builder()
      .tableName(config.fundsTable())
      .key(Map.of(COL_NAME, AttributeValue.builder().s(fund.getName()).build()))
      .build();
    dynamoDB.deleteItem(itemDeleteRequest);

    log.info("Deleted fund record: {}", fund.getName());
  }

}
