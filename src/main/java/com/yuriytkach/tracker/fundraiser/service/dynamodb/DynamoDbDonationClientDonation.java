package com.yuriytkach.tracker.fundraiser.service.dynamodb;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.yuriytkach.tracker.fundraiser.model.Currency;
import com.yuriytkach.tracker.fundraiser.model.Donation;
import com.yuriytkach.tracker.fundraiser.service.DonationStorageClient;

import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.WriteRequest;

@Slf4j
@Singleton
@RequiredArgsConstructor
public class DynamoDbDonationClientDonation implements DonationStorageClient {

  public static final String COL_CURR = "curr";
  public static final String COL_AMOUNT = "amount";
  public static final String COL_TIME = "time";
  public static final String COL_PERSON = "person";
  public static final String COL_ID = "id";

  public static final String[] ALL_ATTRIBUTES = new String[] {
    COL_ID,
    COL_CURR,
    COL_AMOUNT,
    COL_TIME,
    COL_PERSON,
  };

  private final DynamoDbClient dynamoDB;

  public static Optional<Donation> parseDonation(final Map<String, AttributeValue> item) {
    if (item == null || item.isEmpty()) {
      return Optional.empty();
    } else {
      return Optional.of(Donation.builder()
        .id(item.get(COL_ID).s())
        .currency(Currency.fromString(item.get(COL_CURR).s()).orElseThrow())
        .amount(Integer.parseInt(item.get(COL_AMOUNT).n()))
        .dateTime(Instant.parse(item.get(COL_TIME).s()))
        .person(item.get(COL_PERSON).s())
        .build());
    }
  }

  @Override
  public void addAll(final String fundId, final Collection<Donation> donations) {
    log.debug("Saving donations to table `{}`: {}", fundId, donations.size());
    final var writeRequests = StreamEx.of(donations)
      .map(this::createPutRequest)
      .map(putRequest -> WriteRequest.builder().putRequest(putRequest).build())
      .toImmutableSet();

    final BatchWriteItemRequest request = BatchWriteItemRequest.builder()
      .requestItems(Map.of(fundId, writeRequests))
      .build();
    final BatchWriteItemResponse response = dynamoDB.batchWriteItem(request);
    log.debug("Saved donations. Consumed capacity: {}", response.consumedCapacity());
  }

  @Override
  public Collection<Donation> findAll(final String fundId) {
    final var request = ScanRequest.builder()
      .tableName(fundId)
      .attributesToGet(ALL_ATTRIBUTES)
      .build();

    return dynamoDB.scanPaginator(request).items().stream()
      .map(DynamoDbDonationClientDonation::parseDonation)
      .flatMap(Optional::stream)
      .collect(Collectors.toUnmodifiableList());
  }

  private PutRequest createPutRequest(final Donation donation) {
    final Map<String, AttributeValue> item = new HashMap<>();
    item.put(COL_ID, AttributeValue.builder().s(donation.getId()).build());
    item.put(COL_CURR, AttributeValue.builder().s(donation.getCurrency().name()).build());
    item.put(COL_AMOUNT, AttributeValue.builder().n(String.valueOf(donation.getAmount())).build());
    item.put(COL_TIME, AttributeValue.builder().s(donation.getDateTime().toString()).build());
    item.put(COL_PERSON, AttributeValue.builder().s(donation.getPerson()).build());

    return PutRequest.builder()
      .item(item)
      .build();
  }
}
