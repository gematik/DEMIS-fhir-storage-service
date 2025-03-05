package de.gematik.demis.storage.reader.common.search;

/*-
 * #%L
 * fhir-storage-reader
 * %%
 * Copyright (C) 2025 gematik GmbH
 * %%
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the
 * European Commission – subsequent versions of the EUPL (the "Licence").
 * You may not use this work except in compliance with the Licence.
 *
 * You find a copy of the Licence in the "Licence" file or at
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either expressed or implied.
 * In case of changes by gematik find details in the "Readme" file.
 *
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 * #L%
 */

import static de.gematik.demis.storage.reader.error.ErrorCode.INVALID_FILTER;
import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.demis.service.base.error.ServiceException;
import de.gematik.demis.storage.common.entity.Tag;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.util.MultiValueMapAdapter;

@Slf4j
class RequestParamFilterResolverTest {
  final RequestParamFilterResolver<Filter> underTest =
      new RequestParamFilterResolver<>(Filter::new, Map.of());

  static Stream<Arguments> lastUpdated() {
    // Note: sql query: lastUpdated >= LowerBound and lastUpdated <= UpperBound --> bounds are
    // included
    return Stream.of(
        // dates
        /*
        NOTE:
        Dates have no timezone. In contrast to HL7 standard the server timezone is not used in this case.
        Instead, HAPI considers the date as a global time period that covers the entire time span in which the date occurs anywhere in the world.
        That means we have a period of 50 hours.
        The period starts at 00:00 +14:00 --> 10:00 UTC one day before
        The Period ends at 00:00 -12:00 --> 12:00 UTC next day
         */
        Arguments.of(List.of("gt2024-01-01"), "2024-01-01T10:00Z", null),
        Arguments.of(List.of("ge2024-01-01"), "2023-12-31T10:00Z", null),
        Arguments.of(List.of("lt2024-01-01"), null, "2024-01-01T11:59:59.999Z"),
        Arguments.of(List.of("le2024-01-01"), null, "2024-01-02T11:59:59.999Z"),
        Arguments.of(List.of("eq2024-01-01"), "2023-12-31T10:00Z", "2024-01-02T11:59:59.999Z"),
        Arguments.of(List.of("ne2024-01-01"), "2023-12-31T10:00Z", "2024-01-02T11:59:59.999Z"),
        // timestamps (server time zone)
        Arguments.of(List.of("gt2024-01-01T14:25"), "2024-01-01T13:26Z", null),
        Arguments.of(List.of("ge2024-01-01T14:25"), "2024-01-01T13:25Z", null),
        Arguments.of(List.of("lt2024-01-01T14:25"), null, "2024-01-01T13:24:59.999Z"),
        Arguments.of(List.of("le2024-01-01T14:25"), null, "2024-01-01T13:25:59.999Z"),
        Arguments.of(
            List.of("eq2024-01-01T14:25"), "2024-01-01T13:25Z", "2024-01-01T13:25:59.999Z"),
        Arguments.of(
            List.of("ne2024-01-01T14:25"), "2024-01-01T13:25Z", "2024-01-01T13:25:59.999Z"),
        // more precision
        Arguments.of(
            List.of("eq2024-01-01T14:25:55"), "2024-01-01T13:25:55Z", "2024-01-01T13:25:55.999Z"),
        Arguments.of(
            List.of("eq2024-01-01T14:25:55.123"),
            "2024-01-01T13:25:55.123Z",
            "2024-01-01T13:25:55.123Z"),
        // timestamps with timezone
        Arguments.of(
            List.of("eq2024-01-01T14:25:55Z"), "2024-01-01T14:25:55Z", "2024-01-01T14:25:55.999Z"),
        Arguments.of(
            List.of("eq2024-01-01T14:25:55+00:00"),
            "2024-01-01T14:25:55Z",
            "2024-01-01T14:25:55.999Z"),
        Arguments.of(
            List.of("eq2024-01-01T14:25:55+05:00"),
            "2024-01-01T09:25:55Z",
            "2024-01-01T09:25:55.999Z"),
        Arguments.of(
            List.of("eq2024-01-01T14:25:55-03:00"),
            "2024-01-01T17:25:55Z",
            "2024-01-01T17:25:55.999Z"));
  }

  static Stream<Arguments> tag() {
    return Stream.of(
        Arguments.of(
            List.of("https://demis.rki.de/fhir/CodeSystem/ResponsibleDepartment|1.01.0.53."),
            null,
            "1.01.0.53."),
        Arguments.of(
            List.of(
                "https://demis.rki.de/fhir/CodeSystem/ResponsibleDepartment|2.01.0.39.",
                "https://demis.rki.de/fhir/CodeSystem/RelatedNotificationBundle|12345"),
            List.of(
                new Tag()
                    .setSystem("https://demis.rki.de/fhir/CodeSystem/RelatedNotificationBundle")
                    .setCode("12345")),
            "2.01.0.39."));
  }

  static Stream<Arguments> invalid() {
    return Stream.of(
        Arguments.of("_lastUpdated", List.of("gt2024-01-01", "gt2024-01-02")),
        Arguments.of("_lastUpdated", List.of("xx2024-01-01")),
        Arguments.of("_lastUpdated", List.of("gt20240101")),
        Arguments.of("_lastUpdated", List.of("eq2024-01-01+00:00")),
        Arguments.of("_source", List.of("eins", "zwei")));
  }

  @ParameterizedTest
  @MethodSource
  void lastUpdated(final List<String> values, final String lowerBound, final String upperBound) {
    final Map<String, List<String>> params = Map.of("_lastUpdated", values);
    final Filter expectedFilter = new Filter();
    if (lowerBound != null) {
      expectedFilter.setLastUpdatedLowerBound(OffsetDateTime.parse(lowerBound));
    }
    if (upperBound != null) {
      expectedFilter.setLastUpdatedUpperBound(OffsetDateTime.parse(upperBound));
    }

    executeTest(params, expectedFilter);
  }

  @ParameterizedTest
  @MethodSource
  void tag(final List<String> values, final List<Tag> tags, final String responsibleDepartment) {
    final Map<String, List<String>> params = Map.of("_tag", values);
    final Filter expectedFilter = new Filter();
    if (tags != null) {
      expectedFilter.setTags(tags);
    }
    if (responsibleDepartment != null) {
      expectedFilter.setResponsibleDepartment(responsibleDepartment);
    }

    executeTest(params, expectedFilter);
  }

  @Test
  void sourceId() {
    final String sourceId = "abc-123";
    final Map<String, List<String>> params = Map.of("_source", List.of("abc-123"));
    final Filter expectedFilter = new Filter().setSourceId(sourceId);
    executeTest(params, expectedFilter);
  }

  @ParameterizedTest
  @MethodSource
  void invalid(final String parameterName, final List<String> values) {
    final var params = new MultiValueMapAdapter<>(Map.of(parameterName, values));

    Assertions.setMaxStackTraceElementsDisplayed(140);
    Assertions.assertThatThrownBy(() -> underTest.createFilterFromRequestParameters(params))
        .isInstanceOfSatisfying(
            ServiceException.class,
            ex -> {
              assertThat(ex.getErrorCode()).isEqualTo(INVALID_FILTER.getCode());
              log.info("{}={} -> {}", parameterName, values, ex.getMessage());
            });
  }

  private void executeTest(final Map<String, List<String>> params, final Filter expectedFilter) {
    final Filter filter =
        underTest.createFilterFromRequestParameters(new MultiValueMapAdapter<>(params));
    assertThat(filter).isEqualTo(expectedFilter);
  }
}
