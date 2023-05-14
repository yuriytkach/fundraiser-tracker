package com.yuriytkach.tracker.fundraiser.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.jackson.ObjectMapperCustomizer;
import jakarta.inject.Singleton;

@Singleton
public class ObjectMapperConfigurer implements ObjectMapperCustomizer {

  @Override
  public void customize(final ObjectMapper objectMapper) {
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
  }
}
