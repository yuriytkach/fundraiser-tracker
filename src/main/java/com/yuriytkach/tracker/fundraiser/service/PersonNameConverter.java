package com.yuriytkach.tracker.fundraiser.service;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import one.util.streamex.EntryStream;
import one.util.streamex.IntStreamEx;
import software.amazon.awssdk.utils.StringUtils;

@Slf4j
@ApplicationScoped
public class PersonNameConverter {

  private static final String NONAME = "noname";

  private static final Map<Character, String> UA_TO_EN_MAP = EntryStream.zip(
    new Character[] {'а', 'б', 'в', 'г', 'ґ', 'д', 'е', 'є', 'ж', 'з', 'и', 'і', 'ї', 'й', 'к', 'л', 'м', 'н',
      'о', 'п', 'р', 'с', 'т', 'у', 'ф', 'х', 'ц', 'ч', 'ш', 'щ', 'ь', 'ю', 'я', '\''},
    new String[] {"a", "b", "v", "h", "g", "d", "e", "ye", "zh", "z", "y", "i", "yi", "y", "k", "l", "m", "n",
      "o", "p", "r", "s", "t", "u", "f", "kh", "ts", "ch", "sh", "sch", "", "yu", "ya", ""}
  ).toImmutableMap();

  public String convertFromMonoDescription(final String monoDescription) {
    if (monoDescription == null || monoDescription.isBlank()) {
      return NONAME;
    }

    final String[] nameParts = monoDescription.replace("Від: ", "").split(" ");
    if (nameParts.length == 0) {
      return NONAME;
    }

    final String firstNameFinal = extractFirstName(nameParts, 0);
    final String lastNameFirstLetter = extractLastNameFirstLetter(nameParts, 1);

    return firstNameFinal + lastNameFirstLetter;
  }

  public String convertFromPrivatDescription(final String privatbankDescription) {
    if (privatbankDescription == null || privatbankDescription.isBlank()) {
      return NONAME;
    }

    final String[] nameParts = privatbankDescription.replaceAll("ІПН\\d+", "").split(" ");
    if (nameParts.length < 3) {
      return NONAME;
    }

    final String firstNameFinal = extractFirstName(nameParts, nameParts.length - 2);
    final String lastNameFirstLetter = extractLastNameFirstLetter(nameParts, nameParts.length - 3);

    return firstNameFinal + lastNameFirstLetter;
  }

  private static String extractFirstName(final String[] nameParts, final int namePartIndex) {
    final String firstName = nameParts[namePartIndex].toLowerCase();

    final String firstNameConverted = IntStreamEx.of(firstName.chars())
      .mapToObj(ch -> (char) ch)
      .map(key -> UA_TO_EN_MAP.getOrDefault(key, String.valueOf(key)))
      .nonNull()
      .joining();

    return StringUtils.capitalize(firstNameConverted);
  }

  private static String extractLastNameFirstLetter(final String[] nameParts, final int namePartIndex) {
    if (nameParts.length > 1) {
      final char lastNameFirstLetter = nameParts[namePartIndex].toLowerCase().charAt(0);
      final String lastNameFirstLetterConverted = UA_TO_EN_MAP.get(lastNameFirstLetter);
      if (lastNameFirstLetterConverted == null) {
        return "";
      } else {
        return lastNameFirstLetterConverted.toUpperCase();
      }
    } else {
      return "";
    }
  }

}
