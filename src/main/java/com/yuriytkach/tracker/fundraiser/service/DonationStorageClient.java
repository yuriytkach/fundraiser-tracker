package com.yuriytkach.tracker.fundraiser.service;

import java.util.Collection;

import com.yuriytkach.tracker.fundraiser.model.Donation;

public interface DonationStorageClient {
  void addAll(String fundId, Collection<Donation> donations);

  Collection<Donation> findAll(String fundId);

}
