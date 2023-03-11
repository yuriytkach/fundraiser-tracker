package com.yuriytkach.tracker.fundraiser.privatbank;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yuriytkach.tracker.fundraiser.model.Donation;
import com.yuriytkach.tracker.fundraiser.model.Fund;
import com.yuriytkach.tracker.fundraiser.privatbank.model.Transaction;
import com.yuriytkach.tracker.fundraiser.service.DonationStorageClient;
import com.yuriytkach.tracker.fundraiser.service.DonationTracker;

@ExtendWith(MockitoExtension.class)
class PrivatbankTransactionSyncServiceTest {

  private static final String EXISTING_TX_ID = "id";
  private static final String FUND_ID = "fund";
  @Mock
  private DonationStorageClient donationStorageClient;
  @Mock
  private DonationTracker donationTracker;
  @Mock
  private PrivatbankTransactionMapper transactionMapper;

  @InjectMocks
  private PrivatbankTransactionSyncService tested;

  @Test
  void shouldNotTrackDonationsThatAreNotAlreadyPresent() {
    final Donation existingDonation = mock(Donation.class);
    final Donation otherDonation = mock(Donation.class);

    when(existingDonation.getId()).thenReturn(EXISTING_TX_ID);
    when(otherDonation.getId()).thenReturn("PTN PNH");

    when(donationStorageClient.findAll(any())).thenReturn(Set.of(
      existingDonation, otherDonation
    ));

    final Donation donation = mock(Donation.class);

    when(transactionMapper.convertToDonation(any()))
      .thenReturn(Optional.of(donation))
      .thenReturn(Optional.empty());

    final Fund mockFund = mock(Fund.class);
    when(mockFund.getId()).thenReturn(FUND_ID);

    final Transaction trackedTx = mock(Transaction.class);
    when(trackedTx.getId()).thenReturn("1");

    final Transaction failedToMapTx = mock(Transaction.class);
    when(failedToMapTx.getId()).thenReturn("2");

    final Transaction existingTx = mock(Transaction.class);
    when(existingTx.getId()).thenReturn(EXISTING_TX_ID);


    tested.syncTransactions(mockFund, Set.of(trackedTx, failedToMapTx, existingTx));

    verify(donationTracker).trackDonations(mockFund, Set.of(donation));
  }

}
