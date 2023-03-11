package com.yuriytkach.tracker.fundraiser.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yuriytkach.tracker.fundraiser.forex.ForexService;
import com.yuriytkach.tracker.fundraiser.model.Fund;

@ExtendWith(MockitoExtension.class)
class DonationTrackerTest {

  @Mock
  private ForexService forexService;

  @Mock
  private FundService fundService;

  @Mock
  private DonationStorageClient donationStorageClient;

  @InjectMocks
  private DonationTracker tested;

  @Test
  void shouldNotTrackIfNothingToTrack() {
    tested.trackDonations(mock(Fund.class), List.of());

    verifyNoInteractions(forexService, fundService, donationStorageClient);
  }

}
