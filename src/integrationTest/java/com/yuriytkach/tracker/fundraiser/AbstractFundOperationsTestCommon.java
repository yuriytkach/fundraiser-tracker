package com.yuriytkach.tracker.fundraiser;

import static com.yuriytkach.tracker.fundraiser.DynamoDbTestResource.DONATIONS_TABLE;
import static com.yuriytkach.tracker.fundraiser.DynamoDbTestResource.FUND;
import static com.yuriytkach.tracker.fundraiser.DynamoDbTestResource.FUNDS_TABLE;
import static com.yuriytkach.tracker.fundraiser.service.dynamodb.DynamoDbDonationClientDonation.ALL_ATTRIBUTES_WITHOUT_FUND_ID;
import static com.yuriytkach.tracker.fundraiser.service.dynamodb.DynamoDbDonationClientDonation.COL_FUND_ID;
import static com.yuriytkach.tracker.fundraiser.service.dynamodb.DynamoDbDonationClientDonation.COL_ID;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;

import com.yuriytkach.tracker.fundraiser.model.Donation;
import com.yuriytkach.tracker.fundraiser.model.Fund;
import com.yuriytkach.tracker.fundraiser.service.FundService;
import com.yuriytkach.tracker.fundraiser.service.dynamodb.DynamoDbDonationClientDonation;
import com.yuriytkach.tracker.fundraiser.service.dynamodb.DynamoDbFundStorageClient;

import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

@Slf4j
public abstract class AbstractFundOperationsTestCommon {

  static final UUID ITEM_ID_1 = new UUID(1, 1);
  static final UUID ITEM_ID_2 = new UUID(2, 2);
  static final String FUND_2_NAME = "carFund";

  @Inject
  public DynamoDbFundStorageClient fundStorageClient;

  @Inject
  public DynamoDbClient dynamoDB;

  @AfterEach
  void cleanUp() {
    log.info("----- CLEANUP ------");
    deleteItemByIdDirectly(DONATIONS_TABLE, COL_ID, ITEM_ID_1, ITEM_ID_2);
    deleteItemByIdDirectly(FUNDS_TABLE, DynamoDbFundStorageClient.COL_NAME, FUND_2_NAME);
    deleteTableIfExists(FundService.FUND_TABLE_PREFIX + FUND_2_NAME);
    fundStorageClient.save(FUND);
  }

  protected Optional<DonationWithFundId> getDonationDirectlyById(final String donationId) {
    final GetItemRequest dbGetItemRequest = GetItemRequest.builder()
      .tableName(DynamoDbTestResource.DONATIONS_TABLE)
      .key(Map.of(COL_ID, AttributeValue.builder().s(donationId).build()))
      .attributesToGet(StreamEx.of(ALL_ATTRIBUTES_WITHOUT_FUND_ID, 0, ALL_ATTRIBUTES_WITHOUT_FUND_ID.length)
        .append(COL_FUND_ID).toArray(String.class))
      .build();
    final GetItemResponse response = dynamoDB.getItem(dbGetItemRequest);
    assertThat(response.item()).isNotEmpty();
    return DynamoDbDonationClientDonation.parseDonation(response.item())
      .map(donation -> new DonationWithFundId(response.item().get(COL_FUND_ID).s(), donation));
  }

  protected Optional<Fund> getFundDirectlyByName(final String name) {
    final GetItemRequest dbGetItemRequest = GetItemRequest.builder()
      .tableName(FUNDS_TABLE)
      .key(Map.of(DynamoDbFundStorageClient.COL_NAME, AttributeValue.builder().s(name).build()))
      .attributesToGet(DynamoDbFundStorageClient.ALL_ATTRIBUTES)
      .build();
    final GetItemResponse response = dynamoDB.getItem(dbGetItemRequest);
    assertThat(response.item()).isNotEmpty();
    return DynamoDbFundStorageClient.parseFund(response.item());
  }

  protected void deleteItemByIdDirectly(final String table, final String keyColumn, final Object... ids) {
    final var requests = StreamEx.of(ids)
      .map(id -> AttributeValue.builder().s(id.toString()).build())
      .map(attrValue -> Map.of(keyColumn, attrValue))
      .map(key -> DeleteRequest.builder().key(key).build())
      .map(delReq -> WriteRequest.builder().deleteRequest(delReq).build())
      .toList();

    dynamoDB.batchWriteItem(BatchWriteItemRequest.builder()
      .requestItems(Map.of(table, requests))
      .build());
  }

  private void deleteTableIfExists(final String tableName) {
    final var rez = dynamoDB.listTables();
    if (rez.tableNames().stream().anyMatch(name -> name.equals(tableName))) {
      dynamoDB.deleteTable(DeleteTableRequest.builder().tableName(tableName).build());
    }
  }

  public record DonationWithFundId(String fundId, Donation donation) { }

}
