package com.yuriytkach.tracker.fundraiser.service;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import com.yuriytkach.tracker.fundraiser.model.Donation;

public interface DonationStorageClient {
  void add(String fundId, Donation donation);

  Collection<Donation> findAll(String fundId);

  Optional<Donation> getById(String fundId, UUID id);
}
