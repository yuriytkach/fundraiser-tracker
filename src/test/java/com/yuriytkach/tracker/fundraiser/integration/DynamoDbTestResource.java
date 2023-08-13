package com.yuriytkach.tracker.fundraiser.integration;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
  static final String ENABLED_INDEX = "test-mono-index";
  static final String FUND_1_TABLE = "donations-table";
  static final String FUND_2_TABLE = "disabled-table";
  static final String FUND_OWNER = "owner";
  static final String FUND_RED = "red";
  static final String FUND_DESC = "description";
  static final String FUND_BANK_ACCOUNT_ID = "bankAccountId";

  static final String FUND_1_NAME = "fundy";
  static final String FUND_DISABLED_NAME = "dis-fund";

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
    .bankAccounts(Set.of(FUND_BANK_ACCOUNT_ID))
    .build();

  static final Fund FUND_DISABLED = Fund.builder()
    .id(FUND_2_TABLE)
    .enabled(false)
    .name(FUND_DISABLED_NAME)
    .goal(2000)
    .raised(2200)
    .currency(Currency.USD)
    .createdAt(Instant.now())
    .updatedAt(Instant.now())
    .description(FUND_DESC)
    .color(FUND_RED)
    .owner(FUND_OWNER)
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
      buildSecondaryIndex(ENABLED_INDEX, DynamoDbFundStorageClient.COL_ENABLED)
    );
    createTable(dynamoDB, FUND_1_TABLE, DynamoDbDonationClientDonation.COL_ID, null);

    createFundItem(dynamoDB, FUND);
    createFundItem(dynamoDB, FUND_DISABLED);

    return Map.of(
      "app.funds-table", FUNDS_TABLE,
      "app.funds-enabled-index", ENABLED_INDEX,
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

  private void createFundItem(final AmazonDynamoDB dynamoDB, final Fund fund) {
    final PutItemRequest putRequest = new PutItemRequest();
    putRequest.setTableName(FUNDS_TABLE);
    putRequest.addItemEntry(
      DynamoDbFundStorageClient.COL_ID, new AttributeValue().withS(fund.getId()));
    putRequest.addItemEntry(
      DynamoDbFundStorageClient.COL_ENABLED, new AttributeValue().withN(fund.getEnabledNkey()));
    putRequest.addItemEntry(
      DynamoDbFundStorageClient.COL_NAME, new AttributeValue().withS(fund.getName()));
    putRequest.addItemEntry(
      DynamoDbFundStorageClient.COL_DESC, new AttributeValue().withS(fund.getDescription()));
    putRequest.addItemEntry(
      DynamoDbFundStorageClient.COL_COLOR, new AttributeValue().withS(fund.getColor()));
    putRequest.addItemEntry(
      DynamoDbFundStorageClient.COL_OWNER, new AttributeValue().withS(fund.getOwner()));
    putRequest.addItemEntry(
      DynamoDbFundStorageClient.COL_GOAL, new AttributeValue().withN(String.valueOf(fund.getGoal())));
    putRequest.addItemEntry(
      DynamoDbFundStorageClient.COL_RAISED, new AttributeValue().withN(String.valueOf(fund.getRaised())));
    putRequest.addItemEntry(
      DynamoDbFundStorageClient.COL_CURR, new AttributeValue().withS(fund.getCurrency().name()));
    putRequest.addItemEntry(
      DynamoDbFundStorageClient.COL_CREATED_AT, new AttributeValue().withS(fund.getCreatedAt().toString()));
    putRequest.addItemEntry(
      DynamoDbFundStorageClient.COL_UPDATED_AT, new AttributeValue().withS(fund.getUpdatedAt().toString()));
    if (fund.getBankAccounts() != null && !fund.getBankAccounts().isEmpty()) {
      putRequest.addItemEntry(
        DynamoDbFundStorageClient.COL_BANK, new AttributeValue().withSS(fund.getBankAccounts()));
    }

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
        index.getKeySchema().get(0).getAttributeName(), ScalarAttributeType.N));
    }

    final CreateTableResult table = dynamoDB.createTable(request);

    log.info("Created table: {}", table.getTableDescription());
  }

}
