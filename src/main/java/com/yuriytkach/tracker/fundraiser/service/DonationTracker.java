package com.yuriytkach.tracker.fundraiser.service;

import java.time.Instant;
import java.util.Collection;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;

import com.yuriytkach.tracker.fundraiser.forex.ForexService;
import com.yuriytkach.tracker.fundraiser.model.Donation;
import com.yuriytkach.tracker.fundraiser.model.Fund;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class DonationTracker {

  private final ForexService forexService;
  private final FundService fundService;
  private final DonationStorageClient donationStorageClient;

  public Fund trackDonation(final Fund fund, final Donation donation) {
    return trackDonations(fund, Set.of(donation));
  }

  public Fund trackDonations(final Fund fund, final Collection<Donation> donations) {
    log.info("Track donations in fund {}: {}", fund.getName(), donations.size());
    if (log.isDebugEnabled()) {
      log.debug("Tracking donations: {}", donations);
    }

    final int donationAmountInFund = StreamEx.of(donations)
      .mapToInt(donation -> forexService.convertCurrency(
        donation.getAmount(), donation.getCurrency(), fund.getCurrency()
      ))
      .sum();
    log.debug("Donations amount in fund currency {}: {}", fund.getCurrency(), donationAmountInFund);

    final Instant latestDonationTime = StreamEx.of(donations)
      .maxBy(Donation::getDateTime)
      .map(Donation::getDateTime)
      .orElseThrow();

    final Fund updatedFund = fund.toBuilder()
      .raised(fund.getRaised() + donationAmountInFund)
      .updatedAt(latestDonationTime)
      .build();

    donationStorageClient.addAll(fund.getId(), donations);

    fundService.updateFund(updatedFund);

    return updatedFund;
  }
}
