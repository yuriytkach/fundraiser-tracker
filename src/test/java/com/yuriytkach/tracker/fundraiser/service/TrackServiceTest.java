package com.yuriytkach.tracker.fundraiser.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yuriytkach.tracker.fundraiser.config.FundTrackerConfig;
import com.yuriytkach.tracker.fundraiser.forex.ForexService;
import com.yuriytkach.tracker.fundraiser.model.Currency;
import com.yuriytkach.tracker.fundraiser.model.Donation;
import com.yuriytkach.tracker.fundraiser.model.Fund;
import com.yuriytkach.tracker.fundraiser.model.Funder;
import com.yuriytkach.tracker.fundraiser.model.PagedFunders;
import com.yuriytkach.tracker.fundraiser.model.SortOrder;

@ExtendWith(MockitoExtension.class)
class TrackServiceTest {

  private static final String FUND_NAME = "fund";
  private static final String FUND_ID = "fundId";

  private static final Donation DONATION_1 = Donation.builder()
    .amount(10)
    .currency(Currency.UAH)
    .dateTime(Instant.now().minusSeconds(60))
    .person("person1")
    .build();

  private static final Donation DONATION_2 = Donation.builder()
    .amount(20)
    .currency(Currency.USD)
    .dateTime(Instant.now())
    .person("person2")
    .build();

  @Mock
  DonationStorageClient donationStorageClient;

  @Mock
  FundService fundService;

  @Mock
  IdGenerator idGenerator;

  @Mock
  ForexService forexService;

  @Mock
  FundTrackerConfig config;

  @InjectMocks
  TrackService tested;

  @Test
  void shouldReturnEmptyFundersIfFundNotFound() {
    when(fundService.findByName(any())).thenReturn(Optional.empty());

    final var result = tested.getAllFunders(FUND_NAME, SortOrder.DESC, 0, 2);

    assertThat(result).isEqualTo(PagedFunders.empty());

    verify(fundService).findByName(FUND_NAME);
    verifyNoInteractions(donationStorageClient);
  }

  @ParameterizedTest
  @EnumSource(SortOrder.class)
  void shouldReturnFundersSorted(final SortOrder sortOrder) {
    when(fundService.findByName(any())).thenReturn(Optional.of(Fund.builder()
      .id(FUND_ID)
      .name(FUND_NAME)
      .build()));

    when(donationStorageClient.findAll(any())).thenReturn(List.of(DONATION_1, DONATION_2));

    final var result = tested.getAllFunders(FUND_NAME, sortOrder, null, null);

    assertThat(result.getSize()).isEqualTo(2);
    assertThat(result.getPage()).isZero();
    assertThat(result.getTotal()).isEqualTo(2);

    final var funder1 = Funder.fromDonation(DONATION_1);
    final var funder2 = Funder.fromDonation(DONATION_2);

    assertThat(result.getFunders()).isNotEmpty();

    if (sortOrder == SortOrder.ASC) {
      assertThat(result.getFunders()).containsExactly(funder1, funder2);
    } else {
      assertThat(result.getFunders()).containsExactly(funder2, funder1);
    }
  }

  @ParameterizedTest
  @CsvSource({
    ", , 2, , true",
    "5, , 2, , true",
    ", 1, 1, true, true",
    "0, 1, 1, true, true",
    "0, 5, 2, , true",
    "1, 1, 1, false, true",
    "1, 5, 0, , false",
    "2, 1, 0, , false"
  })
  void shouldReturnPagedResults(
    final Integer page,
    final Integer size,
    final int expectedCnt,
    final Boolean firstDonation,
    final boolean hasFundersInList
  ) {
    when(fundService.findByName(any())).thenReturn(Optional.of(Fund.builder()
      .id(FUND_ID)
      .name(FUND_NAME)
      .build()));

    when(donationStorageClient.findAll(any())).thenReturn(List.of(DONATION_1, DONATION_2));

    final var result = tested.getAllFunders(FUND_NAME, SortOrder.ASC, page, size);

    assertThat(result.getTotal()).isEqualTo(2);
    assertThat(result.getPage()).isEqualTo(size == null ? 0 : (page == null ? 0 : page));
    assertThat(result.getSize()).isEqualTo(expectedCnt);
    assertThat(result.getFunders()).hasSize(expectedCnt);

    final var funder1 = Funder.fromDonation(DONATION_1);
    final var funder2 = Funder.fromDonation(DONATION_2);

    if (hasFundersInList) {
      if (firstDonation == null) {
        assertThat(result.getFunders()).containsExactlyInAnyOrder(funder1, funder2);
      } else if (firstDonation) {
        assertThat(result.getFunders()).containsOnly(funder1);
      } else {
        assertThat(result.getFunders()).containsOnly(funder2);
      }
    } else {
      assertThat(result.getFunders()).isEmpty();
    }
  }
}
