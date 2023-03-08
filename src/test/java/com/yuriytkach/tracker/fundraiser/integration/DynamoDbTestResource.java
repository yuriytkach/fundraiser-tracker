package com.yuriytkach.tracker.fundraiser.integration;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.testcontainers.containers.GenericContainer;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.GlobalSecondaryIndex;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.Projection;
import com.amazonaws.services.dynamodbv2.model.ProjectionType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.yuriytkach.tracker.fundraiser.model.Currency;
import com.yuriytkach.tracker.fundraiser.model.Fund;
import com.yuriytkach.tracker.fundraiser.service.dynamodb.DynamoDbDonationClientDonation;
import com.yuriytkach.tracker.fundraiser.service.dynamodb.DynamoDbFundStorageClient;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DynamoDbTestResource implements QuarkusTestResourceLifecycleManager {

  static final String FUNDS_TABLE = "all-funds-table";
  static final String MONO_INDEX = "test-mono-index";
  static final String FUND_1_TABLE = "donations-table";
  static final String FUND_OWNER = "owner";
  static final String FUND_RED = "red";
  static final String FUND_DESC = "description";
  static final String FUND_MONO_ACCOUNT_ID = "monoAccountId";

  static final String FUND_1_NAME = "fundy";

  static final Fund FUND = Fund.builder()
    .id(FUND_1_TABLE)
    .enabled(true)
    .name(FUND_1_NAME)
    .goal(1000)
    .raised(100)
    .currency(Currency.EUR)
    .createdAt(Instant.now())
    .updatedAt(Instant.now())
    .description(FUND_DESC)
    .color(FUND_RED)
    .owner(FUND_OWNER)
    .monobankAccount(FUND_MONO_ACCOUNT_ID)
    .build();

  private static final GenericContainer<?> CONTAINER = new GenericContainer<>("amazon/dynamodb-local:1.11.477")
    .withExposedPorts(8000);

  @Override
  @SneakyThrows
  public Map<String, String> start() {
    CONTAINER.start();

    final String url = "http://" + CONTAINER.getHost() + ":" + CONTAINER.getMappedPort(8000);
    log.info("DynamoDB URL: {}", url);

    final AmazonDynamoDB dynamoDB = AmazonDynamoDBClient.builder()
      .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials("test-key", "test-secret")))
      .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(url, "eu-central-1"))
      .build();

    createTable(
      dynamoDB,
      FUNDS_TABLE,
      DynamoDbFundStorageClient.COL_NAME,
      buildSecondaryIndex(MONO_INDEX, DynamoDbFundStorageClient.COL_MONO)
    );
    createTable(dynamoDB, FUND_1_TABLE, DynamoDbDonationClientDonation.COL_ID, null);

    createFundItem(dynamoDB);

    return Map.of(
      "app.funds-table", FUNDS_TABLE,
      "app.funds-mono-index", MONO_INDEX,
      "quarkus.dynamodb.endpoint-override", url
    );
  }

  private GlobalSecondaryIndex buildSecondaryIndex(
    final String indexName,
    final String keyColumn
  ) {
    return new GlobalSecondaryIndex()
      .withIndexName(indexName)
      .withKeySchema(new KeySchemaElement(keyColumn, KeyType.HASH))
      .withProjection(new Projection().withProjectionType(ProjectionType.ALL))
      .withProvisionedThroughput(new ProvisionedThroughput().withWriteCapacityUnits(1L).withReadCapacityUnits(1L));
  }

  @Override
  public void stop() {
    CONTAINER.stop();
  }

  private void createFundItem(final AmazonDynamoDB dynamoDB) {
    final PutItemRequest putRequest = new PutItemRequest();
    putRequest.setTableName(FUNDS_TABLE);
    putRequest.addItemEntry(
      DynamoDbFundStorageClient.COL_ID, new AttributeValue().withS(FUND.getId()));
    putRequest.addItemEntry(
      DynamoDbFundStorageClient.COL_ENABLED, new AttributeValue().withBOOL(FUND.isEnabled()));
    putRequest.addItemEntry(
      DynamoDbFundStorageClient.COL_NAME, new AttributeValue().withS(FUND.getName()));
    putRequest.addItemEntry(
      DynamoDbFundStorageClient.COL_DESC, new AttributeValue().withS(FUND.getDescription()));
    putRequest.addItemEntry(
      DynamoDbFundStorageClient.COL_COLOR, new AttributeValue().withS(FUND.getColor()));
    putRequest.addItemEntry(
      DynamoDbFundStorageClient.COL_OWNER, new AttributeValue().withS(FUND.getOwner()));
    putRequest.addItemEntry(
      DynamoDbFundStorageClient.COL_GOAL, new AttributeValue().withN(String.valueOf(FUND.getGoal())));
    putRequest.addItemEntry(
      DynamoDbFundStorageClient.COL_RAISED, new AttributeValue().withN(String.valueOf(FUND.getRaised())));
    putRequest.addItemEntry(
      DynamoDbFundStorageClient.COL_CURR, new AttributeValue().withS(FUND.getCurrency().name()));
    putRequest.addItemEntry(
      DynamoDbFundStorageClient.COL_CREATED_AT, new AttributeValue().withS(FUND.getCreatedAt().toString()));
    putRequest.addItemEntry(
      DynamoDbFundStorageClient.COL_UPDATED_AT, new AttributeValue().withS(FUND.getUpdatedAt().toString()));
    putRequest.addItemEntry(
      DynamoDbFundStorageClient.COL_MONO, new AttributeValue().withS(FUND.getMonobankAccount().orElseThrow()));

    dynamoDB.putItem(putRequest);
  }

  private void createTable(
    final AmazonDynamoDB dynamoDB,
    final String tableName,
    final String keyColumn,
    @Nullable final GlobalSecondaryIndex index
  ) {
    final CreateTableRequest request = new CreateTableRequest();
    request.setTableName(tableName);
    request.setKeySchema(List.of(
      new KeySchemaElement(keyColumn, KeyType.HASH)
    ));
    request.setAttributeDefinitions(List.of(
      new AttributeDefinition(keyColumn, ScalarAttributeType.S)
    ));
    request.setProvisionedThroughput(new ProvisionedThroughput(1L, 1L));

    if (index != null) {
      request.withGlobalSecondaryIndexes(List.of(index));
      request.withAttributeDefinitions(new AttributeDefinition(
        index.getKeySchema().get(0).getAttributeName(), ScalarAttributeType.S));
    }

    final CreateTableResult table = dynamoDB.createTable(request);

    log.info("Created table: {}", table.getTableDescription());
  }

}
