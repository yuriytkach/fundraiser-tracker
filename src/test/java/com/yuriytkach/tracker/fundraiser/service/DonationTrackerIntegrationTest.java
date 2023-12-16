package com.yuriytkach.tracker.fundraiser.service;

import com.yuriytkach.tracker.fundraiser.model.Donation;
import com.yuriytkach.tracker.fundraiser.model.Fund;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.util.Collections;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DonationTrackerIntegrationTest {

  @Mock
  private FundService fundService;

  @Mock
  private FundStorageClient fundStorageClient;

  @InjectMocks
  private DonationTracker donationTracker;

  @Test
  public void shouldNotUpdateFundIfCurrentAmountIsNotExpected() {
    final Fund mockFund = mock(Fund.class);
    final Donation mockDonation = mock(Donation.class);

    when(mockFund.getRaised()).thenReturn(100);
    when(mockDonation.getAmount()).thenReturn(50);

    doThrow(ConditionalCheckFailedException.builder().message("Condition check failed").build())
      .when(fundService).updateFundWithCondition(any(Fund.class), anyInt());

    donationTracker.trackDonations(mockFund, Collections.singleton(mockDonation));

    verify(fundService, times(1)).updateFundWithCondition(any(Fund.class), anyInt());
    verify(fundStorageClient, never()).save(any(Fund.class), anyInt());
  }
}
