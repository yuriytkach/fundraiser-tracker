package com.yuriytkach.tracker.fundraiser.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import com.yuriytkach.tracker.fundraiser.model.Fund;
import com.yuriytkach.tracker.fundraiser.model.exception.DuplicateFundException;
import com.yuriytkach.tracker.fundraiser.model.exception.FundNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class FundService {

  public static final String FUND_TABLE_PREFIX = "fund-";

  private final FundStorageClient fundStorageClient;

  public void createFund(final Fund fund) {
    if (fundStorageClient.getByName(fund.getName()).isPresent()) {
      throw DuplicateFundException.withFund(fund);
    } else {
      final Fund fundWithId;
      if (fund.getId() == null) {
        final String fundNameNoSpaces = fund.getName().replaceAll("\\s", "");
        fundWithId = fund.toBuilder().id(FUND_TABLE_PREFIX + fundNameNoSpaces).build();
      } else {
        fundWithId = fund;
      }

      log.info("Creating: {}", fundWithId);
      fundStorageClient.create(fundWithId);
    }
  }

  public Optional<Fund> findByName(final String fundName) {
    final Optional<Fund> byName = fundStorageClient.getByName(fundName);
    if (byName.isEmpty()) {
      log.debug("Fund by name not found: {}", fundName);
    }
    return byName;
  }

  public Optional<Fund> findEnabledByMonoAccount(final String accountId) {
    final Optional<Fund> byName = fundStorageClient.getByMonoAccount(accountId);
    if (byName.isEmpty()) {
      log.debug("Fund by mono account not found: {}", accountId);
      return Optional.empty();
    } else {
      if (byName.get().isEnabled()) {
        return byName;
      } else {
        log.debug("Fund by mono account is disabled!");
        return Optional.empty();
      }
    }
  }

  public List<Fund> findAllFunds(final String owner) {
    return fundStorageClient.findAll().stream()
      .filter(fund -> owner == null || fund.getOwner().equals(owner))
      .collect(Collectors.toUnmodifiableList());
  }

  public Fund findByNameOrException(final String fundName) {
    return findByName(fundName).orElseThrow(() -> FundNotFoundException.withFundName(fundName));
  }

  public void updateFund(final Fund fund) {
    log.info("Updating fund with new data: {}", fund);
    fundStorageClient.save(fund);
  }

  public void deleteFund(final Fund fund) {
    log.info("Deleting fund {} owned by {} created on {}", fund.getName(), fund.getOwner(), fund.getCreatedAt());
    fundStorageClient.remove(fund);
  }
}
