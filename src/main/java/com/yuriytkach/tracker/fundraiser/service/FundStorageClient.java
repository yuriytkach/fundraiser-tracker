package com.yuriytkach.tracker.fundraiser.service;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import com.yuriytkach.tracker.fundraiser.model.Fund;

public interface FundStorageClient {
  void create(Fund fund);

  void save(Fund fund);

  Collection<Fund> findAll();

  Optional<Fund> getByName(String name);

  void remove(Fund fund);

  Optional<Fund> getActiveFundByBankAccount(String accountId);

  Stream<Fund> findAllEnabled();
}
