package com.yuriytkach.tracker.fundraiser.service;

import javax.enterprise.context.ApplicationScoped;

import com.yuriytkach.tracker.fundraiser.forex.ForexService;
import com.yuriytkach.tracker.fundraiser.model.Donation;
import com.yuriytkach.tracker.fundraiser.model.Fund;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class DonationTracker {

  private final ForexService forexService;
  private final FundService fundService;
  private final DonationStorageClient donationStorageClient;

  public Fund trackDonation(final Fund fund, final Donation donation) {
    log.info("Track in fund {}: {}", fund.getName(), donation);

    final int donationAmountInFund = forexService.convertCurrency(
      donation.getAmount(), donation.getCurrency(), fund.getCurrency()
    );
    log.debug("Donation amount in fund currency {}: {}", fund.getCurrency(), donationAmountInFund);

    final Fund updatedFund = fund.toBuilder()
      .raised(fund.getRaised() + donationAmountInFund)
      .updatedAt(donation.getDateTime())
      .build();

    donationStorageClient.add(fund.getId(), donation);

    fundService.updateFund(updatedFund);

    return updatedFund;
  }

}
