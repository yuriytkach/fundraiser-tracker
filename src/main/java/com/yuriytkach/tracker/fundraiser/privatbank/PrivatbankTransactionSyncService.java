package com.yuriytkach.tracker.fundraiser.privatbank;

import static java.util.function.Predicate.not;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.yuriytkach.tracker.fundraiser.model.Donation;
import com.yuriytkach.tracker.fundraiser.model.Fund;
import com.yuriytkach.tracker.fundraiser.privatbank.model.Transaction;
import com.yuriytkach.tracker.fundraiser.service.DonationStorageClient;
import com.yuriytkach.tracker.fundraiser.service.DonationTracker;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.StreamEx;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
class PrivatbankTransactionSyncService {
  private final DonationStorageClient donationStorageClient;
  private final DonationTracker donationTracker;
  private final PrivatbankTransactionMapper transactionMapper;

  void syncTransactions(final Fund fund, final Collection<Transaction> transactions) {
    final Set<String> allDonationIds = donationStorageClient.findAll(fund.getId()).stream()
      .map(Donation::getId)
      .collect(Collectors.toUnmodifiableSet());

    final Set<Donation> donationsForTracking = StreamEx.of(transactions)
      .filter(not(tx -> allDonationIds.contains(tx.getId())))
      .map(transactionMapper::convertToDonation)
      .flatMap(Optional::stream)
      .toImmutableSet();

    donationTracker.trackDonations(fund, donationsForTracking);
  }
}
