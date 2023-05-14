package com.yuriytkach.tracker.fundraiser.service;

import static java.util.Comparator.comparing;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Singleton;

import com.yuriytkach.tracker.fundraiser.config.FundersProperties;
import com.yuriytkach.tracker.fundraiser.model.Donation;
import com.yuriytkach.tracker.fundraiser.model.Fund;
import com.yuriytkach.tracker.fundraiser.model.Funder;
import com.yuriytkach.tracker.fundraiser.model.PagedFunders;
import com.yuriytkach.tracker.fundraiser.model.SortOrder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@RequiredArgsConstructor
public class FundersService {

  private final DonationStorageClient donationStorageClient;

  private final FundService fundService;

  private final FundersProperties fundersProperties;

  public PagedFunders getAllFunders(
    final String fundName,
    final SortOrder sortOrder,
    final Integer page,
    final Integer size
  ) {
    final Optional<Fund> fundOpt = fundService.findByName(fundName);
    if (fundOpt.isEmpty()) {
      return PagedFunders.empty();
    }
    log.info("Getting all funders of fund: {}", fundName);
    final Comparator<Funder> fundedAtComparator = comparing(Funder::getFundedAt);
    final Collection<Donation> foundFunders = donationStorageClient.findAll(fundOpt.get().getId());
    final Stream<Funder> sortedFunders = foundFunders.stream()
      .filter(donation -> donation.getAmount() >= fundersProperties.minDonationForView())
      .map(Funder::fromDonation)
      .sorted(sortOrder == SortOrder.ASC ? fundedAtComparator : fundedAtComparator.reversed());

    if (size == null) {
      return prepareAllFunders(sortedFunders, fundOpt.get().isEnabled());
    } else {
      return preparedPagedFunders(page, size, foundFunders, sortedFunders, fundOpt.get().isEnabled());
    }
  }

  private PagedFunders prepareAllFunders(
    final Stream<Funder> sortedFunders,
    final boolean enabledFund
  ) {
    log.debug("Return all funders as no page/size was specified");
    final var funders = sortedFunders.toList();
    return PagedFunders.builder().page(0)
      .enabledFund(enabledFund)
      .size(funders.size())
      .total(funders.size())
      .funders(funders)
      .build();
  }

  private PagedFunders preparedPagedFunders(
    final Integer page,
    final Integer size,
    final Collection<Donation> foundFunders,
    final Stream<Funder> sortedFunders,
    final boolean enabledFund
  ) {
    final int realPage = page == null ? 0 : page;
    log.debug("Return all funders of page: {}, with size: {}", realPage, size);
    final int skip = size * realPage;
    final var funders = sortedFunders.skip(skip).limit(size).toList();
    return PagedFunders.builder()
      .enabledFund(enabledFund)
      .page(realPage)
      .size(funders.size())
      .total(foundFunders.size())
      .funders(funders)
      .build();
  }

}
