package com.yuriytkach.tracker.fundraiser.util;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class JsonMatcher extends BaseMatcher<String> {

  private static final ObjectMapper OBJECT_MAPPER =
    new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

  private final String expectedJson;

  @SneakyThrows
  public static Matcher<String> jsonEqualTo(final Object obj) {
    return new JsonMatcher(OBJECT_MAPPER.writeValueAsString(obj));
  }

  @Override
  public boolean matches(final Object another) {
    if (another == null) {
      return false;
    }
    try {
      JSONAssert.assertEquals(expectedJson, another.toString(), JSONCompareMode.STRICT);
      return true;
    } catch (final JSONException ex) {
      return false;
    }
  }

  @Override
  public void describeTo(final Description description) {
    description.appendValue(expectedJson);
  }
}
