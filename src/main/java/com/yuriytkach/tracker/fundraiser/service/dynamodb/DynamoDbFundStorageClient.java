package com.yuriytkach.tracker.fundraiser.service.dynamodb;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import com.yuriytkach.tracker.fundraiser.config.FundTrackerConfig;
import com.yuriytkach.tracker.fundraiser.model.Currency;
import com.yuriytkach.tracker.fundraiser.model.Fund;
import com.yuriytkach.tracker.fundraiser.service.FundStorageClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.CreateTableResponse;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

@Slf4j
@Singleton
@RequiredArgsConstructor
public class DynamoDbFundStorageClient implements FundStorageClient {

  public static final String COL_ID = "id";
  public static final String COL_NAME = "name";
  public static final String COL_OWNER = "owner";
  public static final String COL_DESC = "description";
  public static final String COL_COLOR = "color";
  public static final String COL_CURR = "curr";
  public static final String COL_GOAL = "goal";
  public static final String COL_RAISED = "raised";
  public static final String COL_CREATED_AT = "createdAt";
  public static final String COL_UPDATED_AT = "updatedAt";

  public static final String[] ALL_ATTRIBUTES = new String[] {
    COL_ID,
    COL_NAME,
    COL_DESC,
    COL_COLOR,
    COL_OWNER,
    COL_CURR,
    COL_GOAL,
    COL_RAISED,
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
        .name(item.get(COL_NAME).s())
        .owner(item.get(COL_OWNER).s())
        .description(item.get(COL_DESC).s())
        .color(item.get(COL_COLOR).s())
        .goal(Integer.parseInt(item.get(COL_GOAL).n()))
        .raised(Integer.parseInt(item.get(COL_RAISED).n()))
        .currency(Currency.fromString(item.get(COL_CURR).s()).orElseThrow())
        .createdAt(Instant.parse(item.get(COL_CREATED_AT).s()))
        .updatedAt(Instant.parse(item.get(COL_UPDATED_AT).s()))
        .build());
    }
  }

  @Override
  public void create(final Fund fund) {
    final var createTableRequest = CreateTableRequest.builder()
      .tableName(fund.getId())
      .keySchema(
        KeySchemaElement.builder()
          .keyType(KeyType.HASH)
          .attributeName(DynamoDbDonationClientDonation.COL_ID)
          .build()
      )
      .attributeDefinitions(
        AttributeDefinition.builder()
          .attributeName(DynamoDbDonationClientDonation.COL_ID)
          .attributeType(ScalarAttributeType.S)
          .build()
      )
      .provisionedThroughput(ProvisionedThroughput.builder().readCapacityUnits(1L).writeCapacityUnits(1L).build())
      .build();

    log.debug("Creating table for fund '{}': {}", fund.getName(), fund.getId());
    final CreateTableResponse createTableResponse = dynamoDB.createTable(createTableRequest);
    log.info("Created table: {}", createTableResponse.tableDescription().tableName());

    save(fund);
  }

  @Override
  public void save(final Fund item) {
    log.debug("Saving: {}", item);

    final PutItemRequest putRequest = PutItemRequest.builder()
      .tableName(config.fundsTable())
      .item(Map.of(
        COL_ID, AttributeValue.builder().s(item.getId()).build(),
        COL_NAME, AttributeValue.builder().s(item.getName()).build(),
        COL_OWNER, AttributeValue.builder().s(item.getOwner()).build(),
        COL_DESC, AttributeValue.builder().s(item.getDescription()).build(),
        COL_COLOR, AttributeValue.builder().s(item.getColor()).build(),
        COL_CURR, AttributeValue.builder().s(item.getCurrency().name()).build(),
        COL_GOAL, AttributeValue.builder().n(String.valueOf(item.getGoal())).build(),
        COL_RAISED, AttributeValue.builder().n(String.valueOf(item.getRaised())).build(),
        COL_CREATED_AT, AttributeValue.builder().s(item.getCreatedAt().toString()).build(),
        COL_UPDATED_AT, AttributeValue.builder().s(item.getUpdatedAt().toString()).build()
      ))
      .build();

    dynamoDB.putItem(putRequest);

    log.info("Saved: {}", item);
  }

  @Override
  public Collection<Fund> findAll() {
    final var request = ScanRequest.builder()
      .tableName(config.fundsTable())
      .attributesToGet(ALL_ATTRIBUTES)
      .build();

    return dynamoDB.scanPaginator(request).items().stream()
      .map(DynamoDbFundStorageClient::parseFund)
      .flatMap(Optional::stream)
      .collect(Collectors.toUnmodifiableList());
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
  public void remove(final Fund fund) {
    final DeleteItemRequest itemDeleteRequest = DeleteItemRequest.builder()
      .tableName(config.fundsTable())
      .key(Map.of(COL_NAME, AttributeValue.builder().s(fund.getName()).build()))
      .build();
    dynamoDB.deleteItem(itemDeleteRequest);

    log.info("Deleted fund record: {}", fund.getName());

    final DeleteTableRequest deleteTableRequest = DeleteTableRequest.builder()
      .tableName(fund.getId())
      .build();
    dynamoDB.deleteTable(deleteTableRequest);

    log.info("Deleted fund table: {}", fund.getId());
  }
}
